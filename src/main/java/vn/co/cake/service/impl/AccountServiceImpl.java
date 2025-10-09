package vn.co.cake.service.impl;

import vn.co.cake.common.MessageConst;
import vn.co.cake.dto.GenericMailForm;
import vn.co.cake.entity.Account;
import vn.co.cake.entity.District;
import vn.co.cake.entity.Province;
import vn.co.cake.entity.Ward;
import vn.co.cake.enums.AccountStatus;
import vn.co.cake.enums.MailType;
import vn.co.cake.exception.CommonServletException;
//import vn.co.cake.helper.EmailService;
import vn.co.cake.helper.EmailService;
import vn.co.cake.repository.AccountRepository;
import vn.co.cake.repository.DistrictRepository;
import vn.co.cake.repository.ProvinceRepository;
import vn.co.cake.repository.WardRepository;
import vn.co.cake.request.AccountRequest;
import vn.co.cake.request.AccountSearchRequest;
import vn.co.cake.request.ChangePasswordRequest;
import vn.co.cake.request.ForgetPasswordRequest;
import vn.co.cake.request.ResetPasswordRequest;
import vn.co.cake.security.admin.AdminLoginInfo;
import vn.co.cake.security.repository.ContextRepository;
import vn.co.cake.service.AccountService;
//import vn.co.cake.service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static vn.co.cake.common.StringConst.ROLE_USER;

/**
 * AccountServiceImpl
 */
@Slf4j
@Service
@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
public class AccountServiceImpl implements AccountService {
    
    @Value("${cake.base.url}")
    private String baseUrlAM;
    
    @Value("${cake.base.url-sa}")
    private String baseUrlSA;

    private final AccountRepository accountRepository;
    private final BCryptPasswordEncoder encoder;
    private final ContextRepository contextRepository;
    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final WardRepository wardRepository;
    private final EmailService emailService;

    public AccountServiceImpl(AccountRepository accountRepository,
                              EmailService emailService,
                              ContextRepository contextRepository,
                              ProvinceRepository provinceRepository,
                              DistrictRepository districtRepository,
                              WardRepository wardRepository) {
        this.accountRepository = accountRepository;
        this.contextRepository = contextRepository;
        this.provinceRepository = provinceRepository;
        this.districtRepository = districtRepository;
        this.wardRepository = wardRepository;
        this.emailService = emailService;
        this.encoder = new BCryptPasswordEncoder();
    }

    @Override
    public Account getAccount(Long id) throws CommonServletException {
        Account account = accountRepository.findFirstByIdAndDeletedIsFalse(id);
        if (Objects.isNull(account)) {
            throw new CommonServletException("アカウントが存在しません");
        }

        return account;
    }

    /**
     * Find account by email
     *
     * @param email email
     * @return {@link Account}
     */
    @Override
    public Account findAccountForAdmin(String email) {
        return accountRepository.findAccountForAdmin(email);
    }

    @Override
    public Account findAccountForMember(String email) {
        return accountRepository.findAccountForMember(email);
    }

    /**
     * Create account for signup
     * @param accountRequest accountInfo
     */
    @Override
    public void addAccount(AccountRequest accountRequest) throws CommonServletException {
        if (StringUtils.isBlank(accountRequest.getPhone()) || accountRequest.getPhone().length() > 11) {
            throw new CommonServletException("Số điện thoại không hợp lệ!");
        }
        Account account = new Account();
        if (StringUtils.isNotBlank(accountRequest.getAccountId())) {
            account = accountRepository.findFirstByIdAndDeletedIsFalse(Long.parseLong(accountRequest.getAccountId()));
            if (Objects.isNull(account)) {
                throw new CommonServletException("Tài khoản không tồn tại!");
            }
        } else {
            account = accountRepository.findAccountForMember(accountRequest.getPhone());
            if (!accountRequest.getPassword().equals(accountRequest.getConfirmPassword())) {
                throw new CommonServletException(MessageConst.PASSWORD_RE_ENTER_NOT_MATCH);
            }
    
            if (Objects.nonNull(account)) {
                throw new CommonServletException(MessageConst.EMAIL_ALREADY_EXISTS);
            }
            
            account = new Account();
            account.setPassword(encoder.encode(accountRequest.getPassword()));
            account.setAuthorities(ROLE_USER);
            account.setAccountStatus(AccountStatus.VALID.name());
            account.setLogout(false);
        }
        account.setFullName(accountRequest.getFullName());
        account.setMailAddress(accountRequest.getMailAddress());
        account.setPhone(accountRequest.getPhone());
        account.setProvince(accountRequest.getProvince());
        account.setDistrict(accountRequest.getDistrict());
        account.setWard(accountRequest.getWard());
        account.setFloor(accountRequest.getAddress());

        Province province = provinceRepository.findFirstByCode(accountRequest.getProvince());
        District district = districtRepository.findFirstByCode(accountRequest.getDistrict());
        Ward ward = wardRepository.findFirstByCode(accountRequest.getWard());
        
        StringBuilder builder = new StringBuilder();
        if (StringUtils.isNotEmpty(accountRequest.getAddress())) {
            builder.append(accountRequest.getAddress()).append(StringUtils.SPACE);
        }
        if (Objects.nonNull(ward)) {
            builder.append(ward.getPathWithType());
        } else {
            if (Objects.nonNull(district)) {
                builder.append(district.getPathWithType());
            } else if (Objects.nonNull(province)) {
                builder.append(province.getNameWithType());
            }
        }

        account.setAddress(builder.toString());

        accountRepository.save(account);
    }

