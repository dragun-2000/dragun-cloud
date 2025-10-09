package vn.co.cake.controller.external;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import vn.co.cake.controller.BaseController;
import vn.co.cake.dto.CartForm;
import vn.co.cake.dto.OrderItem;
import vn.co.cake.entity.Order;
import vn.co.cake.request.OrderDetailRequest;
import vn.co.cake.request.PaymentRequest;
import vn.co.cake.security.user.UserLoginInfo;
import vn.co.cake.service.CartService;
import vn.co.cake.service.MoMoPaymentService;
import vn.co.cake.service.OrderService;
import vn.co.cake.service.external.PancakePosService;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payment")
@SessionAttributes("cartForm")
public class MomoController extends BaseController {

    private final MoMoPaymentService moMoPaymentService;
    private final OrderService orderService;
    private final PancakePosService pancakePosService;
    private final CartService cartService;

    public MomoController(MoMoPaymentService moMoPaymentService,
                          OrderService orderService,
                          PancakePosService pancakePosService, 
                          CartService cartService) {
        this.moMoPaymentService = moMoPaymentService;
        this.orderService = orderService;
        this.pancakePosService = pancakePosService;
        this.cartService = cartService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createPayment(@RequestBody OrderDetailRequest request, HttpSession session,
                                           @ModelAttribute("cartForm") CartForm cartForm) {
        UserLoginInfo loginInfo = getLoginInfo(session);
        try {
            if (Objects.isNull(loginInfo)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("SA/SA001/login");
            }
            Order order = orderService.create(loginInfo.getId(), cartForm.getOrderItems(), request);
            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setOrderId(order.getId());
            paymentRequest.setAmount(order.getTotalAmount());

            boolean orderToPancakeSuccess = pancakePosService.createOrder(order);
            if (orderToPancakeSuccess) {
                this.removeCartItemOrder(loginInfo.getId(), cartForm);
            } else {
                order.setDeleted(true);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Order Sản phẩm thất bại!");
            }
//            String paymentUrl = moMoPaymentService.createPaymentRequest(paymentRequest);
            return ResponseEntity.ok("Order Sản phẩm thành công!"); // Trả về URL thanh toán từ MoMo
        } catch (Exception e) {
            cartForm.setOrderItems(cartForm.getOrderItems());
            if (cartForm.getOrderItems().isEmpty()) {
                this.removeCartItemOrder(loginInfo.getId(), cartForm);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    private void removeCartItemOrder(Long accountId, CartForm cartForm) {
        cartService.deletedCartByAccount(accountId);
        cartForm.setOrderItems(new ArrayList<>());
    }
}
