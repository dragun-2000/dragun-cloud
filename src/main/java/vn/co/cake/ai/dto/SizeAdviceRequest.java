package vn.co.cake.ai.dto;

import lombok.Data;

@Data
public class SizeAdviceRequest {
    private Long productId;
    private String gender;
    private Integer height;
    private Integer weight;
    private String fitPreference;
    private String note;
}
