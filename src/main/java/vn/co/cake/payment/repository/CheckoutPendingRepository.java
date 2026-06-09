package vn.co.cake.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.co.cake.payment.entity.CheckoutPending;

public interface CheckoutPendingRepository extends JpaRepository<CheckoutPending, Long> {

    Optional<CheckoutPending> findFirstByVietqrOrderId(String vietqrOrderId);

    Optional<CheckoutPending> findFirstByVietqrOrderIdAndStatus(String vietqrOrderId, String status);
}
