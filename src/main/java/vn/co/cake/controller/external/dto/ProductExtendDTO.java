package vn.co.cake.controller.external.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductExtendDTO {
    private Long id;
    private Long shopId;
    private String name;
    private Double price;
    private String imageUrl;
    private String description;
    private String category;
}
