package vn.co.cake.repository;

import vn.co.cake.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, CustomProductRepository {
    Product findFirstByIdAndDeletedIsFalse(Long id);
    Product findFirstById(Long id);
    Product findFirstByProductPancakeId(String pancakeProductId);
    List<Product> findAllByDeletedIsFalseOrderByNameAsc();
    List<Product> findByNameContainingIgnoreCase(String name);
    List<Product> findAllByCodeInAndDeletedIsFalse(List<String> productCodes);
    Product findFirstByCodeAndDeletedIsFalse(String code);
    List<Product> findAllByIdIn(List<Long> productIds);
    Product findFirstByProductPancakeIdAndDeletedIsFalse(String pancakeProductId);
}
