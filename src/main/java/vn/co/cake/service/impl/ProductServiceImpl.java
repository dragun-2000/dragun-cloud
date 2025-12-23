package vn.co.cake.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.co.cake.controller.external.dto.ProductPancakeRequest;
import vn.co.cake.controller.external.dto.VariationResponse;
import vn.co.cake.dto.ShippingDto;
import vn.co.cake.entity.Product;
import vn.co.cake.entity.Variation;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.repository.ProductRepository;
import vn.co.cake.repository.VariationRepository;
import vn.co.cake.request.ProductCreateRequest;
import vn.co.cake.request.ProductSearchRequest;
import vn.co.cake.service.ProductService;
import vn.co.cake.service.external.PancakePosService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final PancakePosService pancakePosService;
    private final VariationRepository variationRepository;

    public ProductServiceImpl(ProductRepository productRepository,
                              PancakePosService pancakePosService,
                              VariationRepository variationRepository) {
        this.productRepository = productRepository;
        this.pancakePosService = pancakePosService;
        this.variationRepository = variationRepository;
    }

    @Override
    public void create(ProductCreateRequest request) throws CommonServletException {
        try {
            Product productExist = productRepository.findFirstByCodeAndDeletedIsFalse(request.getCode());
            if (productExist != null) {
                throw new CommonServletException("Mã sản phẩm đã được đăng kí!");
            }

            Product product = new Product(request, request.getCategory());
            List<VariationResponse> variationResponses = pancakePosService.createProduct(new ProductPancakeRequest(product));
            String pancakeProductId = "";
            if (!variationResponses.isEmpty()) {
                List<Variation> variations = new ArrayList<>();
                for (VariationResponse variationResponse : variationResponses) {
                    pancakeProductId = variationResponse.getProduct_id();
                    Variation variation = new Variation();
                    variation.setName(variationResponse.getBarcode());
                    variation.setDisplayId(variationResponse.getDisplay_id());
                    variation.setVariationId(variationResponse.getId());
                    variation.setPancakeProductId(variationResponse.getProduct_id());
                    variation.setRetailPrice(variationResponse.getRetail_price());
                    variation.setRemainQuantity(variationResponse.getRemain_quantity());
                    List<VariationResponse.Field> fields = variationResponse.getFields();
                    for (VariationResponse.Field field : fields) {
                        if ("Size".equalsIgnoreCase(field.getName())) {
                            variation.setSize(field.getValue());
                        } else if ("Màu".equalsIgnoreCase(field.getName())) {
                            variation.setColor(field.getValue());
                        } else if ("Kiểu".equalsIgnoreCase(field.getName())) {
                            variation.setType(field.getValue());
                        }
                    }
                    variations.add(variation);
                }
              variationRepository.saveAll(variations);
            }
            product.setProductPancakeId(pancakeProductId);
            productRepository.save(product);
        } catch (Exception e) {
            throw new CommonServletException(String.format("Tạo Sản Phẩm Thất Bại! %s", e.getMessage()));
        }
    }

    @Override
    public void update(ProductCreateRequest request) throws CommonServletException, JsonProcessingException {
        Product product = this.findFirstById(request.getId());
        product.update(request, request.getCategory());
        productRepository.save(product);
    }

    @Override
    public void deleted(Long id) throws CommonServletException {
        Product product = this.findFirstById(id);
        product.setDeleted(true);
        productRepository.save(product);
    }

    @Override
    public void restore(Long id) throws CommonServletException {
        Product product = productRepository.findFirstById(id);
        product.setDeleted(false);
        productRepository.save(product);
    }

    @Override
    public Product detail(Long id) throws CommonServletException {
        return findFirstById(id);
    }

    @Override
    public Page<Product> getAllByCondition(ProductSearchRequest searchRequest, Pageable pageable, boolean isAdmin) {
        return productRepository.getAllByCondition(searchRequest, pageable, isAdmin);
    }

    @Override
    public List<Product> getAllOrderByNameAsc() {
        return productRepository.findAllByDeletedIsFalseOrderByNameAsc();
    }

    private Product findFirstById(Long id) throws CommonServletException {
        Product product = productRepository.findFirstByIdAndDeletedIsFalse(id);
        if (Objects.isNull(product)) {
            throw new CommonServletException(String.format("Mã Sản Phẩm = %s đã xóa khỏi cửa hàng. Hãy restock để có thể chỉnh sửa tiếp!", id));
        }
        return product;
    }
    
    public List<Product> searchProducts(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    @Override
    public List<Product> getLinkedProducts(List<String> productCodes) {
        return productRepository.findAllByCodeInAndDeletedIsFalse(productCodes);
    }

    @Override
    public void syncProductPancake(Set<VariationResponse> variationResponses) {
        log.info("*** Start Sync Product ***");
        Map<String, List<VariationResponse>> variationGroupByProductPancake = variationResponses.stream().collect(Collectors.groupingBy(VariationResponse::getProduct_id));
        List<Product> productDebases = new ArrayList<>();
        AtomicInteger countUpdate = new AtomicInteger();
        AtomicInteger countCreate = new AtomicInteger();
        List<Variation> variationDebases = new ArrayList<>();
        
        List<Product> productOnSystem = productRepository.findAll();
        Map<String, Product> productMap = productOnSystem.stream().collect(Collectors.toMap(Product::getProductPancakeId, Function.identity()));
        variationGroupByProductPancake.forEach((productPancakeId, variationPancakes) -> {
            VariationResponse variationResponse = variationPancakes.get(0);
            Set<String> sizes = new HashSet<>();
            Set<String> colors = new HashSet<>();
            Set<String> types = new HashSet<>();
            long stockQuantity = 0;
            for (VariationResponse variation : variationPancakes) {
                List<VariationResponse.Field> fields = variation.getFields();
                for (VariationResponse.Field field : fields) {
                    if ("Size".equalsIgnoreCase(field.getName())) {
                        sizes.add(field.getValue());
                    } else if ("Màu".equalsIgnoreCase(field.getName())) {
                        colors.add(field.getValue());
                    } else if ("Kiểu".equalsIgnoreCase(field.getName())) {
                        types.add(field.getValue());
                    }
                }
                
                stockQuantity += variation.getRemain_quantity();
            }

            Product productDebaseExist = productMap.get(productPancakeId);
            if (Objects.nonNull(productDebaseExist)) {
                Product.toUpdate(productDebaseExist, variationResponse, sizes, colors, types, stockQuantity);
                countUpdate.getAndIncrement();
            } else {
                productDebaseExist = new Product(variationResponse, sizes, colors, stockQuantity);
                countCreate.getAndIncrement();
            }
            productDebases.add(productDebaseExist);
        });
        productRepository.saveAll(productDebases);
        log.info("** Total Created Product = {} **", countCreate.get());
        log.info("** Total Update Product = {} **", countUpdate.get());
        log.info("*** End Sync Product ***");
        
        log.info("================================");
        
        log.info("*** Start Sync variation ***");

        List<Variation> variationOnSystem = variationRepository.findAll();
        Map<String, Variation> variationMap = variationOnSystem.stream().collect(Collectors.toMap(Variation::getVariationId, Function.identity()));
       
        AtomicInteger countVariationUpdate = new AtomicInteger(0);
        AtomicInteger countVariationCreate = new AtomicInteger(0);
        variationGroupByProductPancake.forEach((productPancakeId, variationPancakes) -> {
            for (VariationResponse variationPancake : variationPancakes) {
                Variation variationDebase = variationMap.get(variationPancake.getId());
                if (variationDebase == null) {
                    variationDebase = new Variation();
                    countVariationCreate.getAndIncrement();
                } else {
                    countVariationUpdate.getAndIncrement();
                }
                variationDebase.setPancakeProductId(productPancakeId);
                variationDebase.setVariationId(variationPancake.getId());
                variationDebase.setDisplayId(variationPancake.getDisplay_id());
                variationDebase.setName(variationPancake.getProduct().getName());
                variationDebase.setRemainQuantity(variationPancake.getRemain_quantity());
                variationDebase.setRetailPrice(variationPancake.getRetail_price());
                
                List<VariationResponse.Field> fields = variationPancake.getFields();
                for (VariationResponse.Field field : fields) {
                    if ("Size".equalsIgnoreCase(field.getName())) {
                        variationDebase.setSize(field.getValue());
                    } else if ("Màu".equalsIgnoreCase(field.getName())) {
                        variationDebase.setColor(field.getValue());
                    } else if ("Kiểu".equalsIgnoreCase(field.getName())) {
                        variationDebase.setType(field.getValue());
                    }
                }

                List<VariationResponse.VariationWarehouse> variationsWarehouses = variationPancake.getVariations_warehouses();
                if (!variationsWarehouses.isEmpty()) {
                    VariationResponse.VariationWarehouse warehouse = variationsWarehouses.get(0);
                    variationDebase.setTotalQuantity(warehouse.getTotal_quantity());
                    variationDebase.setActualRemainQuantity(warehouse.getActual_remain_quantity());
                    variationDebase.setWaitingQuantity(warehouse.getWaiting_quantity());
                    variationDebase.setReturningQuantity(warehouse.getReturning_quantity());
                }
                
                variationDebases.add(variationDebase);
            }
        });
        
        log.info("** Total Created Variation = {} **", countVariationCreate.get());
        log.info("** Total Update Variation = {} **", countVariationUpdate.get());
        
        variationRepository.saveAll(variationDebases);
        
        log.info("*** End Sync variation ***");
    }

    @Override
    public void updateProductDiscountPrice(ShippingDto discountDto) {
        int discountPercent = discountDto.getDiscountPercent();
        List<Product> products = productRepository.findAll();
        products.forEach(product -> {
            BigDecimal discountPrice = product.getPrice().multiply(BigDecimal.valueOf(discountPercent)).divide(BigDecimal.valueOf(100));
            product.setDiscount(BigDecimal.valueOf(discountDto.getDiscountPercent()));
            product.setDiscountPrice(discountPrice);
            product.setFinalPrice(product.getPrice().subtract(discountPrice));
            
            log.info("*** Standard Price = {} | Discount Price = {} | Final Price = {}", product.getPrice(), discountPrice, product.getPrice().subtract(discountPrice));
        });
        
        productRepository.saveAll(products);
    }
    
    @Override
    public void resetProductDiscountPrice() {
        List<Product> products = productRepository.findAll();
        products.forEach(product -> {
            BigDecimal oldDiscount = product.getDiscount();
            if (oldDiscount.intValue() <= 0) return;
            product.setDiscount(BigDecimal.ZERO);
            product.setDiscountPrice(BigDecimal.ZERO);
            product.setFinalPrice(product.getPrice());
            
            log.info("*** Reset discount success for product = {} with old discount = {}", product.getName(), oldDiscount.intValue());
        });
        
        productRepository.saveAll(products);
    }

    @Override
    public void updateProductsDiscount(List<Long> productIds, BigDecimal discount) throws CommonServletException {
        if (productIds == null || productIds.isEmpty()) {
            throw new CommonServletException("Vui lòng chọn ít nhất một sản phẩm!");
        }
        
        if (discount == null || discount.compareTo(BigDecimal.ZERO) <= 0 || discount.compareTo(new BigDecimal("100")) >= 0) {
            throw new CommonServletException("Giảm giá phải lớn hơn 0 và nhỏ hơn 100!");
        }
        
        List<Product> products = productRepository.findAllByIdIn(productIds);
        if (products.isEmpty()) {
            throw new CommonServletException("Không tìm thấy sản phẩm nào!");
        }
        
        products.forEach(product -> {
            if (product.getPrice() != null && product.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                // Logic giống Product.java lines 167-174
                BigDecimal discountAmount = BigDecimal.ZERO;
                BigDecimal finalPrice = product.getPrice();
                
                if (discount.compareTo(BigDecimal.ZERO) > 0) {
                    discountAmount = product.getPrice()
                        .multiply(discount)
                        .divide(new BigDecimal("100"), RoundingMode.HALF_UP);
                    finalPrice = product.getPrice().subtract(discountAmount);
                }
                
                product.setDiscount(discount);
                product.setDiscountPrice(discountAmount);
                product.setFinalPrice(finalPrice);
            }
        });
        
        productRepository.saveAll(products);
    }
}
