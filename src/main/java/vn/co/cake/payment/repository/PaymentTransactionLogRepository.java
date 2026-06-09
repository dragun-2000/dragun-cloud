package vn.co.cake.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.co.cake.payment.entity.PaymentTransactionLog;
import vn.co.cake.payment.repository.custom.CustomPaymentTransactionLogRepository;

public interface PaymentTransactionLogRepository extends JpaRepository<PaymentTransactionLog, Long>,
        CustomPaymentTransactionLogRepository {

    boolean existsByExternalTxnIdAndEventTypeAndStatus(String externalTxnId, String eventType, String status);
}
