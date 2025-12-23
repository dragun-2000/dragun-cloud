package vn.co.cake.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.co.cake.controller.external.dto.VariationResponse;
import vn.co.cake.dto.ShippingDto;
import vn.co.cake.entity.Product;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.request.ProductCreateRequest;
import vn.co.cake.request.ProductSearchRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface ProductService {
    void create(ProductCreateRequest request) throws CommonServletException;
    void update(ProductCreateRequest request) throws CommonServletException, JsonProcessingException;
    void deleted(Long id) throws CommonServletException;
    void restore(Long id) throws CommonServletException;
    Product detail(Long id) throws CommonServletException;
    Page<Product> getAllByCondition(ProductSearchRequest searchRequest, Pageable pageable, boolean isAdmin);
    List<Product> getAllOrderByNameAsc();
    List<Product> searchProducts(String name);
    List<Product> getLinkedProducts(List<String> productCodes);
    void syncProductPancake(Set<VariationResponse> variationResponses);
    void updateProductDiscountPrice(ShippingDto discountDto);
    void resetProductDiscountPrice();
    void updateProductsDiscount(List<Long> productIds, BigDecimal discount) throws CommonServletException;
}
