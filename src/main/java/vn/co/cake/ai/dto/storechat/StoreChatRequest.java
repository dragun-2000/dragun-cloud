package vn.co.cake.ai.dto.storechat;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class StoreChatRequest {
    private String message;
    private Integer height;
    private Integer weight;
    private String gender;
    private List<Map<String, String>> conversationHistory;
}
