package vn.co.cake.ai.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "openai")
@Getter
@Setter
public class OpenAIConfig {
    @org.springframework.beans.factory.annotation.Value("${openai.api.key:}")
    private String apiKey;
    
    @org.springframework.beans.factory.annotation.Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;
    
    private String model = "gpt-4o-mini";
    private Double temperature = 0.7;
    private Integer maxTokens = 500;
    private Integer timeout = 30000;

    @PostConstruct
    public void init() {
        log.info("OpenAI Config initialized - apiKey present: {}, apiUrl: {}, model: {}", 
            apiKey != null && !apiKey.isEmpty(), apiUrl, model);
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("your-openai-api-key-here")) {
            log.warn("OpenAI API key is not configured or using default value. Please set openai.api.key in application properties.");
        } else {
            log.info("OpenAI configuration loaded successfully: model={}, apiUrl={}, apiKey length={}", 
                model, apiUrl, apiKey != null ? apiKey.length() : 0);
        }
    }
}
