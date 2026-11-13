package vn.co.cake.vnpay.config;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Cấu hình VNPAY ({@code vnpay.*}). Giá trị bí mật chỉ nạp qua biến môi trường — xem
 * application-*.properties và docker-compose.yml.
 */
@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "vnpay")
public class VnpayProperties {

    private boolean enabled = false;
    private String version = "2.1.0";
    private String tmnCode = "";
    private String hashSecret = "";
    private String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private String apiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
    private String returnUrl = "";
    private String orderType = "other";
    private String locale = "vn";
    /** Thời hạn thanh toán gửi VNPAY (vnp_ExpireDate), phút. */
    private int expireMinutes = 15;
    /**
     * Chờ thêm sau {@code expiresAt} trước khi job bắt đầu QueryDR (IPN có thể trễ).
     * Job chọn phiên khi {@code expiresAt <= now - grace}.
     */
    private int expireGraceMinutes = 5;
    /**
     * Phút sau {@code expiresAt}: nếu QueryDR vẫn lỗi / không kết luận được → huỷ đơn + hoàn kho.
     * Nên {@code >= expireGraceMinutes} (hoặc 0 khi muốn huỷ ngay sau hạn nếu QueryDR fail).
     */
    private int queryDrGiveUpMinutes = 5;
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 15000;

    /** Đủ thông tin bắt buộc để tạo URL thanh toán và ký checksum. */
    public boolean isConfigured() {
        return StringUtils.isNoneBlank(tmnCode, hashSecret, payUrl, returnUrl);
    }

    public boolean isUsable() {
        return enabled && isConfigured();
    }

    @PostConstruct
    void logConfigState() {
        if (enabled && !isConfigured()) {
            log.warn("VNPAY enabled nhưng thiếu cấu hình: tmnCode={}, hashSecret={}, returnUrl={} — "
                            + "thanh toán VNPAY sẽ bị ẩn cho tới khi bổ sung biến môi trường VNPAY_*",
                    StringUtils.isNotBlank(tmnCode) ? "OK" : "MISSING",
                    StringUtils.isNotBlank(hashSecret) ? "OK" : "MISSING",
                    StringUtils.isNotBlank(returnUrl) ? "OK" : "MISSING");
        } else {
            log.info("VNPAY enabled={}, configured={}, payUrl={}, expireMinutes={}, "
                            + "expireGraceMinutes={}, queryDrGiveUpMinutes={}",
                    enabled, isConfigured(), payUrl, expireMinutes, expireGraceMinutes, queryDrGiveUpMinutes);
        }
    }
}
