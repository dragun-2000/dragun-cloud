package vn.co.cake.repository.thiyen;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.co.cake.entity.thiyen.ProductGallery;

import java.util.List;

public interface ProductGalleryRepository extends JpaRepository<ProductGallery, Long> {
    List<ProductGallery> findAllByProductId(Long productId);
}
