package vn.co.cake.dto;

import lombok.Data;

@Data
public class ProvinceDto {
    private String code;
    private String name;
    private String type;
    private String slug;
    private String name_with_type;
}