    @Override
    public void updateAccount(AccountRequest accountRequest) throws CommonServletException {
        Account account = accountRepository.findByIdAndDeletedFalse(Long.parseLong(accountRequest.getAccountId()));
        if (Objects.isNull(account)) {
            throw new CommonServletException("Tài khoản không tồn tại"); //Account does not exist
        }

        Account accountForAdmin = accountRepository.findAccountForAdmin(accountRequest.getMailAddress());
        if (Objects.nonNull(accountForAdmin) && !accountRequest.getMailAddress().equals(account.getMailAddress())) {
            throw new CommonServletException("Email đã được đăng kí"); //registered email address
        }

        account.setFullName(accountRequest.getFullName());
//        account.setMailAddress(accountRequest.getMailAddress());
        account.setPhone(accountRequest.getPhone());
        accountRepository.save(account);
    }

    @Override
    public void deleted(List<Long> ids) throws CommonServletException {
        if(CollectionUtils.isEmpty(ids)) {
            throw new CommonServletException("Tài khoản không tồn tại");
        }
        List<Account> accounts = accountRepository.findAllById(ids);
        for (long id : ids) {
            Account account = getAccount(id);
            account.setDeleted(true);
            accountRepository.save(account);
        }
        accountRepository.saveAll(accounts);
    }

    /**
     * Verify forget password
     * @param passwordForm
     */
    @Override
    public void verifyMailForgetPassword(ForgetPasswordRequest passwordForm) throws CommonServletException {
        String mailAddress = passwordForm.getEmail();

        Account account = accountRepository.findAccountForAdmin(mailAddress);

        if (Objects.isNull(account)) {
            throw new CommonServletException("Tài Khoản Không Tồn Tại");
        }

        Long accountId = account.getId();

        String hash = DigestUtils.sha256Hex(String.format("%s%s", accountId, mailAddress));

        account.setHash(hash);
        accountRepository.save(account);

        GenericMailForm genericMailForm = GenericMailForm.builder()
                .accountName(account.getFullName())
                .hash(hash)
                .key(account.getId())
                .build();

//        mailService.push(Collections.singletonList(mailAddress), null, genericMailForm, MailType.FORGET_PASSWORD);
    }

    /**
     * Check account info
     * @param key
     * @param hash
     * @return ResetPasswordForm
     */
     
     /**
     * Verify forget password
     * @param passwordForm
     */
    @Override
    public void verifyMailForgetPasswordUser(ForgetPasswordRequest passwordForm) throws CommonServletException {
        String mailAddress = passwordForm.getEmail();

        Account account = accountRepository.findAccountForgetPassword(mailAddress);

        if (Objects.isNull(account)) {
            throw new CommonServletException("Tài Khoản Không Tồn Tại");
        }

        Long accountId = account.getId();

        String hash = DigestUtils.sha256Hex(String.format("%s%s", accountId, mailAddress));

        account.setHash(hash);
        accountRepository.save(account);

        GenericMailForm genericMailForm = GenericMailForm.builder()
                .accountName(account.getFullName())
                .hash(hash)
                .key(account.getId())
                .phone(account.getPhone())
                .url(baseUrlAM)
                .build();

        emailService.sendEmail(mailAddress, genericMailForm, MailType.SA_FORGET_PASSWORD);
    }

    /**
     * Check account info
     * @param key
     * @param hash
     * @return ResetPasswordForm
     */
     
