package vn.co.cake.payment.repository.custom;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

import vn.co.cake.payment.dto.PaymentOrderLogSummary;
import vn.co.cake.payment.entity.PaymentTransactionLog;
import vn.co.cake.request.PaymentLogSearchRequest;

public interface CustomPaymentTransactionLogRepository {

    Page<PaymentTransactionLog> findAllByCondition(PaymentLogSearchRequest searchRequest, Pageable pageable);

    Page<PaymentOrderLogSummary> findOrderSummaryByCondition(PaymentLogSearchRequest searchRequest, Pageable pageable);

    List<PaymentTransactionLog> findAllByGroupKey(String groupKey);
}
