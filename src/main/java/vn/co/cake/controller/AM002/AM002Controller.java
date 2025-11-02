package vn.co.cake.controller.AM002;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;
import vn.co.cake.common.RequestPathConst;
import vn.co.cake.common.ScreenPathConst;
import vn.co.cake.controller.BaseController;
import vn.co.cake.controller.external.dto.response.OrderDetailResponse;
import vn.co.cake.entity.Order;
import vn.co.cake.request.SearchRequest;
import vn.co.cake.security.admin.AdminLoginInfo;
import vn.co.cake.service.AccountService;
import vn.co.cake.service.OrderService;
import vn.co.cake.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static vn.co.cake.common.BaseConst.REDIRECT;
import static vn.co.cake.common.StringConst.BOOKMARK_STAFF_KEY;

/**
 *Order
 * */
@Controller
@Slf4j
public class AM002Controller extends BaseController {

    private final AccountService accountService;
    private final OrderService orderService;

    public AM002Controller(AccountService accountService,
                           OrderService orderService) {
        this.accountService = accountService;
        this.orderService = orderService;
    }

    @GetMapping(RequestPathConst.AM002)
    public String listOrder(SearchRequest searchForm,
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

        if (StringUtils.isNotEmpty(bookmarkUrl) && !baseUrl.concat(RequestPathConst.AM002).equals(bookmarkUrl)) {
            accountService.deletedBookmarkUrlLasted(BOOKMARK_STAFF_KEY);
            return REDIRECT.concat(bookmarkUrl);
        }

        setCurrentSearchRequestAndPageableSession(session, searchForm, pageable);
        PageUtil pageUtil = getPageSize(session);
        searchForm = getSessionOrderForm(session, homePage);
        Page<Order> orders = orderService.findAllByCondition(searchForm, pageUtil);
        List<OrderDetailResponse> responses = orders.stream().map(OrderDetailResponse::new).collect(Collectors.toList());
        model.addAttribute("orders", responses);
        setPaginationAttribute(pageUtil, orders.getTotalElements(), RequestPathConst.AM002, model, settingCondition(searchForm));
        model.addAttribute(SEARCH_CONDITION, searchForm);

        addSideMenu(model, RequestPathConst.AM002);
        removeSessionAttributes(session, SESSION_FROM_EDIT_PAGE);
        return ScreenPathConst.AM002_SCREEN;
    }

    @GetMapping(RequestPathConst.AM002_01)
    public String orderDetail(@RequestParam String code, Model model, HttpSession session) {
        AdminLoginInfo adminLoginInfo = getLoginInfoAdmin(session);
        if (adminLoginInfo == null) {
            return REDIRECT.concat(baseUrl).concat(RequestPathConst.AM001_LOGIN);
        }

        Order order = orderService.detail(code);
        OrderDetailResponse orderDetailResponse = new OrderDetailResponse(order);
        model.addAttribute("orderItems", orderDetailResponse.getOrderItems());

        addSideMenu(model, RequestPathConst.AM002);
        return ScreenPathConst.AM002_01_SCREEN;
    }

    private Map<String, Object> settingCondition(SearchRequest searchForm) {
        Map<String, Object> conditionMaps = new HashMap<>();
        return conditionMaps;
    }
}
