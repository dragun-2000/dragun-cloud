package vn.co.cake.payment.dto;

import lombok.Data;

@Data
public class VietQrPilotSettingsForm {

  private boolean visibleToAll;

  /** Để trống = giữ mật khẩu pilot hiện tại */
  private String pilotPassword;
}
