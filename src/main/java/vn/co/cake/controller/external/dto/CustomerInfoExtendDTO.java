package vn.co.cake.controller.external.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerInfoExtendDTO {
    private String name;
    private String phone;
    private String address;
}
