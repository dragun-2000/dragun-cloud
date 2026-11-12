package vn.co.cake.vnpay.api;

import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import vn.co.cake.controller.BaseController;
import vn.co.cake.dto.CartForm;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.request.OrderDetailRequest;
import vn.co.cake.response.CommonResponse;
import vn.co.cake.security.user.UserLoginInfo;
import vn.co.cake.service.CartService;
import vn.co.cake.utils.StringUtil;
import vn.co.cake.vnpay.dto.VnpayCheckoutResponse;
import vn.co.cake.vnpay.service.VnpayCheckoutService;

/** API phía khách cho VNPAY (tách khỏi /api/payment của COD/VietQR). */
@Slf4j
@RestController
@RequestMapping("/api/vnpay")
public class VnpayCheckoutController extends BaseController {

    private static final int IP_MAX_LENGTH = 45;

    private final VnpayCheckoutService vnpayCheckoutService;
    private final CartService cartService;

    public VnpayCheckoutController(VnpayCheckoutService vnpayCheckoutService, CartService cartService) {
        this.vnpayCheckoutService = vnpayCheckoutService;
        this.cartService = cartService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody OrderDetailRequest request, HttpSession session,
                                    HttpServletRequest httpRequest) {
        UserLoginInfo loginInfo = getLoginInfo(session);
        if (Objects.isNull(loginInfo)) {
            return errorResponse(HttpStatus.FORBIDDEN, "Vui lòng đăng nhập trước khi đặt hàng");
        }
        CartForm cartForm = resolveCartForm(session, loginInfo);
        if (CollectionUtils.isEmpty(cartForm.getOrderItems())) {
            return errorResponse(HttpStatus.BAD_REQUEST,
                    "Giỏ hàng trống. Vui lòng thêm sản phẩm trước khi thanh toán.");
        }
        try {
            VnpayCheckoutResponse response = vnpayCheckoutService.startCheckout(
                    loginInfo.getId(), cartForm, request, resolveClientIp(httpRequest));
            return ResponseEntity.ok(response);
        } catch (CommonServletException ex) {
            log.warn("VNPAY create rejected accountId={}: {}", loginInfo.getId(), ex.getMessage());
            return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (Exception ex) {
            log.error("VNPAY create failed accountId={}", loginInfo.getId(), ex);
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể tạo thanh toán VNPAY. Vui lòng thử lại.");
        }
    }

    @GetMapping("/status/{txnRef}")
    public ResponseEntity<?> status(@PathVariable String txnRef, HttpSession session) {
        UserLoginInfo loginInfo = getLoginInfo(session);
        if (loginInfo == null) {
            return errorResponse(HttpStatus.FORBIDDEN, "Vui lòng đăng nhập");
        }
        try {
            return ResponseEntity.ok(vnpayCheckoutService.getStatus(txnRef, loginInfo.getId()));
        } catch (CommonServletException ex) {
            return errorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    private CartForm resolveCartForm(HttpSession session, UserLoginInfo loginInfo) {
        Object sessionAttr = session.getAttribute("cartForm");
        CartForm cartForm = sessionAttr instanceof CartForm ? (CartForm) sessionAttr : new CartForm();
        if (CollectionUtils.isEmpty(cartForm.getOrderItems())) {
            CartForm fromDb = cartService.findFirstByAccountId(loginInfo.getId());
            if (fromDb != null && !CollectionUtils.isEmpty(fromDb.getOrderItems())) {
                cartForm.setOrderItems(fromDb.getOrderItems());
                session.setAttribute("cartForm", cartForm);
            }
        }
        return cartForm;
    }

    /** X-Forwarded-For có thể là "client, proxy1, proxy2" — VNPAY cần IP khách (7–45 ký tự). */
    private static String resolveClientIp(HttpServletRequest request) {
        String ip = StringUtils.trimToEmpty(StringUtils.substringBefore(StringUtil.getClientIpAddr(request), ","));
        return StringUtils.left(StringUtils.defaultIfBlank(ip, "127.0.0.1"), IP_MAX_LENGTH);
    }

    private static ResponseEntity<CommonResponse> errorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(CommonResponse.builder()
                .result(status.getReasonPhrase())
                .message(message)
                .build());
    }
}
