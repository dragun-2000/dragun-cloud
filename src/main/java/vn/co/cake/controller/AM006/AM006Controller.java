package vn.co.cake.controller.AM006;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.co.cake.common.RequestPathConst;
import vn.co.cake.common.ScreenPathConst;
import vn.co.cake.controller.BaseController;
import vn.co.cake.controller.external.dto.response.OrderDetailResponse;
import vn.co.cake.entity.Order;
import vn.co.cake.enums.OrderStatus;
import vn.co.cake.payment.PaymentConstants;
import vn.co.cake.payment.service.InventoryReservationService;
import vn.co.cake.payment.service.PaymentTransactionLogService;
import vn.co.cake.request.SearchRequest;
import vn.co.cake.repository.OrderRepository;
import vn.co.cake.security.admin.AdminLoginInfo;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.service.AccountService;
import vn.co.cake.service.OrderService;
import vn.co.cake.service.external.PancakePosService;
import vn.co.cake.utils.PageUtil;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static vn.co.cake.common.BaseConst.REDIRECT;
import static vn.co.cake.common.StringConst.BOOKMARK_STAFF_KEY;

/**
 * Controller for managing orders with SYNC_FAIL status
 */
@Controller
@Slf4j
public class AM006Controller extends BaseController {

    private final AccountService accountService;
    private final OrderService orderService;
    private final PancakePosService pancakePosService;
    private final InventoryReservationService inventoryReservationService;
    private final OrderRepository orderRepository;
    private final PaymentTransactionLogService paymentTransactionLogService;

    public AM006Controller(AccountService accountService,
                           OrderService orderService,
                           PancakePosService pancakePosService,
                           InventoryReservationService inventoryReservationService,
                           OrderRepository orderRepository,
                           PaymentTransactionLogService paymentTransactionLogService) {
        this.accountService = accountService;
        this.orderService = orderService;
        this.pancakePosService = pancakePosService;
        this.inventoryReservationService = inventoryReservationService;
        this.orderRepository = orderRepository;
        this.paymentTransactionLogService = paymentTransactionLogService;
    }

    @GetMapping(RequestPathConst.AM006)
    public String listSyncFailOrders(SearchRequest searchForm,
                                     @PageableDefault(
                                             size = SIZE_DEFAULT,
                                             sort = {SORT_DEFAULT_FIRST, SORT_DEFAULT},
                                             direction = Sort.Direction.DESC
                                     ) Pageable pageable,
                                     Model model, HttpSession session, String homePage) {

        String bookmarkUrl = accountService.getBookmarkUrlLasted(BOOKMARK_STAFF_KEY);

        AdminLoginInfo adminLoginInfo = getLoginInfoAdmin(session);
        if (adminLoginInfo == null) {
            return REDIRECT.concat(baseUrl).concat(RequestPathConst.AM001_LOGIN);
        }

        if (StringUtils.isNotEmpty(bookmarkUrl) && !baseUrl.concat(RequestPathConst.AM006).equals(bookmarkUrl)) {
            accountService.deletedBookmarkUrlLasted(BOOKMARK_STAFF_KEY);
            return REDIRECT.concat(bookmarkUrl);
        }

        setCurrentSearchRequestAndPageableSession(session, searchForm, pageable);
        PageUtil pageUtil = getPageSize(session);
        searchForm = getSessionOrderForm(session, homePage);
        Page<Order> orders = orderService.findAllSyncFailOrders(searchForm, pageUtil);
        List<OrderDetailResponse> responses = orders.stream().map(OrderDetailResponse::forList).collect(Collectors.toList());
        model.addAttribute("orders", responses);
        setPaginationAttribute(pageUtil, orders.getTotalElements(), RequestPathConst.AM006, model, settingCondition(searchForm));
        model.addAttribute(SEARCH_CONDITION, searchForm);

        addSideMenu(model, RequestPathConst.AM006);
        removeSessionAttributes(session, SESSION_FROM_EDIT_PAGE);
        return ScreenPathConst.AM006_SCREEN;
    }

