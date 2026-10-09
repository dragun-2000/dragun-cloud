package vn.co.cake.ai.dto.storechat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreChatProductCard {
    private Long id;
    private String name;
    private String image;
    private String priceDisplay;
    private String discountPriceDisplay;
    private BigDecimal finalPrice;
    private boolean hasDiscount;
    private boolean outStock;
    private String detailUrl;
    private List<String> colors;
    private List<String> sizes;
    private List<String> types;
    private List<StoreChatVariationDto> variations;
    private String recommendedSize;
}
