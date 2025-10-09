package vn.co.cake.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.co.cake.entity.District;

import java.util.List;

public interface DistrictRepository extends JpaRepository<District, Long> {
    List<District> findAllByParentCode(String parentCode);
    District findFirstByCode(String code);
}
