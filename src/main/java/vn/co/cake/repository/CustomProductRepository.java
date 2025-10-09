package vn.co.cake.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.co.cake.entity.Product;
import vn.co.cake.request.ProductSearchRequest;

public interface CustomProductRepository {
    Page<Product> getAllByCondition(ProductSearchRequest searchRequest, Pageable pageable, boolean isAdmin);
}
