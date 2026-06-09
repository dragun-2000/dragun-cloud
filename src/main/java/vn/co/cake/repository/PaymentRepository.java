package vn.co.cake.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.co.cake.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
