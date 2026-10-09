package vn.co.cake.ai.dto.storechat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreChatResponse {
    private String message;
    private List<StoreChatProductCard> products;
    private boolean needBodyInfo;
    private boolean similarSuggestion;
    private Integer height;
    private Integer weight;
}
