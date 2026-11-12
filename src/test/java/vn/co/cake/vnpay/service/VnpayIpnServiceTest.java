package vn.co.cake.vnpay.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vn.co.cake.payment.service.PaymentTransactionLogService;
import vn.co.cake.vnpay.VnpayConstants;
import vn.co.cake.vnpay.config.VnpayProperties;
import vn.co.cake.vnpay.dto.VnpayIpnResponse;
import vn.co.cake.vnpay.entity.VnpayPayment;
import vn.co.cake.vnpay.repository.VnpayPaymentRepository;
import vn.co.cake.vnpay.support.VnpaySigner;

class VnpayIpnServiceTest {

    private static final String SECRET = "SECRETKEY";
    private static final String TMN = "NTHTEST1";
    private static final String TXN_REF = "DB0001";

    private VnpayPaymentRepository repository;
    private VnpayPaymentResultService resultService;
    private VnpayIpnService service;
    private VnpayPayment payment;

    @BeforeEach
    void setUp() {
        VnpayProperties properties = new VnpayProperties();
        properties.setTmnCode(TMN);
        properties.setHashSecret(SECRET);
        repository = mock(VnpayPaymentRepository.class);
        resultService = mock(VnpayPaymentResultService.class);
        service = new VnpayIpnService(properties, repository, resultService,
                mock(PaymentTransactionLogService.class));

        payment = new VnpayPayment();
        payment.setTxnRef(TXN_REF);
        payment.setOrderId(10L);
        payment.setAccountId(20L);
        payment.setAmount(new BigDecimal("150000"));
        payment.setStatus(VnpayConstants.STATUS_PENDING);
        when(repository.findByTxnRefForUpdate(TXN_REF)).thenReturn(Optional.of(payment));
    }

    private static Map<String, String> ipnParams(String responseCode, String transactionStatus, String amount) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TmnCode", TMN);
        params.put("vnp_TxnRef", TXN_REF);
        params.put("vnp_Amount", amount);
        params.put("vnp_ResponseCode", responseCode);
        params.put("vnp_TransactionStatus", transactionStatus);
        params.put("vnp_TransactionNo", "14123456");
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_PayDate", "20261111101000");
        params.put("vnp_SecureHash", VnpaySigner.sign(params, SECRET));
        return params;
    }

    @Test
    void success_appliesPaidAndReturns00() {
        VnpayIpnResponse response = service.handle(ipnParams("00", "00", "15000000"), "q");

        assertEquals("00", response.getRspCode());
        verify(resultService).applyPaid(eq(payment), eq(VnpayIpnService.SOURCE_IPN), anyString());
        assertEquals("14123456", payment.getVnpTransactionNo());
    }

    @Test
    void customerCancelled_marksFailedAndReturns00() {
        VnpayIpnResponse response = service.handle(ipnParams("24", "02", "15000000"), "q");

        assertEquals("00", response.getRspCode());
        verify(resultService).applyUnpaid(eq(payment), eq(VnpayConstants.STATUS_FAILED),
                eq(VnpayIpnService.SOURCE_IPN), anyString());
        verify(resultService, never()).applyPaid(any(), anyString(), anyString());
    }

    @Test
    void suspicious07_isTreatedAsMoneyReceived() {
        service.handle(ipnParams("07", "00", "15000000"), "q");

        verify(resultService).applyPaid(eq(payment), eq(VnpayIpnService.SOURCE_IPN), anyString());
    }

    @Test
    void invalidChecksum_returns97WithoutTouchingOrder() {
        Map<String, String> params = ipnParams("00", "00", "15000000");
        params.put("vnp_SecureHash", "deadbeef");

        assertEquals("97", service.handle(params, "q").getRspCode());
        verify(repository, never()).findByTxnRefForUpdate(anyString());
    }

    @Test
    void unknownOrder_returns01() {
        when(repository.findByTxnRefForUpdate(TXN_REF)).thenReturn(Optional.empty());

        assertEquals("01", service.handle(ipnParams("00", "00", "15000000"), "q").getRspCode());
    }

    @Test
    void otherMerchantTmnCode_returns01() {
        Map<String, String> params = ipnParams("00", "00", "15000000");
        params.put("vnp_TmnCode", "OTHER001");
        params.put("vnp_SecureHash", VnpaySigner.sign(params, SECRET));

        assertEquals("01", service.handle(params, "q").getRspCode());
    }

    @Test
    void amountMismatch_returns04() {
        assertEquals("04", service.handle(ipnParams("00", "00", "100"), "q").getRspCode());
        verify(resultService, never()).applyPaid(any(), anyString(), anyString());
    }

    @Test
    void alreadyProcessed_returns02() {
        payment.setStatus(VnpayConstants.STATUS_PAID);

        assertEquals("02", service.handle(ipnParams("00", "00", "15000000"), "q").getRspCode());
        verify(resultService, never()).applyPaid(any(), anyString(), anyString());
    }

    @Test
    void lateSuccessAfterExpiry_isStillApplied() {
        payment.setStatus(VnpayConstants.STATUS_EXPIRED);

        assertEquals("00", service.handle(ipnParams("00", "00", "15000000"), "q").getRspCode());
        verify(resultService).applyPaid(eq(payment), eq(VnpayIpnService.SOURCE_IPN), anyString());
    }

    @Test
    void lateFailureAfterExpiry_keepsExpiredStatus() {
        payment.setStatus(VnpayConstants.STATUS_EXPIRED);

        assertEquals("00", service.handle(ipnParams("24", "02", "15000000"), "q").getRspCode());
        verify(resultService, never()).applyUnpaid(any(), anyString(), anyString(), anyString());
    }
}
