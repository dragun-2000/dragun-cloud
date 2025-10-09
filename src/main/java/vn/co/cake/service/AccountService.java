package vn.co.cake.service;

import vn.co.cake.entity.Account;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.request.AccountRequest;
import vn.co.cake.request.AccountSearchRequest;
import vn.co.cake.request.ChangePasswordRequest;
import vn.co.cake.request.ForgetPasswordRequest;
import vn.co.cake.request.ResetPasswordRequest;
import vn.co.cake.security.admin.AdminLoginInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * AccountService
 */
public interface AccountService {

    Account getAccount(Long id) throws CommonServletException;

    Account findAccountForAdmin(String email);
    
    Account findAccountForMember(String email);

    void addAccount(AccountRequest accountRequest) throws CommonServletException;

    void updateAccount(AccountRequest accountRequest) throws CommonServletException;

    void deleted(List<Long> id) throws CommonServletException;

    void verifyMailForgetPassword(ForgetPasswordRequest form) throws CommonServletException;
    
    void verifyMailForgetPasswordUser(ForgetPasswordRequest form) throws CommonServletException;

    ResetPasswordRequest checkAccountInfo(String accountId, String hash) throws CommonServletException;

    void resetPassword(ResetPasswordRequest request) throws CommonServletException;

    void changePasswordAdmin(ChangePasswordRequest request, Model model, RedirectAttributes redirectAttributes) throws CommonServletException;

    void changePasswordForStaff(ChangePasswordRequest request, AdminLoginInfo adminLoginInfo, Model model, RedirectAttributes redirectAttributes) throws CommonServletException;

    Page<Account> getAllAccountByCondition(AccountSearchRequest accountSearchRequest, String email, Pageable pageable);

    String getBookmarkUrlLasted(String key);

    void deletedBookmarkUrlLasted(String key);

    void save(Account account);

    void updateLoginStatus(Long id, boolean statusLogin);
}
