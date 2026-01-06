package vn.co.cake.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SizeAdviceResponse {
    private SizeAdviceData data;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SizeAdviceData {
        private Long productId;
        private SizeAdviceMessage message;
    }
}
