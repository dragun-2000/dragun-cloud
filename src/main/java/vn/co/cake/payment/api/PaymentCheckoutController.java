package vn.co.cake.payment.api;

import java.util.Objects;

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
import vn.co.cake.payment.PaymentConstants;
import vn.co.cake.payment.config.VietQrProperties;
import vn.co.cake.payment.dto.PaymentCheckoutResponse;
import vn.co.cake.payment.dto.PaymentCheckoutStatusResponse;
import vn.co.cake.payment.service.CodCheckoutService;
import vn.co.cake.payment.service.VietQrCheckoutService;
import vn.co.cake.payment.support.PaymentCheckoutFlowLog;
import vn.co.cake.request.OrderDetailRequest;
import vn.co.cake.response.CommonResponse;
import vn.co.cake.security.user.UserLoginInfo;
import vn.co.cake.service.CartService;

import javax.servlet.http.HttpSession;

@Slf4j
@RestController
@RequestMapping("/api/payment")
public class PaymentCheckoutController extends BaseController {

    private final CodCheckoutService codCheckoutService;
    private final VietQrCheckoutService vietQrCheckoutService;
    private final CartService cartService;
    private final VietQrProperties vietQrProperties;

    public PaymentCheckoutController(CodCheckoutService codCheckoutService,
                                     VietQrCheckoutService vietQrCheckoutService,
                                     CartService cartService,
                                     VietQrProperties vietQrProperties) {
        this.codCheckoutService = codCheckoutService;
        this.vietQrCheckoutService = vietQrCheckoutService;
        this.cartService = cartService;
        this.vietQrProperties = vietQrProperties;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createPayment(@RequestBody OrderDetailRequest request, HttpSession session) {
        UserLoginInfo loginInfo = getLoginInfo(session);
        if (Objects.isNull(loginInfo)) {
            log.warn("Payment create rejected: user not logged in");
            return errorResponse(HttpStatus.FORBIDDEN, "Vui lòng đăng nhập trước khi đặt hàng");
        }

        CartForm cartForm = resolveCartForm(session, loginInfo);
        if (CollectionUtils.isEmpty(cartForm.getOrderItems())) {
            log.warn("Payment create rejected: empty cart, accountId={}", loginInfo.getId());
            return errorResponse(HttpStatus.BAD_REQUEST, "Giỏ hàng trống. Vui lòng thêm sản phẩm trước khi thanh toán.");
        }

        String paymentMethod = resolvePaymentMethod(request.getPaymentMethod());
        log.info("Payment create start: accountId={}, method={}, cartItems={}",
                loginInfo.getId(), paymentMethod, cartForm.getOrderItems().size());

        try {
            if (PaymentConstants.METHOD_COD.equals(paymentMethod)) {
                PaymentCheckoutFlowLog.step("-", 1,
                        "API POST /api/payment/create — COD, accountId=%s", loginInfo.getId());
                PaymentCheckoutResponse response = codCheckoutService.placeOrder(loginInfo.getId(), cartForm, request);
                return ResponseEntity.ok(response);
            }
            if (PaymentConstants.METHOD_VIETQR.equals(paymentMethod)) {
                PaymentCheckoutFlowLog.step("-", 1,
                        "API POST /api/payment/create — VietQR, accountId=%s", loginInfo.getId());
                PaymentCheckoutResponse response = vietQrCheckoutService.startCheckout(loginInfo.getId(), cartForm, request);
                return ResponseEntity.ok(response);
            }
            return errorResponse(HttpStatus.BAD_REQUEST, "Phương thức thanh toán không được hỗ trợ: " + paymentMethod);
        } catch (CommonServletException ex) {
            log.warn("Payment create failed (business): accountId={}, method={}, message={}",
                    loginInfo.getId(), paymentMethod, ex.getMessage());
            return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (Exception ex) {
            log.error("Payment create failed: accountId={}, method={}", loginInfo.getId(), paymentMethod, ex);
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, resolveErrorMessage(ex));
        }
    }

    @GetMapping("/status/{vietqrOrderId}")
    public ResponseEntity<?> paymentStatus(@PathVariable String vietqrOrderId, HttpSession session) {
        UserLoginInfo loginInfo = getLoginInfo(session);
        if (loginInfo == null) {
            return errorResponse(HttpStatus.FORBIDDEN, "Vui lòng đăng nhập");
        }
        try {
            PaymentCheckoutStatusResponse response = vietQrCheckoutService.getCheckoutDetails(
                    vietqrOrderId, loginInfo.getId());
            return ResponseEntity.ok(response);
        } catch (CommonServletException ex) {
            return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @GetMapping("/checkout/{vietqrOrderId}")
    public ResponseEntity<?> checkoutDetails(@PathVariable String vietqrOrderId, HttpSession session) {
        return paymentStatus(vietqrOrderId, session);
    }

    @PostMapping("/sandbox-simulate/{vietqrOrderId}")
    public ResponseEntity<?> sandboxSimulate(@PathVariable String vietqrOrderId, HttpSession session) {
        if (!vietQrProperties.getCheckout().isSandboxSimulateEnabled()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        UserLoginInfo loginInfo = getLoginInfo(session);
        if (loginInfo == null) {
            return errorResponse(HttpStatus.FORBIDDEN, "Vui lòng đăng nhập");
        }
        try {
            PaymentCheckoutFlowLog.step(vietqrOrderId, 1,
                    "API POST sandbox-simulate — accountId=%s", loginInfo.getId());
            vietQrCheckoutService.runSandboxPaymentConfirm(vietqrOrderId, loginInfo.getId());
            PaymentCheckoutStatusResponse response = vietQrCheckoutService.getCheckoutDetails(
                    vietqrOrderId, loginInfo.getId());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Sandbox simulate failed: orderId={}", vietqrOrderId, ex);
            return errorResponse(HttpStatus.BAD_REQUEST, resolveErrorMessage(ex));
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

    private static String resolvePaymentMethod(String paymentMethod) {
        if (PaymentConstants.METHOD_TRANSFER_LEGACY.equals(paymentMethod)) {
            return PaymentConstants.METHOD_VIETQR;
        }
        return paymentMethod;
    }

    private static String resolveErrorMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "Đã xảy ra lỗi khi tạo đơn hàng. Vui lòng thử lại.";
        }
        if (message.contains("401")) {
            return "Không thể kết nối VietQR (xác thực thất bại). Kiểm tra vietqr.client.username và vietqr.client.password trong cấu hình.";
        }
        if (message.contains("VietQR")) {
            return message;
        }
        return message;
    }

    private static ResponseEntity<CommonResponse> errorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(CommonResponse.builder()
                .result(status.getReasonPhrase())
                .message(message)
                .build());
    }
}
