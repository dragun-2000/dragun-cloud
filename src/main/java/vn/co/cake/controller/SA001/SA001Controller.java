package vn.co.cake.controller.SA001;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.co.cake.common.BaseConst;
import vn.co.cake.common.MessageConst;
import vn.co.cake.common.RequestPathConst;
import vn.co.cake.common.ScreenPathConst;
import vn.co.cake.controller.BaseController;
import vn.co.cake.dto.CartForm;
import vn.co.cake.entity.Account;
import vn.co.cake.entity.Province;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.repository.ProvinceRepository;
import vn.co.cake.helper.SmsService;
import vn.co.cake.request.AccountRequest;
import vn.co.cake.request.RegisterOtpRequest;
import vn.co.cake.security.user.UserLoginInfo;
import vn.co.cake.service.AccountService;
import vn.co.cake.service.OtpVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import vn.co.cake.utils.FunctionUtil;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Objects;

@Controller
@Slf4j
@SessionAttributes("cartForm") 
public class SA001Controller extends BaseController {

    private final AccountService accountService;
    private final ProvinceRepository provinceRepository;
    private final SmsService smsService;
    private final OtpVerificationService otpVerificationService;

    public SA001Controller(AccountService accountService,
                           ProvinceRepository provinceRepository,
                           SmsService smsService,
                           OtpVerificationService otpVerificationService) {
        this.accountService = accountService;
        this.provinceRepository = provinceRepository;
        this.smsService = smsService;
        this.otpVerificationService = otpVerificationService;
    }

    @ModelAttribute("cartForm")
    public CartForm createCartForm() {
        return new CartForm();
    }

    /**
     * Login screen
     *
     * @return login view screen
     * */
    @RequestMapping(path ={ RequestPathConst.SA001_LOGIN})
    public String loginEpoView() {
        return ScreenPathConst.SA001_SCREEN;
    }
    
    /**
     * Login screen
     *
     * @return list CM screen
     * */
    @GetMapping(path = {RequestPathConst.SA, RequestPathConst.SA001})
    public String loginEpo(@RequestParam(value = LOGOUT, required = false) String logout, HttpServletResponse response, HttpServletRequest request) {
        if (StringUtils.isNotEmpty(request.getQueryString()) && request.getQueryString().equals("error")) {
            return ScreenPathConst.SA001_SCREEN;
        }

        if (logout != null) {
            Cookie cookie = new Cookie(BaseConst.BID, null);
            cookie.setHttpOnly(true);
            cookie.setMaxAge(0);
            response.addCookie(cookie);
            log.info("# User logout success");
        } else {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (Objects.nonNull(auth) && !(auth instanceof AnonymousAuthenticationToken)) {
                Account loggedInUser = accountService.findAccountForMember(auth.getName());
                if (Objects.nonNull(loggedInUser)) {
                    loggedInUser.setFirstLogin(true);
                    accountService.save(loggedInUser);
                    return BaseConst.REDIRECT + RequestPathConst.HOME;
                }
            }

            log.info("# Init user login");
        }

        return BaseConst.REDIRECT.concat(baseUrlSa);
    }

    @GetMapping(RequestPathConst.SA001_01_REGISTER_VIEW)
    public String registerPage(Model model, @ModelAttribute("cartForm") CartForm cartForm) {
        List<Province> provinces = provinceRepository.findAll();
        model.addAttribute("provinces", provinces);
        model.addAttribute("account", new Account());
        FunctionUtil.updateCartQuantity(model, cartForm);
        return ScreenPathConst.SA001_01_SCREEN;
    }
    
    @GetMapping(RequestPathConst.SA001_PROFILE)
    public String profile(Model model, HttpSession session, @ModelAttribute("cartForm") CartForm cartForm) throws CommonServletException {
        UserLoginInfo loginInfo = getLoginInfo(session);
        Account account = new Account();
        if (loginInfo != null) {
            account = accountService.getAccount(loginInfo.getId());
        }
        List<Province> provinces = provinceRepository.findAll();
        model.addAttribute("provinces", provinces);
        model.addAttribute("account", account);
        FunctionUtil.updateCartQuantity(model, cartForm);
        return ScreenPathConst.SA001_PROFILE_SCREEN;
    }

