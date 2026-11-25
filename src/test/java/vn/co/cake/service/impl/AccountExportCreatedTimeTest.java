package vn.co.cake.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import vn.co.cake.entity.Account;
import vn.co.cake.helper.EmailService;
import vn.co.cake.repository.AccountRepository;
import vn.co.cake.repository.DistrictRepository;
import vn.co.cake.repository.ProvinceRepository;
import vn.co.cake.repository.WardRepository;
import vn.co.cake.request.AccountSearchRequest;
import vn.co.cake.security.repository.ContextRepository;

/** Cột "Ngày Tạo" trong file Excel khách hàng phải là giờ Việt Nam, dù JVM chạy ở múi giờ nào. */
class AccountExportCreatedTimeTest {

    @ParameterizedTest
    @ValueSource(strings = {"Asia/Ho_Chi_Minh", "UTC"})
    void createdColumnIsVietnamTime_whateverTheJvmTimezone(String jvmTimezone) throws Exception {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(jvmTimezone));
            Account account = new Account();
            account.setCreated(Date.from(Instant.parse("2026-11-25T03:04:05Z")));

            AccountRepository accountRepository = mock(AccountRepository.class);
            WardRepository wardRepository = mock(WardRepository.class);
            when(accountRepository.findAllByDeletedIsFalseAndConditionForExport(any(), any()))
                    .thenReturn(Collections.singletonList(account));
            when(wardRepository.findAllByCodeIn(anyList())).thenReturn(Collections.emptyList());
            AccountServiceImpl service = new AccountServiceImpl(accountRepository, mock(EmailService.class),
                    mock(ContextRepository.class), mock(ProvinceRepository.class),
                    mock(DistrictRepository.class), wardRepository);

            byte[] xlsx = service.exportAccountsToExcel(new AccountSearchRequest(), "admin@example.com");

            try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
                String created = workbook.getSheetAt(0).getRow(1).getCell(8).getStringCellValue();
                assertEquals("2026-11-25 10:04:05", created, "JVM " + jvmTimezone);
            }
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
