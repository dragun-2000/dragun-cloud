package vn.co.cake.controller.thiyen;

import lombok.Data;

import java.util.List;

@Data
public class ProductDTO {
    private Long id;
    private String name;
    private Double price;
    private Double oldPrice;
    private Double bulkPrice;
    private Integer quantity;
    private String bulkQuantity;
    private Integer discount;
    private Integer reviewCount;
    private String shortDesc;
    private String benefits;
    private String targetUsers;
    private String usage;
    private String manufacturer;
    private String ingredients;
    private String detailedUsage;
    private String specifications;
    private String technology;
    private String storage;
    private String image;
    private String category;
    private Boolean isNew;
    private List<ProductGalleryDTO> gallery;  // list ảnh
    private List<ProductVariantDTO> variants; // list biến thể
}

