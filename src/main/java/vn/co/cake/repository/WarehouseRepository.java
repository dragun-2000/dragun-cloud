package vn.co.cake.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.co.cake.entity.Warehouse;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    Warehouse findFirstByDeletedIsFalse();
}
