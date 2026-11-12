package vn.co.cake.vnpay.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VnpayAccessStatusResponse {

    /** true = mọi user dùng VNPAY bình thường */
    private boolean visibleToAll;

    /** true = đang chế độ pilot (cần mật khẩu nếu chưa unlock) */
    private boolean pilotMode;

    /** User hiện tại đã nhập đúng mật khẩu pilot trong session */
    private boolean unlocked;

    /** Có thể gọi API checkout VNPAY (sau khi đã bật feature) */
    private boolean canUseVnpay;

    private String pilotMessage;
}
