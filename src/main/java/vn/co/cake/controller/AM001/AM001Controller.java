package vn.co.cake.controller.AM001;

import vn.co.cake.common.BaseConst;
import vn.co.cake.common.RequestPathConst;
import vn.co.cake.common.ScreenPathConst;
import vn.co.cake.common.StringConst;
import vn.co.cake.controller.BaseController;
import vn.co.cake.entity.Account;
import vn.co.cake.enums.Authorities;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.request.ChangePasswordRequest;
import vn.co.cake.request.ForgetPasswordRequest;
import vn.co.cake.request.ResetPasswordRequest;
import vn.co.cake.security.admin.AdminLoginInfo;
import vn.co.cake.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Objects;

@Controller
@Slf4j
public class AM001Controller extends BaseController {

    private final AccountService accountService;

    public AM001Controller(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Login screen
     *
     * @return list CM screen
     * */
    @GetMapping(path = {RequestPathConst.AM, RequestPathConst.AM001})
    public String login(@RequestParam(value = LOGOUT, required = false) String logout, HttpServletResponse response, HttpServletRequest request) {
        if (StringUtils.isNotEmpty(request.getQueryString()) && request.getQueryString().equals("error")) {
            return ScreenPathConst.AM001_SCREEN;
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
                Account loggedInUser = accountService.findAccountForAdmin(auth.getName());
                if (Objects.nonNull(loggedInUser)) {
                    return BaseConst.REDIRECT + RequestPathConst.AM002;
                }
            }

            log.info("# Init user login");
        }

        return BaseConst.REDIRECT.concat(baseUrl).concat(RequestPathConst.AM001_LOGIN);
    }

    /**
     * Login screen
     *
     * @return login view screen
     * */
    @GetMapping(RequestPathConst.AM001_LOGIN)
    public String loginView() {
        return ScreenPathConst.AM001_SCREEN;
    }
    
    /**
     * forget password screen
     *
     * @return forget password screen
     * */
    @GetMapping(RequestPathConst.AM001_FORGET_PASSWORD)
    public String forgetPasswordView() {
        return ScreenPathConst.AM001_01_SCREEN;
    }

    /**
     * change password screen
     *
     * @return change password screen
     * */
    @GetMapping(RequestPathConst.AM001_CHANGE_PASSWORD_VIEW_ACCOUNT)
    public String changePasswordView(@PathVariable String accountId, Model model, HttpServletRequest httpServletRequest) {
        model.addAttribute(StringConst.ROLE, Authorities.ROLE_ADMIN.name());
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setAccountId(accountId);
        String setting = String.valueOf(httpServletRequest.getSession().getAttribute(SETTING));
        model.addAttribute(SETTING, setting);
        model.addAttribute(CHANGE_PASSWORD, request);
        addSideMenu(model, RequestPathConst.AM004);
        return ScreenPathConst.AM001_03_SCREEN;
    }

    /**
     * Senmail mail for change password
     *
     * @param form information for send mail
     * @return verify MailAddress screen
     * */
    @PostMapping(RequestPathConst.AM001_SEND_MAIL)
    public ResponseEntity<String> verifyMailAddress(@RequestBody ForgetPasswordRequest form) {
        try {
            accountService.verifyMailForgetPassword(form);
            return ResponseEntity.status(HttpStatus.OK).body(StringUtils.EMPTY);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Tài khoản không tồn tại");
        }
    }

    /**
     * change password screen
     *
     * @param key information for verify password
     * @param hash information for verify password
     * @return  screen reset password
     * */
    @GetMapping(RequestPathConst.AM001_VERIFY_USER)
    public String createNewPassword(@RequestParam(value = KEY) String key,
                                    @RequestParam(value = HASH) String hash,
                                    Model model) throws CommonServletException {
        ResetPasswordRequest request = accountService.checkAccountInfo(key, hash);
        model.addAttribute(RESET_PASSWORD, request);
        return ScreenPathConst.AM001_02_SCREEN;
    }

    /**
     * reset password screen
     *
     * @param request information for reset password
     * @return login screen
     * */
    @PostMapping(RequestPathConst.AM001_RESET_PASSWORD)
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            accountService.resetPassword(request);
            return ResponseEntity.status(HttpStatus.OK).body("");
        } catch (CommonServletException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    /**
     * change password screen
     *
     * @param request information for reset password
     * @return login screen
     * */
    @PostMapping(RequestPathConst.AM001_CHANGE_PASSWORD)
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordRequest request,
                                                 HttpServletRequest httpServletRequest, HttpSession session, Model model,
                                                 RedirectAttributes redirectAttributes) {
        try {
            AdminLoginInfo adminLoginInfo = getLoginInfoAdmin(session);
            String setting = String.valueOf(httpServletRequest.getSession().getAttribute(SETTING));
            model.addAttribute(SETTING, setting);
            if (Authorities.ROLE_STAFF.name().equals(adminLoginInfo.getAuthority().getText()) || TRUE.equals(setting)) {
                accountService.changePasswordForStaff(request, adminLoginInfo, model, redirectAttributes);
            } else {
                accountService.changePasswordAdmin(request, model, redirectAttributes);
            }
            return new ResponseEntity<>("Đã thay đổi thành công.", HttpStatus.CREATED);
        } catch (CommonServletException e) {
            model.addAttribute(CHANGE_PASSWORD, request);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
