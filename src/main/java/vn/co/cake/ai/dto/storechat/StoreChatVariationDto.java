package vn.co.cake.ai.dto.storechat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreChatVariationDto {
    private String variationId;
    private String color;
    private String size;
    private String type;
    private String image;
    private Long remainQuantity;
}
