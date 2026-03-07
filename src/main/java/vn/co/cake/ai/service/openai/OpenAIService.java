package vn.co.cake.ai.service.openai;

import java.util.List;
import java.util.Map;

public interface OpenAIService {
    String callGPT(String systemPrompt, String userPrompt) throws Exception;
    String callGPTWithHistory(String systemPrompt, List<Map<String, String>> conversationHistory) throws Exception;
}
