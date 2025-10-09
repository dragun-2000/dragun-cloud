package vn.co.cake.controller.external.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShopExtendDTO {
    private Long id;
    private String code; 
    private String name;
    private String address;
    private String phone;
    private String imageUrl;
    private String category;
}
