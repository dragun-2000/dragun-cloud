package vn.co.cake.vnpay.dto;

import lombok.Data;

@Data
public class VnpaySettingsForm {

    private boolean enabled;

    /** true = public; false = pilot (cần mật khẩu trên trang thanh toán) */
    private boolean visibleToAll;

    /** Để trống = giữ mật khẩu pilot hiện tại */
    private String pilotPassword;
}
