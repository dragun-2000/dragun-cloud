package vn.co.cake.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.co.cake.entity.Province;

public interface ProvinceRepository extends JpaRepository<Province, Long> {
    Province findFirstByCode(String code);
}
