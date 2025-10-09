package vn.co.cake.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.co.cake.entity.external.ShopExtend;

@Repository
public interface ShopExtendRepository extends JpaRepository<ShopExtend, Long> {
}
