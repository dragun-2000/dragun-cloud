package vn.co.cake.payment.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.co.cake.payment.entity.CheckoutPending;

public interface CheckoutPendingRepository extends JpaRepository<CheckoutPending, Long> {

    Optional<CheckoutPending> findFirstByVietqrOrderId(String vietqrOrderId);

    Optional<CheckoutPending> findFirstByVietqrOrderIdAndStatus(String vietqrOrderId, String status);

    @Query("select p from CheckoutPending p "
            + "where p.status = 'PENDING' and p.expiresAt is not null and p.expiresAt <= :now")
    List<CheckoutPending> findExpiredPending(@Param("now") Date now);
}
