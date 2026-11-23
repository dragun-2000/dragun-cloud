package vn.co.cake.controller.store;

import java.util.Objects;

import javax.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import vn.co.cake.controller.BaseController;
import vn.co.cake.dto.CustomerOrderCancelResponse;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.response.CommonResponse;
import vn.co.cake.security.user.UserLoginInfo;
import vn.co.cake.service.CustomerOrderCancelService;

@Slf4j
@RestController
@RequestMapping("/api/orders")
public class CustomerOrderCancelController extends BaseController {

    private final CustomerOrderCancelService customerOrderCancelService;

    public CustomerOrderCancelController(CustomerOrderCancelService customerOrderCancelService) {
        this.customerOrderCancelService = customerOrderCancelService;
    }

    @PostMapping("/{code}/cancel")
    public ResponseEntity<?> cancel(@PathVariable("code") String code, HttpSession session) {
        UserLoginInfo loginInfo = getLoginInfo(session);
        if (Objects.isNull(loginInfo)) {
            return errorResponse(HttpStatus.FORBIDDEN, "Vui lòng đăng nhập");
        }
        try {
            CustomerOrderCancelResponse response = customerOrderCancelService.cancel(code, loginInfo.getId());
            return ResponseEntity.ok(response);
        } catch (CommonServletException ex) {
            log.warn("Customer cancel rejected accountId={} code={}: {}", loginInfo.getId(), code, ex.getMessage());
            return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (Exception ex) {
            log.error("Customer cancel failed accountId={} code={}", loginInfo.getId(), code, ex);
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể hủy đơn. Vui lòng thử lại.");
        }
    }

    private static ResponseEntity<CommonResponse> errorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(CommonResponse.builder()
                .result(status.getReasonPhrase())
                .message(message)
                .build());
    }
}
