package vn.co.cake.payment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VietQrAccessStatusResponse {

  /** true = mọi user dùng VietQR bình thường */
  private boolean visibleToAll;

  /** true = đang chế độ pilot (cần mật khẩu nếu chưa unlock) */
  private boolean pilotMode;

  /** User hiện tại đã nhập đúng mật khẩu pilot trong session */
  private boolean unlocked;

  /** Có thể chọn / gọi API VietQR */
  private boolean canUseVietQr;

  private String pilotMessage;
}
