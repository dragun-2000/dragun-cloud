package vn.co.cake.ai.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class SizeAdviceChatRequest {
    private Long productId;
    private String message;
    // Context từ lần tư vấn đầu
    private String gender;
    private Integer height;
    private Integer weight;
    private String fitPreference;
    private String note;
    // Conversation history để AI hiểu được luồng chat
    private List<Map<String, String>> conversationHistory;
}
