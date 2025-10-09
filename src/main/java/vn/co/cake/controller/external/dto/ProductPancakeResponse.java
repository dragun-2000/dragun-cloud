package vn.co.cake.controller.external.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProductPancakeResponse {
    private String displayId;
    private String productId;
    private String variationId;
    private boolean success;
}

