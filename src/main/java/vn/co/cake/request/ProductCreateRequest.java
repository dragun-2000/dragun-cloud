package vn.co.cake.request;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class ProductCreateRequest implements Serializable {
    private static final long serialVersionUID = -4115973602750199107L;
    private long id;
    private String name;
    private String code;
    private String subCode;
    private String description;
    private String descriptionSize;
    private String productInformation;
    private String imageUrl;
    private String image1Url;
    private String image2Url;
    private String image3Url;
    private String image4Url;
    private String image5Url;
    private String image6Url;
    private String image7Url;
    private String image8Url;
    private String image9Url;
    private String color;
    private String sizes;
    private BigDecimal price;
    private BigDecimal discount;
    private String category;
    private Long stockQuantity;
    private String relatedProduct1;
    private String relatedProduct2;
    private String relatedProduct3;
    private String relatedProduct4;
}
