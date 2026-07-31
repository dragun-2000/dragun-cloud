package vn.co.cake.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.co.cake.entity.Ward;

import java.util.List;

public interface WardRepository extends JpaRepository<Ward, Long> {
    List<Ward> findAllByParentCodeOrderByNameAsc(String parentCode);

    List<Ward> findAllByCodeIn(List<String> codes);

    Ward findFirstByCode(String code);
}
