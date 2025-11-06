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
import vn.co.cake.request.AccountRequest;
import vn.co.cake.security.user.UserLoginInfo;
import vn.co.cake.service.AccountService;
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

    public SA001Controller(AccountService accountService,
                           ProvinceRepository provinceRepository) {
        this.accountService = accountService;
        this.provinceRepository = provinceRepository;
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
     * Registration account
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

