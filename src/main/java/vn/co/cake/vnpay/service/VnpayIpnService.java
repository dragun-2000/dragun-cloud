package vn.co.cake.vnpay.service;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import vn.co.cake.payment.service.PaymentTransactionLogService;
import vn.co.cake.vnpay.VnpayConstants;
import vn.co.cake.vnpay.config.VnpayProperties;
import vn.co.cake.vnpay.dto.VnpayIpnResponse;
import vn.co.cake.vnpay.entity.VnpayPayment;
import vn.co.cake.vnpay.repository.VnpayPaymentRepository;
import vn.co.cake.vnpay.support.VnpayFormats;
import vn.co.cake.vnpay.support.VnpaySigner;

/**
 * Xử lý IPN (server-to-server) — nơi DUY NHẤT cập nhật kết quả thanh toán theo tài liệu VNPAY.
 * Thứ tự: checksum (97) → tìm đơn (01) → số tiền (04) → đã xử lý (02) → cập nhật → 00.
 * VNPAY dừng gọi lại khi nhận 00/02; còn lại retry tối đa 10 lần, mỗi lần cách 5 phút.
 */
@Slf4j
@Service
public class VnpayIpnService {

    public static final String SOURCE_IPN = "IPN";

    /** Cột payment_transaction_log.vietqr_order_id (dùng làm group key) dài tối đa 13. */
    private static final int LOG_REF_MAX_LENGTH = 13;

    private static final Set<String> FINAL_STATUSES = Set.of(
            VnpayConstants.STATUS_PAID, VnpayConstants.STATUS_PAID_ISSUE, VnpayConstants.STATUS_FAILED);

    private final VnpayProperties properties;
    private final VnpayPaymentRepository vnpayPaymentRepository;
    private final VnpayPaymentResultService resultService;
    private final PaymentTransactionLogService paymentTransactionLogService;

    public VnpayIpnService(VnpayProperties properties,
                           VnpayPaymentRepository vnpayPaymentRepository,
                           VnpayPaymentResultService resultService,
                           PaymentTransactionLogService paymentTransactionLogService) {
        this.properties = properties;
        this.vnpayPaymentRepository = vnpayPaymentRepository;
        this.resultService = resultService;
        this.paymentTransactionLogService = paymentTransactionLogService;
    }

    /** Ném exception → controller trả 99 và transaction rollback (VNPAY sẽ gọi lại). */
    @Transactional(rollbackFor = Exception.class)
    public VnpayIpnResponse handle(Map<String, String> params, String rawQuery) {
        String txnRef = params.get("vnp_TxnRef");

        if (!VnpaySigner.verify(params, properties.getHashSecret())) {
            logFailure(txnRef, null, "INVALID_CHECKSUM", rawQuery);
            return VnpayIpnResponse.invalidChecksum();
        }

        VnpayPayment payment = txnRef == null ? null
                : vnpayPaymentRepository.findByTxnRefForUpdate(txnRef).orElse(null);
        if (payment == null || !properties.getTmnCode().equals(params.get("vnp_TmnCode"))) {
            logFailure(txnRef, null, "ORDER_NOT_FOUND", rawQuery);
            return VnpayIpnResponse.orderNotFound();
        }

        if (!VnpayFormats.amountMatches(payment.getAmount(), params.get("vnp_Amount"))) {
            logFailure(txnRef, payment, "INVALID_AMOUNT", rawQuery);
            return VnpayIpnResponse.invalidAmount();
        }

        if (FINAL_STATUSES.contains(payment.getStatus())) {
            log.info("VNPAY IPN duplicate txnRef={} status={}", txnRef, payment.getStatus());
            return VnpayIpnResponse.alreadyConfirmed();
        }

        copyResultFields(payment, params);
        String responseCode = payment.getVnpResponseCode();
        String transactionStatus = payment.getVnpTransactionStatus();
        boolean paid = VnpayConstants.CODE_SUCCESS.equals(responseCode)
                && VnpayConstants.CODE_SUCCESS.equals(transactionStatus);
        boolean suspicious = VnpayConstants.CODE_SUSPICIOUS.equals(responseCode)
                || VnpayConstants.CODE_SUSPICIOUS.equals(transactionStatus);

        if (paid || suspicious) {
            resultService.applyPaid(payment, SOURCE_IPN, rawQuery);
            paymentTransactionLogService.logSuccess(VnpayConstants.METHOD_VNPAY, VnpayConstants.EVENT_IPN,
                    txnRef, payment.getOrderId(), txnRef, payment.getAccountId(), payment.getVnpTransactionNo(),
                    payment.getVnpBankTranNo(), payment.getAmount(), rawQuery, payment.getStatus(), 200, null);
        } else {
            // EXPIRED giữ nguyên (đơn đã huỷ); PENDING → FAILED.
            if (VnpayConstants.STATUS_PENDING.equals(payment.getStatus())) {
                resultService.applyUnpaid(payment, VnpayConstants.STATUS_FAILED, SOURCE_IPN,
                        "VNPAY: " + VnpayResponseMessages.forResponseCode(responseCode));
            }
            logFailure(txnRef, payment, "VNP_" + responseCode, rawQuery);
        }
        vnpayPaymentRepository.save(payment);
        return VnpayIpnResponse.success();
    }

    static void copyResultFields(VnpayPayment payment, Map<String, String> params) {
        payment.setVnpResponseCode(params.get("vnp_ResponseCode"));
        payment.setVnpTransactionStatus(params.get("vnp_TransactionStatus"));
        payment.setVnpTransactionNo(params.get("vnp_TransactionNo"));
        payment.setVnpBankCode(params.get("vnp_BankCode"));
        payment.setVnpBankTranNo(params.get("vnp_BankTranNo"));
        payment.setVnpCardType(params.get("vnp_CardType"));
        payment.setVnpPayDate(params.get("vnp_PayDate"));
    }

    private void logFailure(String txnRef, VnpayPayment payment, String errorCode, String rawQuery) {
        log.warn("VNPAY IPN rejected/failed txnRef={} code={}", txnRef, errorCode);
        paymentTransactionLogService.logFailure(VnpayConstants.METHOD_VNPAY, VnpayConstants.EVENT_IPN,
                StringUtils.left(txnRef, LOG_REF_MAX_LENGTH), payment != null ? payment.getOrderId() : null,
                payment != null ? txnRef : null,
                payment != null ? payment.getAccountId() : null, payment != null ? payment.getAmount() : null,
                errorCode, null, rawQuery, null, 200, null);
    }
}
