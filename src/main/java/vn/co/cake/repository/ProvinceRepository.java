package vn.co.cake.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.co.cake.entity.Province;

import java.util.List;

public interface ProvinceRepository extends JpaRepository<Province, Long> {
    Province findFirstByCode(String code);
    List<Province> findAllByDeletedFalse();
}
