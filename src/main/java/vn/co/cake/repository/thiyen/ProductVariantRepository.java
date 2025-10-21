package vn.co.cake.repository.thiyen;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.co.cake.entity.thiyen.ProductVariant;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findAllByProductId(Long productId);
}
