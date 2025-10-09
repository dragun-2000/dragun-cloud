package vn.co.cake.controller.SA002;

import vn.co.cake.common.RequestPathConst;
import vn.co.cake.common.ScreenPathConst;
import vn.co.cake.common.StringConst;
import vn.co.cake.controller.BaseController;
import vn.co.cake.enums.Authorities;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.request.ChangePasswordRequest;
import vn.co.cake.request.ForgetPasswordRequest;
import vn.co.cake.request.ResetPasswordRequest;
import vn.co.cake.security.user.UserLoginInfo;
import vn.co.cake.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Controller
@Slf4j
public class SA002Controller extends BaseController {

    private final AccountService accountService;

    public SA002Controller(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * forget password screen
     *
     * @return forget password screen
     * */
    @GetMapping(RequestPathConst.SA002_FORGET_PASSWORD)
    public String forgetPasswordView(HttpSession session) {
        UserLoginInfo userLoginInfo = getLoginInfo(session);
        return ScreenPathConst.SA002_01_SCREEN;
    }

    /**
     * change password screen
     *
     * @return change password screen
     * */
    @GetMapping(RequestPathConst.SA002_CHANGE_PASSWORD_VIEW_ACCOUNT)
    public String changePasswordView(@PathVariable String accountId, Model model, HttpServletRequest httpServletRequest) {
        model.addAttribute(StringConst.ROLE, Authorities.ROLE_USER.name());
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setAccountId(accountId);
        String setting = String.valueOf(httpServletRequest.getSession().getAttribute(SETTING));
        model.addAttribute(SETTING, setting);
        model.addAttribute(CHANGE_PASSWORD, request);
        return ScreenPathConst.SA002_03_SCREEN;
    }

    /**
     * Senmail mail for change password
     *
     * @param form information for send mail
     * @return verify MailAddress screen
     * */
    @PostMapping(RequestPathConst.SA002_SEND_MAIL)
    public ResponseEntity<String> verifyMailAddress(@RequestBody ForgetPasswordRequest form) {
        try {
            accountService.verifyMailForgetPasswordUser(form);
            return ResponseEntity.status(HttpStatus.OK).body(StringUtils.EMPTY);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Tài khoản không tồn tại");
        }
    }

    /**
     * change password screen
     *
     * @param accountId information for verify password
     * @param hash information for verify password
     * @return  screen reset password
     * */
    @GetMapping(RequestPathConst.SA002_VERIFY_USER)
    public String createNewPassword(@RequestParam(value = ACCOUNT_ID) String accountId,
                                    @RequestParam(value = HASH) String hash,
                                    Model model) throws CommonServletException {
        ResetPasswordRequest request = accountService.checkAccountInfo(accountId, hash);
        model.addAttribute(RESET_PASSWORD, request);
        return ScreenPathConst.SA002_02_SCREEN;
    }

    /**
     * reset password screen
     *
     * @param request information for reset password
     * @return login screen
     * */
    @PostMapping(RequestPathConst.SA002_RESET_PASSWORD)
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
     */
    @PostMapping(RequestPathConst.SA002_CHANGE_PASSWORD)
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordRequest request,
                                                 HttpServletRequest httpServletRequest, Model model,
                                                 RedirectAttributes redirectAttributes) {
        try {
            String setting = String.valueOf(httpServletRequest.getSession().getAttribute(SETTING));
            model.addAttribute(SETTING, setting);
            accountService.changePasswordAdmin(request, model, redirectAttributes);
            return new ResponseEntity<>("Đã Thay Đổi Mật Khẩu Thành Công", HttpStatus.CREATED);
        } catch (CommonServletException e) {
            model.addAttribute(CHANGE_PASSWORD, request);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
