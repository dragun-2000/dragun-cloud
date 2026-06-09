package vn.co.cake.payment.service;

import java.math.BigDecimal;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import vn.co.cake.payment.PaymentConstants;
import vn.co.cake.payment.dto.PaymentOrderLogSummary;
import vn.co.cake.payment.entity.PaymentTransactionLog;
import vn.co.cake.payment.repository.PaymentTransactionLogRepository;
import vn.co.cake.request.PaymentLogSearchRequest;

@Service
public class PaymentTransactionLogService {

    private static final Logger log = LoggerFactory.getLogger(PaymentTransactionLogService.class);

    private final PaymentTransactionLogRepository paymentTransactionLogRepository;

    public PaymentTransactionLogService(PaymentTransactionLogRepository paymentTransactionLogRepository) {
        this.paymentTransactionLogRepository = paymentTransactionLogRepository;
    }

    @Transactional(readOnly = true)
    public Page<PaymentTransactionLog> findPage(PaymentLogSearchRequest searchRequest, Pageable pageable) {
        return paymentTransactionLogRepository.findAllByCondition(searchRequest, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PaymentOrderLogSummary> findOrderSummaryPage(PaymentLogSearchRequest searchRequest, Pageable pageable) {
        return paymentTransactionLogRepository.findOrderSummaryByCondition(searchRequest, pageable);
    }

    @Transactional(readOnly = true)
    public List<PaymentTransactionLog> findLogsByGroupKey(String groupKey) {
        return paymentTransactionLogRepository.findAllByGroupKey(groupKey);
    }

    @Transactional(readOnly = true)
    public PaymentTransactionLog findById(long id) {
        return paymentTransactionLogRepository.findById(id).orElse(null);
    }

    public void logSuccess(String paymentMethod, String eventType, String vietqrOrderId, Long orderId,
                           String orderCode, Long accountId, String externalTxnId, String referenceNumber,
                           BigDecimal amount, String requestPayload, String responsePayload,
                           Integer httpStatus, Long durationMs) {
        save(paymentMethod, eventType, PaymentConstants.LOG_STATUS_SUCCESS, vietqrOrderId, orderId, orderCode,
                accountId, externalTxnId, referenceNumber, amount, null, null, requestPayload, responsePayload,
                httpStatus, durationMs);
    }

    /** Bước audit chưa hoàn tất thanh toán (tạo QR, bắt đầu checkout). */
    public void logInfo(String paymentMethod, String eventType, String vietqrOrderId, Long accountId,
                      BigDecimal amount, String requestPayload, String responsePayload,
                      Integer httpStatus, Long durationMs) {
        save(paymentMethod, eventType, PaymentConstants.LOG_STATUS_INFO, vietqrOrderId, null, null,
                accountId, null, null, amount, null, null, requestPayload, responsePayload,
                httpStatus, durationMs);
    }

    public void logFailure(String paymentMethod, String eventType, String vietqrOrderId, Long orderId,
                           String orderCode, Long accountId, BigDecimal amount, String errorCode, String errorMessage,
                           String requestPayload, String responsePayload, Integer httpStatus, Long durationMs) {
        save(paymentMethod, eventType, PaymentConstants.LOG_STATUS_FAILED, vietqrOrderId, orderId, orderCode,
                accountId, null, null, amount, errorCode, truncate(errorMessage, 500), requestPayload, responsePayload,
                httpStatus, durationMs);
    }

    private void save(String paymentMethod, String eventType, String status, String vietqrOrderId, Long orderId,
                      String orderCode, Long accountId, String externalTxnId, String referenceNumber,
                      BigDecimal amount, String errorCode, String errorMessage, String requestPayload,
                      String responsePayload, Integer httpStatus, Long durationMs) {
        try {
            PaymentTransactionLog row = new PaymentTransactionLog();
            row.setPaymentMethod(paymentMethod);
            row.setEventType(eventType);
            row.setStatus(status);
            row.setVietqrOrderId(vietqrOrderId);
            row.setOrderId(orderId);
            row.setOrderCode(orderCode);
            row.setAccountId(accountId);
            row.setExternalTxnId(externalTxnId);
            row.setReferenceNumber(referenceNumber);
            row.setAmount(amount);
            row.setErrorCode(errorCode);
            row.setErrorMessage(errorMessage);
            row.setRequestPayload(truncatePayload(requestPayload));
            row.setResponsePayload(truncatePayload(responsePayload));
            row.setHttpStatus(httpStatus);
            row.setDurationMs(durationMs);
            paymentTransactionLogRepository.save(row);
        } catch (Exception e) {
            log.warn(String.format(Locale.ROOT, "Failed to save payment_transaction_log event=%s: %s",
                    eventType, e.getMessage()));
        }
    }

    private static String truncatePayload(String payload) {
        if (StringUtils.isEmpty(payload)) {
            return payload;
        }
        if (payload.length() <= PaymentConstants.LOG_PAYLOAD_MAX_LENGTH) {
            return payload;
        }
        return payload.substring(0, PaymentConstants.LOG_PAYLOAD_MAX_LENGTH);
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