    @Override
    public ResetPasswordRequest checkAccountInfo(String key, String hash) throws CommonServletException {
        Long accountId = Long.valueOf(key);

        Account account = accountRepository.findByIdAndHash(accountId, hash);
        if (Objects.isNull(account)) {
            throw new CommonServletException("Tài Khoản Không Tồn Tại");
        }

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setAccountId(accountId);
        request.setHash(hash);
        return request;
    }

    /**
     * Reset password
     * @param passwordForm
     */
    @Override
    public void resetPassword(ResetPasswordRequest passwordForm) throws CommonServletException {
        Account account = accountRepository.findByIdAndHash(passwordForm.getAccountId(), passwordForm.getHash());
        if (Objects.isNull(account)) {
            throw new CommonServletException("Tài Khoản Không Tồn Tại");
        }

        if (encoder.matches(passwordForm.getNewPassword(), account.getPassword())) {
            throw new CommonServletException("Vui lòng nhập mật khẩu khác với mật khẩu hiện tại của bạn.");
        }

        // Validate
        String password = passwordForm.getNewPassword();
        if (StringUtils.isBlank(password)) {
            throw new CommonServletException("Yêu Cầu Nhập Mật Khẩu");
        }

        if (!StringUtils.equalsAny(password, passwordForm.getConfirmPassword())) {
            throw new CommonServletException("Mật Khẩu(Xác Nhận Lại) Không Đúng");
        }

        // Update password
        account.setHash(null);
        account.setPassword(encoder.encode(password));
        accountRepository.save(account);
    }

    @Override
    public void changePasswordAdmin(ChangePasswordRequest request, Model model, RedirectAttributes redirectAttributes) throws CommonServletException {
        Account account = accountRepository.findByIdAndDeletedFalseAndLogoutIsFalse(Long.parseLong(request.getAccountId()));
        if (Objects.isNull(account)) {
            throw new CommonServletException("Tài Khoản Không Tồn Tại");
        }

        if (!StringUtils.equalsAny(request.getNewPassword(), request.getConfirmPassword())) {
            throw new CommonServletException("Mật Khẩu(Xác Nhận Lại) Không Đúng");
        }

        // Update password
        account.setPassword(encoder.encode(request.getNewPassword()));
        accountRepository.save(account);

        GenericMailForm genericMailForm = GenericMailForm.builder()
                .accountName(account.getFullName())
                .password(request.getConfirmPassword())
                .key(account.getId())
                .build();

//        mailService.push(Collections.singletonList(account.getMailAddress()), null, genericMailForm, MailType.CHANGE_PASSWORD);
    }

    @Override
    public void changePasswordForStaff(ChangePasswordRequest request, AdminLoginInfo adminLoginInfo, Model model,
                                       RedirectAttributes redirectAttributes) throws CommonServletException {
        Account account = accountRepository.findAccountForAdmin(adminLoginInfo.getUsername());

        this.validateChangePasswordStaff(account, request);

        // Update password
        account.setPassword(encoder.encode(request.getNewPassword()));
        accountRepository.save(account);
    }

    private void validateChangePasswordStaff(Account account, ChangePasswordRequest request) throws CommonServletException {
        if (Objects.isNull(account)) {
            throw new CommonServletException("Tài Khoản Không Tồn Tại");
        }

        if (!encoder.matches(request.getOldPassword(), account.getPassword())) {
            throw new CommonServletException("Mật Khẩu Cũ Không Đúng");
        }

        if (request.getNewPassword().equals(request.getOldPassword())) {
            throw new CommonServletException("Vui lòng nhập mật khẩu khác với mật khẩu hiện tại của bạn.");
        }

        if (!StringUtils.equalsAny(request.getNewPassword(), request.getConfirmPassword())) {
            throw new CommonServletException("Mật Khẩu(Xác Nhận Lại) Không Đúng");
        }
    }

    @Override
    public Page<Account> getAllAccountByCondition(AccountSearchRequest accountSearchRequest, String email, Pageable pageable) {
        return accountRepository.findAllByDeletedIsFalseAndCondition(accountSearchRequest.getKeyword(), email, pageable);
    }

    @Override
    public String getBookmarkUrlLasted(String key) {
        return (String) contextRepository.getBookmarkUrl(key);
    }

    @Override
    public void deletedBookmarkUrlLasted(String key) {
        contextRepository.deleteKey(key);
    }

    @Override
    public void save(Account account) {
        accountRepository.save(account);
    }

    @Override
    public void updateLoginStatus(Long id, boolean status) {
        Optional<Account> account = accountRepository.findById(id);
        if (account.isPresent()) {
            account.get().setFirstLogin(status);
            accountRepository.save(account.get());
        }
    }
}
