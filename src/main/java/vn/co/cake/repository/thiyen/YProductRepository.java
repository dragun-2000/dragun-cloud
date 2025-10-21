package vn.co.cake.repository.thiyen;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.co.cake.entity.thiyen.YProduct;

public interface YProductRepository extends JpaRepository<YProduct, Long> {
    YProduct findFirstById(Long id);
}