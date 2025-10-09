package vn.co.cake.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.co.cake.entity.external.ProductExtend;

@Repository
public interface ProductExtendRepository extends JpaRepository<ProductExtend, Long> {
}
