package vn.co.cake.ai.service.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import vn.co.cake.ai.config.OpenAIConfig;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class OpenAIServiceImpl implements OpenAIService {
    
    private final RestTemplate restTemplate;
    private final OpenAIConfig openAIConfig;
    private final ObjectMapper objectMapper;
    
    public OpenAIServiceImpl(@Qualifier("openaiRestTemplate") RestTemplate restTemplate, 
                             OpenAIConfig openAIConfig) {
        this.restTemplate = restTemplate;
        this.openAIConfig = openAIConfig;
        this.objectMapper = new ObjectMapper();
        
        // Log để debug
        log.info("OpenAIServiceImpl initialized - apiKey present: {}, apiUrl: {}", 
            openAIConfig.getApiKey() != null && !openAIConfig.getApiKey().isEmpty(),
            openAIConfig.getApiUrl());
    }
    
    @Override
    public String callGPT(String systemPrompt, String userPrompt) throws Exception {
        try {
            if (openAIConfig.getApiKey() == null || openAIConfig.getApiKey().trim().isEmpty()) {
                log.error("OpenAI API key is not configured. Please set openai.api.key in application properties.");
                throw new Exception("OpenAI API key is not configured");
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAIConfig.getApiKey());
            
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", openAIConfig.getModel());
            requestBody.put("messages", new Object[]{systemMessage, userMessage});
            requestBody.put("temperature", openAIConfig.getTemperature());
            requestBody.put("max_tokens", openAIConfig.getMaxTokens());
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                openAIConfig.getApiUrl(), 
                request, 
                String.class
            );
            
            if (response.getStatusCode() != HttpStatus.OK) {
                log.error("OpenAI API error: status={}, body={}", 
                    response.getStatusCode(), response.getBody());
                throw new Exception("OpenAI API returned error: " + response.getStatusCode());
            }
            
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode choices = rootNode.path("choices");
            
            if (choices.isEmpty() || !choices.isArray()) {
                throw new Exception("Invalid response format from OpenAI API");
            }
            
            String content = choices.get(0).path("message").path("content").asText();
            
            if (content == null || content.trim().isEmpty()) {
                throw new Exception("Empty content from OpenAI API");
            }
            
            return content;
            
        } catch (ResourceAccessException e) {
            log.warn("OpenAI API timeout or connection error: {}", e.getMessage());
            throw new Exception("OpenAI API timeout: " + e.getMessage(), e);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode() != null) {
                if (e.getStatusCode().value() == 429) {
                    log.warn("OpenAI API quota exceeded (429). Using fallback size recommendation.");
                    throw new Exception("OpenAI API quota exceeded");
                } else if (e.getStatusCode().value() == 401) {
                    log.error("OpenAI API authentication failed. Please check API key.");
                    throw new Exception("OpenAI API authentication failed");
                }
            }
            log.warn("OpenAI API client error ({}): {}", 
                e.getStatusCode() != null ? e.getStatusCode().value() : "unknown", 
                e.getMessage());
            throw new Exception("OpenAI API error: " + e.getMessage(), e);
        } catch (RestClientException e) {
            log.warn("OpenAI API client error: {}", e.getMessage());
            throw new Exception("OpenAI API error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.warn("Error calling OpenAI API: {}", e.getMessage());
            throw e;
        }
    }
}