    /**
     * Send OTP for registration (Step 1)
     *
     * @param accountRequest AccountRequest
     * @return ResponseEntity with status
     */
    @PostMapping(RequestPathConst.SA001_REGISTER_SEND_OTP)
    public ResponseEntity<?> sendOtp(@RequestBody AccountRequest accountRequest) {
        try {
            if (StringUtils.isBlank(accountRequest.getPhone())) {
                return new ResponseEntity<>("Số điện thoại không được để trống!", HttpStatus.BAD_REQUEST);
            }

            if (!otpVerificationService.checkRateLimit(accountRequest.getPhone())) {
                return new ResponseEntity<>("Bạn đã gửi quá nhiều OTP. Vui lòng thử lại sau 1 giờ.", HttpStatus.BAD_REQUEST);
            }

            String otpCode = otpVerificationService.generateOtp();

            boolean smsSent = true;
            try {
                smsSent = smsService.sendOtp(accountRequest.getPhone(), otpCode);
            } catch (Exception e) {
                log.warn("SMS send failed: {}", e.getMessage());
                return new ResponseEntity<>("Không thể gửi SMS. Vui lòng thử lại sau.", HttpStatus.INTERNAL_SERVER_ERROR);
            }

           if (!smsSent) {
               log.warn("SMS not sent for phone: {}", accountRequest.getPhone());
               return new ResponseEntity<>("Không thể gửi SMS. Vui lòng kiểm tra số điện thoại và thử lại.", HttpStatus.INTERNAL_SERVER_ERROR);
           }

            boolean stored = otpVerificationService.storeRegistrationData(accountRequest.getPhone(), accountRequest, otpCode);
            if (!stored) {
                log.error("Failed to store OTP data for phone: {}", accountRequest.getPhone());
                return new ResponseEntity<>("Không thể lưu OTP. Vui lòng thử lại sau.", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            log.info("OTP sent for phone: {}", accountRequest.getPhone());
            log.info("[OTP] Mã OTP cho số {}: {}", accountRequest.getPhone(), otpCode);

            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("status", "success");
            body.put("message", "Mã OTP đã được gửi đến số điện thoại của bạn");
            body.put("expiresIn", 300);
            return new ResponseEntity<>(body, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error sending OTP: {}", e.getMessage(), e);
            return new ResponseEntity<>("Có lỗi xảy ra khi gửi OTP. Vui lòng thử lại.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Verify OTP and create account (Step 2)
     *
     * @param otpRequest RegisterOtpRequest
     * @return ResponseEntity with status
     */
    @PostMapping(RequestPathConst.SA001_REGISTER_VERIFY_OTP)
    public ResponseEntity<String> verifyOtp(@RequestBody RegisterOtpRequest otpRequest) {
        try {
            if (StringUtils.isBlank(otpRequest.getPhone()) || StringUtils.isBlank(otpRequest.getOtpCode())) {
                return new ResponseEntity<>("Vui lòng nhập đầy đủ thông tin!", HttpStatus.BAD_REQUEST);
            }

            // Verify OTP
            OtpVerificationService.OtpData otpData = otpVerificationService.verifyOtp(otpRequest.getPhone(), otpRequest.getOtpCode());
            
            if (otpData == null) {
                OtpVerificationService.OtpData existingData = otpVerificationService.getRegistrationData(otpRequest.getPhone());
                if (existingData == null) {
                    return new ResponseEntity<>("Phiên đăng ký đã hết hạn. Vui lòng đăng ký lại.", HttpStatus.BAD_REQUEST);
                } else if (System.currentTimeMillis() > existingData.getExpiresAt()) {
                    return new ResponseEntity<>("Mã OTP đã hết hạn. Vui lòng gửi lại.", HttpStatus.BAD_REQUEST);
                } else if (existingData.getAttemptCount() >= 3) {
                    return new ResponseEntity<>("Bạn đã nhập sai quá nhiều lần. Vui lòng gửi lại OTP.", HttpStatus.BAD_REQUEST);
                } else {
                    return new ResponseEntity<>("Mã OTP không đúng. Vui lòng thử lại.", HttpStatus.BAD_REQUEST);
                }
            }

            // Create account
            accountService.addAccount(otpData.getAccountRequest());

            // Delete OTP data
            otpVerificationService.deleteRegistrationData(otpRequest.getPhone());

            log.info("Account created successfully for phone: {}", otpRequest.getPhone());
            return new ResponseEntity<>("Đăng Ký Tài Khoản Người Dùng Thành Công", HttpStatus.CREATED);

        } catch (CommonServletException e) {
            log.error("SA001_VERIFY_OTP error: {}", e.getMessage());
            String message = "Đăng ký tài khoản không thành công";
            if (MessageConst.PASSWORD_RE_ENTER_NOT_MATCH.equals(e.getMessage())) {
                message = MessageConst.PASSWORD_RE_ENTER_NOT_MATCH;
            } else if (MessageConst.EMAIL_ALREADY_EXISTS.equals(e.getMessage())) {
                message = MessageConst.EMAIL_ALREADY_EXISTS;
            }
            return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Error verifying OTP: {}", e.getMessage(), e);
            return new ResponseEntity<>("Có lỗi xảy ra khi xác thực OTP. Vui lòng thử lại.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Resend OTP
     *
     * @param otpRequest RegisterOtpRequest (only phone needed)
     * @return ResponseEntity with status
     */
    @PostMapping(RequestPathConst.SA001_REGISTER_RESEND_OTP)
    public ResponseEntity<?> resendOtp(@RequestBody RegisterOtpRequest otpRequest) {
        try {
            if (StringUtils.isBlank(otpRequest.getPhone())) {
                return new ResponseEntity<>("Số điện thoại không được để trống!", HttpStatus.BAD_REQUEST);
            }

            // Check rate limit
            if (!otpVerificationService.checkRateLimit(otpRequest.getPhone())) {
                return new ResponseEntity<>("Bạn đã gửi quá nhiều OTP. Vui lòng thử lại sau 1 giờ.", HttpStatus.BAD_REQUEST);
            }

            // Get existing registration data
            OtpVerificationService.OtpData existingData = otpVerificationService.getRegistrationData(otpRequest.getPhone());
            if (existingData == null) {
                return new ResponseEntity<>("Phiên đăng ký đã hết hạn. Vui lòng đăng ký lại.", HttpStatus.BAD_REQUEST);
            }

            // Generate new OTP
            String newOtpCode = otpVerificationService.generateOtp();

            // Send SMS
            boolean smsSent = smsService.sendOtp(otpRequest.getPhone(), newOtpCode);
            if (!smsSent) {
                return new ResponseEntity<>("Không thể gửi SMS. Vui lòng thử lại sau.", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // Update OTP data
            otpVerificationService.storeRegistrationData(otpRequest.getPhone(), existingData.getAccountRequest(), newOtpCode);

            log.info("OTP resent successfully to phone: {}", otpRequest.getPhone());
            return new ResponseEntity<>(new java.util.HashMap<String, Object>() {{
                put("status", "success");
                put("message", "Mã OTP mới đã được gửi đến số điện thoại của bạn");
                put("expiresIn", 300);
            }}, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error resending OTP: {}", e.getMessage(), e);
            return new ResponseEntity<>("Có lỗi xảy ra khi gửi lại OTP. Vui lòng thử lại.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Registration account (Legacy endpoint - kept for backward compatibility)
     * This endpoint is now deprecated, use send-otp and verify-otp instead
     *
     * @param accountRequest AccountRequest
     * @return screen url
     * */
    @PostMapping(RequestPathConst.SA001_REGISTER)
    public ResponseEntity<String> register(@RequestBody AccountRequest accountRequest) {
        String message = "Đăng Kí Tài Khoản Người Dùng Thành Công";
        try {
            accountService.addAccount(accountRequest);
        } catch (CommonServletException e) {
            log.error("SA001_REGISTER error: {}", e.getMessage());
            message = "Đăng ký tài khoản không thành công";
            if (MessageConst.PASSWORD_RE_ENTER_NOT_MATCH.equals(e.getMessage())) {
                message = MessageConst.PASSWORD_RE_ENTER_NOT_MATCH;
            } else if (MessageConst.EMAIL_ALREADY_EXISTS.equals(e.getMessage())) {
                message = MessageConst.EMAIL_ALREADY_EXISTS;
            }
            return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(message, HttpStatus.CREATED);
    }
}