    @PostMapping(RequestPathConst.AM006_RETRY_SYNC)
    public ResponseEntity<?> retrySync(@RequestParam String code, HttpSession session) {
        AdminLoginInfo adminLoginInfo = getLoginInfoAdmin(session);
        if (adminLoginInfo == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");
        }

        try {
            Order order = orderService.detail(code);
            if (order == null) {
                log.error("AM006: Order not found with code: {}", code);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");
            }
            order = orderRepository.findWithItemsAndVariationsById(order.getId()).orElse(order);
            boolean recreatePaidOrder = OrderStatus.PAYMENT_RECEIVED_UNFULFILLABLE.getValue()
                    .equals(order.getStatus());
            if (!recreatePaidOrder && !OrderStatus.SYNC_FAIL.getValue().equals(order.getStatus())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("success", false, "message", "Trạng thái đơn không cho phép tạo lại"));
            }

            if (recreatePaidOrder) {
                Calendar deadline = Calendar.getInstance();
                deadline.add(Calendar.MINUTE, 10);
                try {
                    inventoryReservationService.reserveAndConfirmInNewTransaction(
                            order.getId(), deadline.getTime());
                } catch (CommonServletException stockError) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("success", false,
                                    "message", "Chưa thể tạo lại đơn. Admin cần restock sản phẩm trước: "
                                            + stockError.getMessage()));
                }
                order.setStatus(OrderStatus.PENDING_SYNC.getValue());
                order.setMessageError(null);
                orderRepository.save(order);
            }

            log.info("AM006: Admin {} retrying sync for order {}", adminLoginInfo.getUsername(), code);
            boolean success = pancakePosService.createOrder(order);

            if (success) {
                Long accountId = order.getAccount() != null ? order.getAccount().getId() : null;
                paymentTransactionLogService.logSuccess(PaymentConstants.METHOD_VIETQR,
                        PaymentConstants.EVENT_ADMIN_RECREATE_ORDER, order.getCode(), order.getId(),
                        order.getCode(), accountId, null, null, order.getPrepaid(),
                        null, "Admin " + adminLoginInfo.getUsername() + " tạo lại đơn thành công",
                        200, null);
                log.info("AM006: Successfully synced order {} to Pancake POS", code);
                String okMessage = recreatePaidOrder ? "Tạo lại đơn thành công" : "Đồng bộ thành công";
                return ResponseEntity.ok(Map.of("success", true, "message", okMessage));
            } else {
                // Reload order to get updated error info
                Order updatedOrder = orderService.detail(code);
                String errorMessage = updatedOrder != null && updatedOrder.getMessageError() != null 
                    ? updatedOrder.getMessageError() 
                    : "Lỗi không xác định";
                int countError = updatedOrder != null && updatedOrder.getCountError() != null 
                    ? updatedOrder.getCountError() 
                    : 0;
                log.error("AM006: Failed to sync order {} to Pancake POS. CountError: {}, Message: {}", 
                    code, countError, errorMessage);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Đồng bộ thất bại: " + errorMessage, 
                        "countError", countError));
            }
        } catch (Exception e) {
            log.error("AM006: Error while retrying sync for order {}: {}", code, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @PostMapping(RequestPathConst.AM006_DELETE)
    public ResponseEntity<?> deleteOrder(@RequestParam String code, HttpSession session) {
        AdminLoginInfo adminLoginInfo = getLoginInfoAdmin(session);
        if (adminLoginInfo == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");
        }

        try {
            log.info("AM006: Admin {} deleting order {}", adminLoginInfo.getUsername(), code);
            orderService.delete(code);
            log.info("AM006: Successfully deleted order {}", code);
            return ResponseEntity.ok(Map.of("success", true, "message", "Xóa đơn hàng thành công"));
        } catch (CommonServletException e) {
            log.error("AM006: Error deleting order {}: {}", code, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("AM006: Unexpected error while deleting order {}: {}", code, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    private Map<String, Object> settingCondition(SearchRequest searchForm) {
        Map<String, Object> conditionMaps = new HashMap<>();
        return conditionMaps;
    }
}

