package vn.co.cake.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import vn.co.cake.controller.external.dto.VariationResponse;
import vn.co.cake.request.ProductCreateRequest;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

@Data
@Entity
@Table(name = "products")
@NoArgsConstructor
public class Product extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "product_pancake_id")
    private String productPancakeId;

    @Column(name = "variation_id")
    private String variationId;

    @Column(name = "display_id")
    private String displayId;

    private String code;
    private String subCode;
    private String name;
    private String description;
    private String descriptionSize;

    @Column(name = "product_information", columnDefinition = "TEXT")
    private String productInformation;

    private String image;
    private String image1;
    private String image2;
    private String image3;
    private String image4;
    private String image5;
    private String image6;
    private String image7;
    private String image8;
    private String image9;
    private String colors;
    private String sizes;
    private String types;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private BigDecimal finalPrice;

    @Column(
            name = "discount",
            columnDefinition = "numeric(5,2) default 0"
    )
    private BigDecimal discount;
    private Long stockQuantity;
    private String relatedProduct1;
    private String relatedProduct2;
    private String relatedProduct3;
    private String relatedProduct4;

    @JoinColumn(name = "categorys")
    private String categories;
    
    private boolean deleted;

    public Product(ProductCreateRequest product, String categories) {
        this.name = product.getName();
        this.code = product.getCode();
        this.subCode = product.getSubCode();
        this.description = product.getDescription();
        this.descriptionSize = product.getDescriptionSize();
        this.productInformation = product.getProductInformation();
        if (StringUtils.isNotEmpty(product.getImageUrl())) {
            this.image = product.getImageUrl();
        }
        if (StringUtils.isNotEmpty(product.getImage1Url())) {
            this.image1 = product.getImage1Url();
        }
        if (StringUtils.isNotEmpty(product.getImage2Url())) {
            this.image2 = product.getImage2Url();
        }
        if (StringUtils.isNotEmpty(product.getImage3Url())) {
            this.image3 = product.getImage3Url();
        }
        if (StringUtils.isNotEmpty(product.getImage4Url())) {
            this.image4 = product.getImage4Url();
        }
        if (StringUtils.isNotEmpty(product.getImage5Url())) {
            this.image5 = product.getImage5Url();
        }
        if (StringUtils.isNotEmpty(product.getImage6Url())) {
            this.image6 = product.getImage6Url();
        }
        if (StringUtils.isNotEmpty(product.getImage7Url())) {
            this.image7 = product.getImage7Url();
        }
        if (StringUtils.isNotEmpty(product.getImage8Url())) {
            this.image8 = product.getImage8Url();
        }
        if (StringUtils.isNotEmpty(product.getImage9Url())) {
            this.image9 = product.getImage9Url();
        }
        this.colors = product.getColor();
        this.sizes = product.getSizes();
        this.price = product.getPrice();
        this.discount = product.getDiscount() == null ? BigDecimal.ZERO : product.getDiscount();
        
        BigDecimal discountPrice = product.getPrice();
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (product.getDiscount().longValue() > 0) {
            discountAmount = product.getPrice().multiply(product.getDiscount()).divide(new BigDecimal("100"), RoundingMode.HALF_UP);
            discountPrice = product.getPrice().subtract(discountAmount);
        }
        this.discountPrice = discountAmount;
        this.finalPrice = discountPrice;
        
        this.categories = categories;
        this.stockQuantity = product.getStockQuantity();
        this.relatedProduct1 = product.getRelatedProduct1();
        this.relatedProduct2 = product.getRelatedProduct2();
        this.relatedProduct3 = product.getRelatedProduct3();
        this.relatedProduct4 = product.getRelatedProduct4();
    }
    
    public void update(ProductCreateRequest product, String categories) {
        this.name = product.getName();
        this.code = product.getCode();
        this.subCode = product.getSubCode();
        this.description = product.getDescription();
        this.descriptionSize = product.getDescriptionSize();
        this.productInformation = product.getProductInformation();
        if (StringUtils.isNotEmpty(product.getImageUrl())) {
            this.image = product.getImageUrl();
        }
        if (StringUtils.isNotEmpty(product.getImage1Url())) {
            this.image1 = product.getImage1Url();
        }
        if (StringUtils.isNotEmpty(product.getImage2Url())) {
            this.image2 = product.getImage2Url();
        }
        if (StringUtils.isNotEmpty(product.getImage3Url())) {
            this.image3 = product.getImage3Url();
        }
        if (StringUtils.isNotEmpty(product.getImage4Url())) {
            this.image4 = product.getImage4Url();
        }
        if (StringUtils.isNotEmpty(product.getImage5Url())) {
            this.image5 = product.getImage5Url();
        }
        if (StringUtils.isNotEmpty(product.getImage6Url())) {
            this.image6 = product.getImage6Url();
        }
        if (StringUtils.isNotEmpty(product.getImage7Url())) {
            this.image7 = product.getImage7Url();
        }
        if (StringUtils.isNotEmpty(product.getImage8Url())) {
            this.image8 = product.getImage8Url();
        }
        if (StringUtils.isNotEmpty(product.getImage9Url())) {
            this.image9 = product.getImage9Url();
        }
        this.colors = product.getColor();
        this.sizes = product.getSizes();
        this.price = product.getPrice();
        this.discount = product.getDiscount() == null ? BigDecimal.ZERO : product.getDiscount();
        
        BigDecimal discountPrice = product.getPrice();
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (product.getDiscount().longValue() > 0) {
            discountAmount = product.getPrice().multiply(product.getDiscount()).divide(new BigDecimal("100"), RoundingMode.HALF_UP);
            discountPrice = product.getPrice().subtract(discountAmount);
        }
        this.discountPrice = discountAmount;
        this.finalPrice = discountPrice;
        
        this.stockQuantity = product.getStockQuantity();
        this.categories = categories;
        this.relatedProduct1 = product.getRelatedProduct1();
        this.relatedProduct2 = product.getRelatedProduct2();
        this.relatedProduct3 = product.getRelatedProduct3();
        this.relatedProduct4 = product.getRelatedProduct4();
    }
    
    public Product(VariationResponse variationResponse, Set<String> sizes, Set<String> colors, long stockQuantity) {
        VariationResponse.Product variationProduct = variationResponse.getProduct();
        this.name = variationProduct.getName();
        this.code = variationProduct.getDisplay_id();
        this.subCode = variationResponse.getDisplay_id();
        this.description = variationProduct.getNote_product();
        this.productPancakeId = variationResponse.getProduct_id();
        this.stockQuantity = stockQuantity;
        this.price = BigDecimal.valueOf(variationResponse.getRetail_price());
        this.discount = BigDecimal.ZERO;
        this.categories = "BEST SELLER";

        List<String> images = variationResponse.getImages();
        for (String image : images) {
            if (StringUtils.isNotEmpty(image)) {
                this.image = image;
            }
        }
        
        if (!sizes.isEmpty()) {
            this.sizes = String.join(",", sizes.stream().map(String::valueOf).toArray(String[]::new));
        }
        if (!colors.isEmpty()) {
            this.colors = String.join(",", colors.stream().map(String::valueOf).toArray(String[]::new));
        }
    }
    
    public static void toUpdate(Product product, VariationResponse variationResponse, Set<String> sizes,
                                Set<String> colors, Set<String> types, long stockQuantity) {
        VariationResponse.Product variationProduct = variationResponse.getProduct();
        product.setCode(variationProduct.getDisplay_id());
        product.setSubCode(variationResponse.getDisplay_id());
        product.setStockQuantity(stockQuantity);
        product.setPrice(BigDecimal.valueOf(variationResponse.getRetail_price()));

        if (!sizes.isEmpty()) {
            product.setSizes(String.join(",", sizes.stream().map(String::valueOf).toArray(String[]::new)));
        }
        if (!colors.isEmpty()) {
            product.setColors(String.join(",", colors.stream().map(String::valueOf).toArray(String[]::new)));
        }
        if (!types.isEmpty()) {
            product.setTypes(String.join(",", types.stream().map(String::valueOf).toArray(String[]::new)));
        }
    }
}
