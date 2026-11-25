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
import vn.co.cake.enums.Authorities;
import vn.co.cake.payment.dto.VietQrPilotSettingsForm;
import vn.co.cake.payment.entity.VietQrPilotSettings;
import vn.co.cake.payment.service.VietQrPilotAccessService;
import vn.co.cake.security.admin.AdminLoginInfo;
import vn.co.cake.storefront.dto.SaleMenuSettingsForm;
import vn.co.cake.storefront.entity.SaleMenuStyle;
import vn.co.cake.storefront.entity.StorefrontSettings;
import vn.co.cake.storefront.service.StorefrontSettingsService;
import vn.co.cake.vnpay.config.VnpayProperties;
import vn.co.cake.vnpay.dto.VnpaySettingsForm;
import vn.co.cake.vnpay.entity.VnpaySettings;
import vn.co.cake.vnpay.service.VnpayPilotAccessService;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@Controller
@Slf4j
public class AM011Controller extends BaseController {

  private final VietQrPilotAccessService vietQrPilotAccessService;
  private final VnpayPilotAccessService vnpayPilotAccessService;
  private final VnpayProperties vnpayProperties;
  private final StorefrontSettingsService storefrontSettingsService;

  public AM011Controller(VietQrPilotAccessService vietQrPilotAccessService,
                         VnpayPilotAccessService vnpayPilotAccessService,
                         VnpayProperties vnpayProperties,
                         StorefrontSettingsService storefrontSettingsService) {
    this.vietQrPilotAccessService = vietQrPilotAccessService;
    this.vnpayPilotAccessService = vnpayPilotAccessService;
    this.vnpayProperties = vnpayProperties;
    this.storefrontSettingsService = storefrontSettingsService;
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

    VnpaySettings vnpaySettings = vnpayPilotAccessService.getSettings();
    model.addAttribute("vnpayEnabled", vnpaySettings.isEnabled());
    model.addAttribute("vnpayVisibleToAll", vnpaySettings.isVisibleToAll());
    model.addAttribute("vnpayHasPilotPassword", StringUtils.isNotBlank(vnpaySettings.getPilotPasswordHash()));
    model.addAttribute("vnpayConfigured", vnpayProperties.isConfigured());
    model.addAttribute("vnpayLastUpdated", vnpaySettings.getUpdated());
    model.addAttribute("vnpayLastUpdater", vnpaySettings.getUpdater());

    StorefrontSettings storefrontSettings = storefrontSettingsService.getSettings();
    SaleMenuStyle saleMenuStyle = storefrontSettings.getSaleMenuStyle() != null
        ? storefrontSettings.getSaleMenuStyle() : SaleMenuStyle.SALE;
    model.addAttribute("saleMenuStyle", saleMenuStyle.name());
    model.addAttribute("saleMenuLastUpdated", storefrontSettings.getUpdated());
    model.addAttribute("saleMenuLastUpdater", storefrontSettings.getUpdater());

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

  @PostMapping(value = RequestPathConst.AM011_VNPAY_SAVE, produces = MediaType.TEXT_PLAIN_VALUE)
  @ResponseBody
  public ResponseEntity<String> saveVnpaySettings(@RequestBody VnpaySettingsForm form, HttpSession session) {
    AdminLoginInfo admin = getLoginInfoAdmin(session);
    if (admin == null) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");
    }
    if (admin.getAuthority() != Authorities.ROLE_ADMIN) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Chỉ ROLE_ADMIN mới được cấu hình VNPAY");
    }
    try {
      vnpayPilotAccessService.saveSettings(
          form.isEnabled(),
          form.isVisibleToAll(),
          form.getPilotPassword(),
          admin.getUsername());
      log.info("AM011: VNPAY enabled={} visibleToAll={} by {}",
          form.isEnabled(), form.isVisibleToAll(), admin.getUsername());
      if (!form.isEnabled()) {
        return ResponseEntity.ok("Đã tắt thanh toán VNPAY");
      }
      return ResponseEntity.ok(form.isVisibleToAll()
          ? "Đã bật VNPAY công khai cho tất cả user"
          : "Đã bật VNPAY ở chế độ pilot (cần mật khẩu)");
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().body(ex.getMessage());
    } catch (Exception ex) {
      log.error("AM011 VNPAY save failed: {}", ex.getMessage(), ex);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Không lưu được cấu hình VNPAY");
    }
  }

  @PostMapping(value = RequestPathConst.AM011_SALE_MENU_SAVE, produces = MediaType.TEXT_PLAIN_VALUE)
  @ResponseBody
  public ResponseEntity<String> saveSaleMenu(@RequestBody SaleMenuSettingsForm form, HttpSession session) {
    AdminLoginInfo admin = getLoginInfoAdmin(session);
    if (admin == null) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");
    }
    if (admin.getAuthority() != Authorities.ROLE_ADMIN) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Chỉ ROLE_ADMIN mới được cấu hình hiển thị menu");
    }
    try {
      SaleMenuStyle style = SaleMenuStyle.parse(form.getSaleMenuStyle());
      storefrontSettingsService.saveSaleMenuStyle(style, admin.getUsername());
      log.info("AM011: sale menu style={} by {}", style, admin.getUsername());
      return ResponseEntity.ok(style == SaleMenuStyle.BLACK_FRIDAY
          ? "Menu đang hiển thị BLACK FRIDAY (màu đỏ)"
          : "Menu đang hiển thị SALE");
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().body(ex.getMessage());
    } catch (Exception ex) {
      log.error("AM011 sale menu save failed: {}", ex.getMessage(), ex);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Không lưu được cấu hình hiển thị menu");
    }
  }
}
