package vn.co.cake.vnpay.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.co.cake.vnpay.entity.VnpayPayment;

public interface VnpayPaymentRepository extends JpaRepository<VnpayPayment, Long> {

    Optional<VnpayPayment> findFirstByTxnRef(String txnRef);

    /** Khoá dòng khi xử lý IPN / hết hạn — tránh IPN retry song song xử lý 2 lần. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from VnpayPayment p where p.txnRef = :txnRef")
    Optional<VnpayPayment> findByTxnRefForUpdate(@Param("txnRef") String txnRef);

    @Query("select p.txnRef from VnpayPayment p where p.status = 'PENDING' and p.expiresAt <= :deadline")
    List<String> findPendingTxnRefsExpiredBefore(@Param("deadline") Date deadline);
}
