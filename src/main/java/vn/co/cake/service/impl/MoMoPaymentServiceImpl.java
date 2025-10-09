package vn.co.cake.service.impl;

import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.co.cake.request.PaymentRequest;
import vn.co.cake.service.MoMoPaymentService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HashMap;
import java.util.Map;

@Service
public class MoMoPaymentServiceImpl implements MoMoPaymentService {

    @Value("${cake.base.url}")
    protected String baseUrl;
    
    private final String partnerCode = "MOMOXXXXXX";
    private final String accessKey = "XXXXXXXXXXXX";
    private final String secretKey = "XXXXXXXXXXXX";
    private final String requestType = "captureWallet";
    private final String apiUrl = "https://test-payment.momo.vn/v2/gateway/api/create";

    public String createPaymentRequest(PaymentRequest paymentRequest) throws Exception {
        String requestId = partnerCode + System.currentTimeMillis();  // Tạo mã yêu cầu duy nhất
        String orderId = requestId;  // Mã đơn hàng
        String redirectUrl = String.format("%s/cart-detail", baseUrl);  // URL chuyển hướng sau khi thanh toán thành công
        String ipnUrl = String.format("%s/api/payment/ipn", baseUrl);  // URL nhận thông báo từ MoMo
        String amount = String.valueOf(paymentRequest.getAmount());  // Số tiền thanh toán
        String orderInfo = "Thanh toán đơn hàng " + paymentRequest.getOrderId();
        String extraData = "";  // Dữ liệu bổ sung nếu cần

        // Tạo chuỗi chữ ký
        String rawSignature = "accessKey=" + accessKey +
                "&amount=" + amount +
                "&extraData=" + extraData +
                "&ipnUrl=" + ipnUrl +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&partnerCode=" + partnerCode +
                "&redirectUrl=" + redirectUrl +
                "&requestId=" + requestId +
                "&requestType=" + requestType;
        
        String signature = HmacSHA256(rawSignature, secretKey);

        // Tạo đối tượng request
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("partnerCode", partnerCode);
        requestBody.put("accessKey", accessKey);
        requestBody.put("requestId", requestId);
        requestBody.put("amount", amount);
        requestBody.put("orderId", orderId);
        requestBody.put("orderInfo", orderInfo);
        requestBody.put("redirectUrl", redirectUrl);
        requestBody.put("ipnUrl", ipnUrl);
        requestBody.put("extraData", extraData);
        requestBody.put("requestType", requestType);
        requestBody.put("signature", signature);

        // Gửi yêu cầu POST đến MoMo
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

        // Xử lý kết quả trả về
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> responseBody = response.getBody();
            String payUrl = (String) responseBody.get("payUrl");
            return payUrl;  // Trả về URL thanh toán từ MoMo
        } else {
            throw new Exception("Cannot create payment request to MoMo");
        }
    }

    // Hàm hỗ trợ tạo chữ ký HMAC SHA256
    private String HmacSHA256(String data, String secret) throws Exception {
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        sha256Hmac.init(secretKeySpec);
        return Hex.encodeHexString(sha256Hmac.doFinal(data.getBytes()));
    }
}
