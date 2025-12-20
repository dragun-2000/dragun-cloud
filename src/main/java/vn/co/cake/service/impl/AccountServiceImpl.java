package vn.co.cake.service.impl;

import vn.co.cake.common.MessageConst;
import vn.co.cake.dto.GenericMailForm;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import vn.co.cake.entity.Account;
import vn.co.cake.entity.District;
import vn.co.cake.entity.Province;
import vn.co.cake.entity.Ward;
import vn.co.cake.utils.DateUtil;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
    public List<Account> getAllAccountsForExport(AccountSearchRequest accountSearchRequest, String adminEmail) {
        return accountRepository.findAllByDeletedIsFalseAndConditionForExport(accountSearchRequest.getKeyword(), adminEmail);
    }

    @Override
    public byte[] exportAccountsToExcel(AccountSearchRequest accountSearchRequest, String adminEmail) throws IOException {
        // Get all accounts for export (no pagination)
        List<Account> accounts = getAllAccountsForExport(accountSearchRequest, adminEmail);
        
        // Get ward information for addresses
        List<String> wardIds = accounts.stream()
                .map(Account::getWard)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<String, Ward> wardMap = wardRepository.findAllByCodeIn(wardIds).stream()
                .collect(Collectors.toMap(Ward::getCode, x -> x));

        // Create Excel workbook
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Danh Sách Khách Hàng");

        // Create header style
        CellStyle headerStyle = createHeaderStyle(workbook);
        
        // Create data style
        CellStyle dataStyle = createDataStyle(workbook);

        // Create header row
        String[] headers = {
            "STT", "Tên Khách Hàng", "Email", "Số Điện Thoại", 
            "Địa Chỉ", "Tỉnh/Thành", "Quận/Huyện", "Phường/Xã", "Ngày Tạo"
        };
        createHeaderRow(sheet, headers, headerStyle);

        // Create data rows
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        int rowNum = 1;
        for (Account account : accounts) {
            createDataRow(sheet, rowNum++, account, wardMap, dateFormat, dataStyle);
        }

        // Auto-size columns
        autoSizeColumns(sheet, headers.length);

        // Write workbook to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        log.info("Exported {} customers to Excel", accounts.size());
        return outputStream.toByteArray();
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        return headerStyle;
    }

    private CellStyle createDataStyle(XSSFWorkbook workbook) {
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        return dataStyle;
    }

    private void createHeaderRow(Sheet sheet, String[] headers, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void createDataRow(Sheet sheet, int rowNum, Account account, Map<String, Ward> wardMap, 
                               SimpleDateFormat dateFormat, CellStyle dataStyle) {
        Row row = sheet.createRow(rowNum);
        
        // STT
        Cell cell0 = row.createCell(0);
        cell0.setCellValue(rowNum);
        cell0.setCellStyle(dataStyle);
        
        // Tên Khách Hàng
        Cell cell1 = row.createCell(1);
        cell1.setCellValue(account.getFullName() != null ? account.getFullName() : "");
        cell1.setCellStyle(dataStyle);
        
        // Email
        Cell cell2 = row.createCell(2);
        cell2.setCellValue(account.getMailAddress() != null ? account.getMailAddress() : "");
        cell2.setCellStyle(dataStyle);
        
        // Số Điện Thoại
        Cell cell3 = row.createCell(3);
        cell3.setCellValue(account.getPhone() != null ? account.getPhone() : "");
        cell3.setCellStyle(dataStyle);
        
        // Địa Chỉ (đã bao gồm ward path nếu có)
        String address = account.getAddress() != null ? account.getAddress() : "";
        Ward ward = wardMap.get(account.getWard());
        if (ward != null) {
            address = address + (StringUtils.isNotBlank(address) ? " " : "") + ward.getPathWithType();
        }
        Cell cell4 = row.createCell(4);
        cell4.setCellValue(address);
        cell4.setCellStyle(dataStyle);
        
        // Tỉnh/Thành
        Cell cell5 = row.createCell(5);
        cell5.setCellValue(account.getProvince() != null ? account.getProvince() : "");
        cell5.setCellStyle(dataStyle);
        
        // Quận/Huyện
        Cell cell6 = row.createCell(6);
        cell6.setCellValue(account.getDistrict() != null ? account.getDistrict() : "");
        cell6.setCellStyle(dataStyle);
        
        // Phường/Xã
        Cell cell7 = row.createCell(7);
        String wardName = ward != null ? ward.getName() : (account.getWard() != null ? account.getWard() : "");
        cell7.setCellValue(wardName);
        cell7.setCellStyle(dataStyle);
        
        // Ngày Tạo
        Cell cell8 = row.createCell(8);
        if (account.getCreated() != null) {
            cell8.setCellValue(dateFormat.format(DateUtil.plusHours(account.getCreated(), 7)));
        } else {
            cell8.setCellValue("");
        }
        cell8.setCellStyle(dataStyle);
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            // Add some padding
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
        }
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
