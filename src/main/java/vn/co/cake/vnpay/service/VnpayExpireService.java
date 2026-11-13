package vn.co.cake.vnpay.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import vn.co.cake.payment.service.PaymentTransactionLogService;
import vn.co.cake.vnpay.VnpayConstants;
import vn.co.cake.vnpay.client.VnpayApiClient;
import vn.co.cake.vnpay.config.VnpayProperties;
import vn.co.cake.vnpay.dto.VnpayQueryDrResult;
import vn.co.cake.vnpay.entity.VnpayPayment;
import vn.co.cake.vnpay.repository.VnpayPaymentRepository;
import vn.co.cake.vnpay.support.VnpayFormats;

/**
 * Phiên VNPAY quá hạn mà chưa nhận IPN: hỏi lại VNPAY (QueryDR) trước khi huỷ đơn,
 * tránh huỷ nhầm đơn khách đã trả tiền nhưng IPN đến trễ / không đến.
 */
@Slf4j
@Service
public class VnpayExpireService {

    public static final String SOURCE_QUERYDR = "QUERYDR";
    public static final String SOURCE_TIMEOUT = "TIMEOUT";

    private final VnpayProperties properties;
    private final VnpayPaymentRepository vnpayPaymentRepository;
    private final VnpayApiClient vnpayApiClient;
    private final VnpayPaymentResultService resultService;
    private final PaymentTransactionLogService paymentTransactionLogService;

    public VnpayExpireService(VnpayProperties properties,
                              VnpayPaymentRepository vnpayPaymentRepository,
                              VnpayApiClient vnpayApiClient,
                              VnpayPaymentResultService resultService,
                              PaymentTransactionLogService paymentTransactionLogService) {
        this.properties = properties;
        this.vnpayPaymentRepository = vnpayPaymentRepository;
        this.vnpayApiClient = vnpayApiClient;
        this.resultService = resultService;
        this.paymentTransactionLogService = paymentTransactionLogService;
    }

    /** Mỗi giao dịch một transaction riêng — lỗi một dòng không chặn các dòng khác. */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void reconcile(String txnRef) {
        VnpayPayment payment = vnpayPaymentRepository.findByTxnRefForUpdate(txnRef).orElse(null);
        if (payment == null || !VnpayConstants.STATUS_PENDING.equals(payment.getStatus())) {
            return;
        }
        long giveUpAtMs = payment.getExpiresAt().getTime()
                + TimeUnit.MINUTES.toMillis(properties.getQueryDrGiveUpMinutes());
        // >= để query-dr-give-up-minutes=0 huỷ ngay từ lần job đầu sau expiresAt.
        boolean giveUp = System.currentTimeMillis() >= giveUpAtMs;

        VnpayQueryDrResult result = null;
        try {
            result = vnpayApiClient.queryDr(payment, serverIp());
        } catch (Exception ex) {
            log.warn("VNPAY querydr failed txnRef={}: {}", txnRef, ex.getMessage());
        }

        if (result != null && result.isSignatureValid()) {
            boolean suspicious = VnpayConstants.CODE_SUSPICIOUS.equals(result.getTransactionStatus());
            if (result.isPaid() || suspicious) {
                if (!VnpayFormats.amountMatches(payment.getAmount(), result.getAmount())) {
                    log.error("VNPAY querydr amount mismatch txnRef={} expected={} got={}",
                            txnRef, payment.getAmount(), result.getAmount());
                    logQuery(payment, false, "INVALID_AMOUNT", result);
                    return;
                }
                copyResultFields(payment, result);
                resultService.applyPaid(payment, SOURCE_QUERYDR, String.valueOf(result.getRaw()));
                logQuery(payment, true, null, result);
                vnpayPaymentRepository.save(payment);
                return;
            }
            if (result.isDefinitelyUnpaid()) {
                copyResultFields(payment, result);
                resultService.applyUnpaid(payment, VnpayConstants.STATUS_EXPIRED, SOURCE_QUERYDR,
                        "Phiên thanh toán VNPAY đã hết hạn");
                logQuery(payment, false, "EXPIRED_VNP_" + result.getResponseCode(), result);
                vnpayPaymentRepository.save(payment);
                return;
            }
        } else if (result != null) {
            log.warn("VNPAY querydr signature invalid txnRef={} — bỏ qua kết quả", txnRef);
        }

        if (giveUp) {
            // Không xác định được kết quả sau thời gian chờ: huỷ đơn, IPN đến muộn vẫn được xử lý (PAID_ISSUE).
            resultService.applyUnpaid(payment, VnpayConstants.STATUS_EXPIRED, SOURCE_TIMEOUT,
                    "Phiên thanh toán VNPAY đã hết hạn (không xác nhận được với VNPAY)");
            logQuery(payment, false, "EXPIRED_UNVERIFIED", result);
            vnpayPaymentRepository.save(payment);
            return;
        }
        log.info("VNPAY reconcile giữ PENDING txnRef={} — chờ QueryDR/IPN đến {}",
                txnRef, new Date(giveUpAtMs));
    }

    private static void copyResultFields(VnpayPayment payment, VnpayQueryDrResult result) {
        payment.setVnpResponseCode(result.getResponseCode());
        payment.setVnpTransactionStatus(result.getTransactionStatus());
        payment.setVnpTransactionNo(result.getTransactionNo());
        payment.setVnpBankCode(result.getBankCode());
        payment.setVnpPayDate(result.getPayDate());
    }

    private void logQuery(VnpayPayment payment, boolean success, String errorCode, VnpayQueryDrResult result) {
        String response = result != null ? String.valueOf(result.getRaw()) : null;
        if (success) {
            paymentTransactionLogService.logSuccess(VnpayConstants.METHOD_VNPAY, VnpayConstants.EVENT_QUERYDR,
                    payment.getTxnRef(), payment.getOrderId(), payment.getTxnRef(), payment.getAccountId(),
                    payment.getVnpTransactionNo(), null, payment.getAmount(), null, response, 200, null);
        } else {
            paymentTransactionLogService.logFailure(VnpayConstants.METHOD_VNPAY, VnpayConstants.EVENT_EXPIRED,
                    payment.getTxnRef(), payment.getOrderId(), payment.getTxnRef(), payment.getAccountId(),
                    payment.getAmount(), errorCode, null, null, response, null, null);
        }
    }

    private static String serverIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            return "127.0.0.1";
        }
    }
}
