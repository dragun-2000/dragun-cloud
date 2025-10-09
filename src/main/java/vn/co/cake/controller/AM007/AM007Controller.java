package vn.co.cake.controller.AM007;

import vn.co.cake.common.RequestPathConst;
import vn.co.cake.common.ScreenPathConst;
import vn.co.cake.controller.BaseController;
import vn.co.cake.entity.Account;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.request.AccountRequest;
import vn.co.cake.security.admin.AdminLoginInfo;
import vn.co.cake.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@Controller
@Slf4j
public class AM007Controller extends BaseController {

    private final AccountService accountService;

    public AM007Controller(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping(RequestPathConst.AM004_02_EDIT)
    public String editAccountPage(@PathVariable(ID) Long id, Model model, HttpSession session,
                                  HttpServletRequest request, HttpServletResponse response) throws CommonServletException, IOException {
        AdminLoginInfo adminLoginInfo = getLoginInfoAdmin(session);
        if (getLoginInfoAdmin(session) == null) {
            response.sendRedirect("");
        }
        Account account = accountService.getAccount(id);
        model.addAttribute("account", account);
        boolean setting = adminLoginInfo.getId().equals(id);
        request.getSession().setAttribute(SETTING, setting);
        addSideMenu(model, RequestPathConst.AM004);
        return ScreenPathConst.AM004_02_SCREEN;
    }

    @GetMapping(RequestPathConst.AM004_02_SETTING)
    public String settingAccountPage(Model model, HttpSession session, HttpServletRequest request,
                                     HttpServletResponse response) throws IOException {
        AdminLoginInfo adminLoginInfo = getLoginInfoAdmin(session);
        if (getLoginInfoAdmin(session) == null) {
            response.sendRedirect(baseUrl);
        }
        Account account = accountService.findAccountForAdmin(adminLoginInfo.getUsername());
        model.addAttribute("account", account);
        request.getSession().setAttribute(SETTING, true);
        return ScreenPathConst.AM004_02_SCREEN;
    }

    /**
     * Registration account
     *
     * @param accountRequest AccountRequest
     * @return screen url
     * */
    @PostMapping(RequestPathConst.AM004_UPDATE)
    public ResponseEntity<String> update(@RequestBody AccountRequest accountRequest, HttpSession session,
                                         HttpServletResponse response) {
        try {
            if (getLoginInfoAdmin(session) == null) {
                response.sendRedirect(baseUrl);
            }
            accountService.updateAccount(accountRequest);
        } catch (CommonServletException | IOException e) {
            return new ResponseEntity<>("Lỗi Cập Nhập Thông Tin Người Dùng", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>("Cập Nhập Lại Thông Tin Người Dùng Thành Công", HttpStatus.OK);
    }

    /**
     * Registration account
     *
     * @param ids accountId
     * @return screen url
     * */
    @PostMapping(RequestPathConst.AM004_DELETED)
    public void deleted(@RequestBody List<Long> ids, RedirectAttributes redirAttrs, HttpSession session,
                        HttpServletResponse response) {
        try {
            if (getLoginInfoAdmin(session) == null) {
                response.sendRedirect("");
            }
            accountService.deleted(ids);
            redirAttrs.addFlashAttribute(SUCCESS, "Đã xóa.");
        } catch (CommonServletException | IOException e) {
            log.error(e.getMessage());
        }
    }
}
