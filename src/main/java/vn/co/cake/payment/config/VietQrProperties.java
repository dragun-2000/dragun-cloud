package vn.co.cake.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "vietqr")
public class VietQrProperties {

    private boolean enabled = true;
    private Api api = new Api();
    private Client client = new Client();
    private Partner partner = new Partner();
    private Bank bank = new Bank();
    private Checkout checkout = new Checkout();

    @Data
    public static class Api {
        private String baseUrl = "https://dev.vietqr.org";
    }

    @Data
    public static class Client {
        private String username = "";
        private String password = "";
    }

    @Data
    public static class Partner {
        private String username = "";
        private String password = "";
        private String jwtSecret = "";
    }

    @Data
    public static class Bank {
        private String code = "VCB";
        private String account = "";
        private String holder = "";
    }

    @Data
    public static class Checkout {
        private int expireHours = 24;
        /** Thời gian giữ kho/hiệu lực checkout. Ưu tiên cấu hình này thay cho expireHours cũ. */
        private int expireMinutes = 15;
        /** Dev: nút mô phỏng webhook khi chạy localhost (VietQR không gọi được Transaction Sync). */
        private boolean sandboxSimulateEnabled = false;
        /** Trang thanh toán VietQR Pro (mở tab mới), token nối vào cuối URL. */
        private String paymentPageBaseUrl = "https://pro.vietqr.vn/test/qr-generated?token=";
    }
}
