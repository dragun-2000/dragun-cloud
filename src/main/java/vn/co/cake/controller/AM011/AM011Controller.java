package vn.co.cake.controller.AM011;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.extern.slf4j.Slf4j;
import vn.co.cake.common.RequestPathConst;
import vn.co.cake.common.ScreenPathConst;
import vn.co.cake.controller.BaseController;
import vn.co.cake.payment.dto.VietQrPilotSettingsForm;
import vn.co.cake.payment.entity.VietQrPilotSettings;
import vn.co.cake.payment.service.VietQrPilotAccessService;
import vn.co.cake.enums.Authorities;
import vn.co.cake.security.admin.AdminLoginInfo;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@Controller
@Slf4j
public class AM011Controller extends BaseController {

  private final VietQrPilotAccessService vietQrPilotAccessService;

  public AM011Controller(VietQrPilotAccessService vietQrPilotAccessService) {
    this.vietQrPilotAccessService = vietQrPilotAccessService;
  }

  @GetMapping(RequestPathConst.AM011)
  public String settingsPage(Model model, HttpSession session, HttpServletResponse response) throws IOException {
    AdminLoginInfo admin = getLoginInfoAdmin(session);
    if (admin == null) {
      response.sendRedirect(baseUrl.concat(RequestPathConst.AM001_LOGIN));
      return null;
    }
    VietQrPilotSettings settings = vietQrPilotAccessService.getSettings();
    model.addAttribute("visibleToAll", settings.isVisibleToAll());
    model.addAttribute("hasPilotPassword", StringUtils.isNotBlank(settings.getPilotPasswordHash()));
    model.addAttribute("lastUpdated", settings.getUpdated());
    model.addAttribute("lastUpdater", settings.getUpdater());
    addSideMenu(model, RequestPathConst.AM011);
    return ScreenPathConst.AM011_SCREEN;
  }

  @PostMapping(value = RequestPathConst.AM011_SAVE, produces = MediaType.TEXT_PLAIN_VALUE)
  @ResponseBody
  public ResponseEntity<String> saveSettings(@RequestBody VietQrPilotSettingsForm form, HttpSession session) {
    AdminLoginInfo admin = getLoginInfoAdmin(session);
    if (admin == null) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");
    }
    if (admin.getAuthority() != Authorities.ROLE_ADMIN) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Chỉ ROLE_ADMIN mới được cấu hình VietQR pilot");
    }
    try {
      vietQrPilotAccessService.saveSettings(
          form.isVisibleToAll(),
          form.getPilotPassword(),
          admin.getUsername());
      log.info("AM011: VietQR pilot settings updated by {}", admin.getUsername());
      return ResponseEntity.ok("Đã lưu cấu hình VietQR");
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().body(ex.getMessage());
    } catch (Exception ex) {
      log.error("AM011 save failed: {}", ex.getMessage(), ex);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Không lưu được cấu hình");
    }
  }
}
