package vn.co.cake.controller.AM004;

import org.apache.commons.lang3.StringUtils;
import vn.co.cake.common.MessageConst;
import vn.co.cake.common.RequestPathConst;
import vn.co.cake.common.ScreenPathConst;
import vn.co.cake.controller.BaseController;
import vn.co.cake.entity.Account;
import vn.co.cake.entity.Ward;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.repository.WardRepository;
import vn.co.cake.request.AccountRequest;
import vn.co.cake.request.AccountSearchRequest;
import vn.co.cake.security.admin.AdminLoginInfo;
import vn.co.cake.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class AM004Controller extends BaseController {

    private final AccountService accountService;
    private final WardRepository wardRepository;

    public AM004Controller(AccountService accountService, WardRepository wardRepository) {
        this.accountService = accountService;
        this.wardRepository = wardRepository;
    }

    @GetMapping(RequestPathConst.AM004_01)
    public String registerPage(Model model, HttpSession session, HttpServletResponse response) throws IOException {
        if (getLoginInfoAdmin(session) == null) {
            response.sendRedirect(baseUrl);
        }
        model.addAttribute("account", new Account());
        addSideMenu(model, RequestPathConst.AM004);
        return ScreenPathConst.AM004_01_SCREEN;
    }

    /**
     * Registration account
     *
     * @param accountRequest AccountRequest
     * @return screen url
     * */
    @PostMapping(RequestPathConst.AM004_REGISTER)
    public ResponseEntity<String> register(@RequestBody AccountRequest accountRequest, HttpSession session,
                                           HttpServletResponse response) {         
        String message = "Đăng kí Tài Khoản Nhân viên Thành Công!";
        try {
            if (getLoginInfoAdmin(session) == null) {
                response.sendRedirect(baseUrl);
            }
            accountService.addAccount(accountRequest);
        } catch (CommonServletException | IOException e) {
            log.error("AM004_REGISTER error: {}", e.getMessage());
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

    /**
     * list Account display
     *
     * @param accountSearchRequest search condition
     * @param pageable paging
     * @return list CM display
     * */
    @GetMapping(RequestPathConst.AM004)
    public String listAccounts(AccountSearchRequest accountSearchRequest, 
                               @PageableDefault(size = SIZE_DEFAULT, sort = {SORT_DEFAULT}, direction = Sort.Direction.DESC) Pageable pageable,
                               Model model, HttpSession session) throws CommonServletException {
        AdminLoginInfo info = getLoginInfoAdmin(session);
        if (info == null) {
            return ScreenPathConst.AM001_SCREEN;
        }

        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
        Page<Account> accounts = accountService.getAllAccountByCondition(accountSearchRequest, info.getEmail(), pageable);
        List<String> wardIds = accounts.stream().map(Account::getWard).collect(Collectors.toList());
        Map<String, Ward> wardMap = wardRepository.findAllByCodeIn(wardIds).stream().collect(Collectors.toMap(Ward::getCode, x -> x));
        accounts.getContent().forEach(account -> {
            Ward ward = wardMap.get(account.getWard());
            if (Objects.nonNull(ward)) {
                account.setAddress(account.getAddress() + StringUtils.SPACE + ward.getPathWithType());
            }
        });
        model.addAttribute(ACCOUNT_DATA, accounts.getContent());
        setPaginationAttribute(pageable, accounts.getTotalElements(), RequestPathConst.AM004, model, settingConditionForAccount(accountSearchRequest));

        model.addAttribute(SEARCH_CONDITION, accountSearchRequest);

        addSideMenu(model, RequestPathConst.AM004);
        return ScreenPathConst.AM004_SCREEN;
    }

    /**
     * Export customer list to Excel
     *
     * @param accountSearchRequest search condition
     * @param session HttpSession
     * @param response HttpServletResponse
     * @throws IOException
     */
    @GetMapping(RequestPathConst.AM004_EXPORT_EXCEL)
    public void exportExcel(AccountSearchRequest accountSearchRequest,
                           HttpSession session,
                           HttpServletResponse response) throws IOException {
        AdminLoginInfo info = getLoginInfoAdmin(session);
        if (info == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Unauthorized");
            return;
        }

        // Call service to generate Excel file
        byte[] excelBytes = accountService.exportAccountsToExcel(accountSearchRequest, info.getEmail());

        // Set response headers
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = "DanhSachKhachHang_" + timestamp + ".xlsx";
        
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setContentLength(excelBytes.length);
        
        // Write Excel bytes to response
        response.getOutputStream().write(excelBytes);
        response.getOutputStream().flush();
        
        log.info("Exported Excel file: {}", filename);
    }

    private Map<String, Object> settingConditionForAccount(AccountSearchRequest searchForm) {
        Map<String, Object> conditionMaps = new HashMap<>();
        conditionMaps.put("staffName", searchForm.getKeyword());
        return conditionMaps;
    }
}
