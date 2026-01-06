package vn.co.cake.ai.service.openai;

public interface OpenAIService {
    String callGPT(String systemPrompt, String userPrompt) throws Exception;
}
