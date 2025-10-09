package vn.co.cake.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.co.cake.entity.Voucher;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Voucher findFirstByCodeAndDeletedIsFalse(String code);
}
