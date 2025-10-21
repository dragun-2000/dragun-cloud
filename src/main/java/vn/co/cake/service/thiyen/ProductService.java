package vn.co.cake.service.thiyen;

import org.springframework.stereotype.Service;
import vn.co.cake.controller.thiyen.ProductDTO;
import vn.co.cake.controller.thiyen.ProductMapper;
import vn.co.cake.entity.thiyen.ProductGallery;
import vn.co.cake.entity.thiyen.ProductVariant;
import vn.co.cake.entity.thiyen.YProduct;
import vn.co.cake.repository.thiyen.ProductGalleryRepository;
import vn.co.cake.repository.thiyen.ProductVariantRepository;
import vn.co.cake.repository.thiyen.YProductRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {
    private final YProductRepository productRepository;
    private final ProductGalleryRepository productGalleryRepository;
    private final ProductVariantRepository productVariantRepository;

    public ProductService(YProductRepository repo,
                          ProductGalleryRepository productGalleryRepository,
                          ProductVariantRepository productVariantRepository) {
        this.productRepository = repo;
        this.productGalleryRepository = productGalleryRepository;
        this.productVariantRepository = productVariantRepository;
    }

    public List<ProductDTO> getAll() {
        List<YProduct> products = productRepository.findAll();
        return products.stream().map(product -> {
            List<ProductGallery> galleryList = productGalleryRepository.findAllByProductId(product.getId());
            List<ProductVariant> variantList = productVariantRepository.findAllByProductId(product.getId());
            return ProductMapper.toDTO(product, galleryList, variantList);
        }).collect(Collectors.toList());
    }

    public ProductDTO getById(Long id) {
        YProduct product = productRepository.findFirstById(id);
        List<ProductGallery> galleryList = productGalleryRepository.findAllByProductId(product.getId());
        List<ProductVariant> variantList = productVariantRepository.findAllByProductId(product.getId());
        
        return ProductMapper.toDTO(product, galleryList, variantList);
    }

    public void save(ProductDTO productDTO) {
        YProduct yProduct1 = ProductMapper.toEntity(productDTO);
        YProduct yProduct = productRepository.save(yProduct1);

        if (productDTO.getGallery() != null) {
            List<ProductGallery> galleries = productDTO.getGallery().stream().map(pgDto -> {
                ProductGallery pg = new ProductGallery();
                pg.setImageUrl(pgDto.getImageUrl());
                pg.setPosition(pgDto.getPosition());
                pg.setProduct(yProduct);
                return pg;
            }).collect(Collectors.toList());
            productGalleryRepository.saveAll(galleries);
        }

        if (productDTO.getVariants() != null) {
            List<ProductVariant> variants = productDTO.getVariants().stream().map(pvDto -> {
                ProductVariant pv = new ProductVariant();
                pv.setLabel(pvDto.getLabel());
                pv.setValue(pvDto.getValue());
                pv.setProduct(yProduct);
                return pv;
            }).collect(Collectors.toList());
            productVariantRepository.saveAll(variants);
        }
    }
    
    public void update(Long id, ProductDTO productDTO) {
        YProduct toEntity = ProductMapper.toEntity(productDTO);
        toEntity.setId(id);
        YProduct yProduct = productRepository.save(toEntity);

        if (productDTO.getGallery() != null) {
            List<ProductGallery> galleries = productDTO.getGallery().stream().map(pgDto -> {
                ProductGallery pg = new ProductGallery();
                pg.setImageUrl(pgDto.getImageUrl());
                pg.setPosition(pgDto.getPosition());
                pg.setProduct(yProduct);
                return pg;
            }).collect(Collectors.toList());
            productGalleryRepository.saveAll(galleries);
        }

        if (productDTO.getVariants() != null) {
            List<ProductVariant> variants = productDTO.getVariants().stream().map(pvDto -> {
                ProductVariant pv = new ProductVariant();
                pv.setLabel(pvDto.getLabel());
                pv.setValue(pvDto.getValue());
                pv.setProduct(yProduct);
                return pv;
            }).collect(Collectors.toList());
            productVariantRepository.saveAll(variants);
        }
    }

    public void deleteById(Long id) {
        YProduct yProduct = productRepository.findFirstById(id);
        yProduct.setDeleted(true);
        productRepository.save(yProduct);
    }
}