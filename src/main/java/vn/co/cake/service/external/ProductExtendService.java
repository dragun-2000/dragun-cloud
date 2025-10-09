package vn.co.cake.service.external;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import vn.co.cake.controller.external.dto.ProductExtendDTO;
import vn.co.cake.entity.external.ProductExtend;
import vn.co.cake.repository.ProductExtendRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductExtendService {

    private final ProductExtendRepository productExtendRepository;

    public ProductExtendService(ProductExtendRepository productExtendRepository) {
        this.productExtendRepository = productExtendRepository;
    }

    public List<ProductExtendDTO> getAllProducts() {
        return productExtendRepository.findAll()
            .stream()
            .map(ProductExtendService::toDTO)
            .collect(Collectors.toList());
    }

    public Optional<ProductExtendDTO> getProductById(Long id) {
        return productExtendRepository.findById(id)
            .map(ProductExtendService::toDTO);
    }

    public ProductExtendDTO createProduct(ProductExtendDTO productExtendDTO) {
        ProductExtend productExtend = toEntity(productExtendDTO);
        ProductExtend savedProduct = productExtendRepository.save(productExtend);
        return toDTO(savedProduct);
    }

    public ProductExtendDTO updateProduct(Long id, ProductExtendDTO productExtendDTO) {
        Optional<ProductExtend> optionalProductExtend = productExtendRepository.findById(id);
        ProductExtend productExtend = optionalProductExtend.map(extend -> toUpdate(extend, productExtendDTO)).orElseGet(() -> toEntity(productExtendDTO));
        ProductExtend updatedProduct = productExtendRepository.save(productExtend);
        return toDTO(updatedProduct);
    }

    public boolean deleteProduct(Long id) {
        if (productExtendRepository.existsById(id)) {
            productExtendRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    public static ProductExtend toEntity(ProductExtendDTO productExtendDTO) {
        ProductExtend productExtend = new ProductExtend();
        productExtend.setId(productExtendDTO.getId());
        productExtend.setShopId(productExtendDTO.getShopId());
        productExtend.setName(productExtendDTO.getName());
        productExtend.setPrice(productExtendDTO.getPrice());
        if (StringUtils.isNotEmpty(productExtendDTO.getImageUrl())) {
            productExtend.setImageUrl(productExtendDTO.getImageUrl());
        }
        productExtend.setDescription(productExtendDTO.getDescription());
        productExtend.setCategory(productExtendDTO.getCategory());
        return productExtend;
    }

    public static ProductExtend toUpdate(ProductExtend productExtend, ProductExtendDTO productExtendDTO) {
        productExtend.setId(productExtendDTO.getId());
        productExtend.setShopId(productExtendDTO.getShopId());
        productExtend.setName(productExtendDTO.getName());
        productExtend.setPrice(productExtendDTO.getPrice());
        if (StringUtils.isNotEmpty(productExtendDTO.getImageUrl())) {
            productExtend.setImageUrl(productExtendDTO.getImageUrl());
        }
        productExtend.setDescription(productExtendDTO.getDescription());
        productExtend.setCategory(productExtendDTO.getCategory());
        return productExtend;
    }
    
    public static ProductExtendDTO toDTO(ProductExtend productExtend) {
        return new ProductExtendDTO(
                productExtend.getId(),
                productExtend.getShopId(),
                productExtend.getName(),
                productExtend.getPrice(),
                productExtend.getImageUrl(),
                productExtend.getDescription(),
                productExtend.getCategory()
        );
    }
}
