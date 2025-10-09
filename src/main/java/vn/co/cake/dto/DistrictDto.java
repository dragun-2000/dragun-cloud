package vn.co.cake.dto;

import lombok.Data;

@Data
public class DistrictDto {
    private String code;
    private String name;
    private String type;
    private String slug;
    private String name_with_type;
    private String path;
    private String path_with_type;
    private String parent_code;
}
