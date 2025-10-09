package vn.co.cake.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.co.cake.entity.PancakeProperties;

public interface PancakePropertyRepository extends JpaRepository<PancakeProperties, Long> {
    PancakeProperties findFirstByDeletedIsFalse();
}
