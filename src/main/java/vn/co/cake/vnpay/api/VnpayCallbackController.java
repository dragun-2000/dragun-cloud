package vn.co.cake.vnpay.api;

import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.extern.slf4j.Slf4j;
import vn.co.cake.common.BaseConst;
import vn.co.cake.vnpay.VnpayConstants;
import vn.co.cake.vnpay.config.VnpayProperties;
import vn.co.cake.vnpay.dto.VnpayIpnResponse;
import vn.co.cake.vnpay.service.VnpayIpnService;
import vn.co.cake.vnpay.service.VnpayResponseMessages;
import vn.co.cake.vnpay.support.VnpayFormats;
import vn.co.cake.vnpay.support.VnpaySigner;

/**
 * Endpoint VNPAY gọi về:
 * <ul>
 *   <li>{@code GET /vnpay/ipn} — server-to-server, cập nhật kết quả. URL này gửi VNPAY đăng ký (HTTPS public).</li>
 *   <li>{@code GET /vnpay/return} — trình duyệt khách quay về; chỉ verify checksum + hiển thị, KHÔNG cập nhật DB.</li>
 * </ul>
 */
@Slf4j
@Controller
@RequestMapping("/vnpay")
public class VnpayCallbackController {

    private final VnpayIpnService vnpayIpnService;
    private final VnpayProperties properties;

    public VnpayCallbackController(VnpayIpnService vnpayIpnService, VnpayProperties properties) {
        this.vnpayIpnService = vnpayIpnService;
        this.properties = properties;
    }

    @GetMapping("/ipn")
    @ResponseBody
    public ResponseEntity<VnpayIpnResponse> ipn(HttpServletRequest request) {
        Map<String, String> params = extractVnpParams(request);
        log.info("VNPAY IPN received txnRef={} responseCode={} transactionStatus={}",
                params.get("vnp_TxnRef"), params.get("vnp_ResponseCode"), params.get("vnp_TransactionStatus"));
        VnpayIpnResponse response;
        try {
            response = vnpayIpnService.handle(params, request.getQueryString());
        } catch (Exception ex) {
            log.error("VNPAY IPN processing error txnRef={}", params.get("vnp_TxnRef"), ex);
            response = VnpayIpnResponse.unknownError();
        }
        log.info("VNPAY IPN response txnRef={} RspCode={}", params.get("vnp_TxnRef"), response.getRspCode());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/return")
    public String returnPage(HttpServletRequest request, HttpSession session, Model model) {
        Map<String, String> params = extractVnpParams(request);
        boolean validSignature = VnpaySigner.verify(params, properties.getHashSecret());
        String responseCode = params.get("vnp_ResponseCode");
        boolean success = validSignature && VnpayConstants.CODE_SUCCESS.equals(responseCode)
                && VnpayConstants.CODE_SUCCESS.equals(params.get("vnp_TransactionStatus"));

        model.addAttribute("isLogin", session.getAttribute(BaseConst.USER_SESSION) != null);
        model.addAttribute("validSignature", validSignature);
        model.addAttribute("success", success);
        model.addAttribute("txnRef", params.get("vnp_TxnRef"));
        model.addAttribute("amount", VnpayFormats.fromVnpAmount(params.get("vnp_Amount")));
        model.addAttribute("message", validSignature
                ? VnpayResponseMessages.forResponseCode(responseCode)
                : "Dữ liệu trả về không hợp lệ (sai chữ ký).");
        log.info("VNPAY return txnRef={} responseCode={} validSignature={}",
                params.get("vnp_TxnRef"), responseCode, validSignature);
        return "vnpay-return";
    }

    private static Map<String, String> extractVnpParams(HttpServletRequest request) {
        Map<String, String> params = new TreeMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (key.startsWith("vnp_") && values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });
        return params;
    }
}
