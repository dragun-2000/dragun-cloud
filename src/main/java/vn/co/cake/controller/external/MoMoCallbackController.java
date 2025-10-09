package vn.co.cake.controller.external;

import org.apache.commons.codec.binary.Hex;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class MoMoCallbackController {

    @PostMapping("/ipn")
    public ResponseEntity<String> handleMoMoIPN(@RequestBody Map<String, String> requestBody) {
        try {
            // Lấy các thông tin từ MoMo callback
            String partnerCode = requestBody.get("partnerCode");
            String orderId = requestBody.get("orderId");
            String requestId = requestBody.get("requestId");
            String amount = requestBody.get("amount");
            String resultCode = requestBody.get("resultCode");
            String signature = requestBody.get("signature");

            // Tạo lại chuỗi để kiểm tra chữ ký hợp lệ
            String rawSignature = "partnerCode=" + partnerCode +
                    "&orderId=" + orderId +
                    "&requestId=" + requestId +
                    "&amount=" + amount +
                    "&resultCode=" + resultCode;

            String expectedSignature = HmacSHA256(rawSignature, "YOUR_SECRET_KEY");

            // Kiểm tra tính hợp lệ của chữ ký
            if (!signature.equals(expectedSignature)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
            }

            // Kiểm tra kết quả thanh toán
            if ("0".equals(resultCode)) {
                // Thanh toán thành công
                // Cập nhật trạng thái đơn hàng trong database
                System.out.println("Thanh toán thành công cho đơn hàng: " + orderId);
            } else {
                // Thanh toán thất bại
                System.out.println("Thanh toán thất bại cho đơn hàng: " + orderId);
            }

            // Trả về cho MoMo
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error when processing IPN");
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
