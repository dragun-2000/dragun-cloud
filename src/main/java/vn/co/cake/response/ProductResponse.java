package vn.co.cake.response;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import vn.co.cake.entity.Product;
import vn.co.cake.enums.Colors;
import vn.co.cake.utils.BigDecimalUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ProductResponse {
    private long id;
    private String code;
    private String subCode;
    private String name;
    private String description;
    private String descriptionSize;
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
    private String colorBase;
    private String sizeBase;
    private List<String> colors;
    private List<String> codeColors;
    private List<String> sizes;
    private BigDecimal price;
    private String priceDisplay;
    private BigDecimal discount;
    private BigDecimal discountPrice;
    private BigDecimal finalPrice;
    private String discountPriceDisplay;
    private long stockQuantity;
    private List<String> category;
    private String relatedProduct1;
    private String relatedProduct2;
    private String relatedProduct3;
    private String relatedProduct4;
    private boolean isOutStock;
    private boolean isHasDiscount;

    public ProductResponse() {}

    public ProductResponse(Product product) {
        this.id = product.getId();
        this.code = product.getCode();
        this.subCode = product.getSubCode();
        this.name = product.getName();
        this.description = product.getDescription();
        this.descriptionSize = product.getDescriptionSize();
        this.productInformation = product.getProductInformation();
        this.image = product.getImage();
        this.image1 = product.getImage1();
        this.image2 = product.getImage2();
        this.image3 = product.getImage3();
        this.image4 = product.getImage4();
        this.image5 = product.getImage5();
        this.image6 = product.getImage6();
        this.image7 = product.getImage7();
        this.image8 = product.getImage8();
        this.image9 = product.getImage9();
        List<String> colors = new ArrayList<>();
        if (StringUtils.isNotEmpty(product.getColors())) {
            colors = Arrays.stream(product.getColors().split(","))
                    .distinct()
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toList());
            this.colorBase = product.getColors();
        }
        this.colors = colors;
        
        List<String> backgroundColor = new ArrayList<>();
        colors.forEach(color -> {
            String codeByName = Colors.getCodeByName(color);
            if (StringUtils.isNotBlank(codeByName)) {
                backgroundColor.add(codeByName);
            }
        });
        this.codeColors = backgroundColor;

        List<String> sizes = new ArrayList<>();
        if (StringUtils.isNotEmpty(product.getSizes())) {
            sizes = Arrays.stream(product.getSizes().split(","))
                    .distinct()
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toList());
            this.sizeBase = product.getSizes();
        }
        this.sizes = sizes;
        this.price = product.getPrice();
        this.discount = product.getDiscount() == null ? BigDecimal.ZERO : product.getDiscount();
        BigDecimal finalPrice = product.getPrice();
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (product.getDiscount() != null && product.getDiscount().longValue() > 0) {
            discountAmount = product.getPrice().multiply(product.getDiscount()).divide(new BigDecimal("100"), RoundingMode.HALF_UP);
            finalPrice = product.getPrice().subtract(discountAmount);
        }
        this.discountPrice = discountAmount;
        this.finalPrice = finalPrice;
        this.priceDisplay = BigDecimalUtil.formatMoney(this.price);
        this.discountPriceDisplay = BigDecimalUtil.formatMoney(finalPrice);
        this.stockQuantity = product.getStockQuantity();
        List<String> category = new ArrayList<>();
        if (StringUtils.isNotEmpty(product.getCategories())) {
            category = Arrays.stream(product.getCategories().split(","))
                    .distinct()
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toList());
        }
        this.category = category;
        this.relatedProduct1 = product.getRelatedProduct1();
        this.relatedProduct2 = product.getRelatedProduct2();
        this.relatedProduct3 = product.getRelatedProduct3();
        this.relatedProduct4 = product.getRelatedProduct4();
        this.isOutStock = product.getStockQuantity() <= 0;
        this.isHasDiscount = product.getDiscount().longValue() > 0;
    }
}
