package vn.co.cake.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import vn.co.cake.entity.Product;
import vn.co.cake.repository.ProductRepository;
import vn.co.cake.ai.dto.SizeAdviceMessage;
import vn.co.cake.ai.dto.SizeAdviceRequest;
import vn.co.cake.ai.dto.SizeAdviceResponse;
import vn.co.cake.ai.service.SizeAdviceService;
import vn.co.cake.ai.service.openai.OpenAIService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SizeAdviceServiceImpl implements SizeAdviceService {
    
    private final ProductRepository productRepository;
    private final OpenAIService openAIService;
    private final ObjectMapper objectMapper;
    
    public SizeAdviceServiceImpl(@Lazy ProductRepository productRepository,
                                 OpenAIService openAIService) {
        this.productRepository = productRepository;
        this.openAIService = openAIService;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public SizeAdviceResponse getSizeAdvice(SizeAdviceRequest request) {
        try {
            Product product = productRepository.findFirstByIdAndDeletedIsFalse(request.getProductId());
            if (product == null) {
                throw new IllegalArgumentException("Product not found: " + request.getProductId());
            }
            
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(request, product);
            
            String aiResponse = openAIService.callGPT(systemPrompt, userPrompt);
            SizeAdviceResponse response = parseAIResponse(aiResponse, request.getProductId());
            
            return response;
            
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("quota")) {
                log.info("OpenAI quota exceeded, using fallback size recommendation based on standard size chart");
            } else {
                log.warn("AI service error, using fallback: {}", e.getMessage());
            }
            return fallbackToDefaultSize(request);
        }
    }
    
    private String buildSystemPrompt() {
        return "Bạn là nhân viên tư vấn size quần áo cho website thương mại điện tử.\n" +
               "Mục tiêu của bạn là đề xuất size CHÍNH XÁC NHẤT cho khách hàng.\n\n" +
               "Nguyên tắc:\n" +
               "1. Ưu tiên bảng đo THỰC TẾ của sản phẩm.\n" +
               "2. So sánh khách với mẫu mặc nếu có.\n" +
               "3. Tránh tư vấn size quá rộng hoặc quá dài.\n" +
               "4. Nếu thiếu thông tin quan trọng, hỏi tối đa 2 câu ngắn.\n" +
               "5. Chỉ trả về JSON đúng format, không thêm nội dung ngoài JSON.\n\n" +
               "Format JSON response:\n" +
               "{\n" +
               "  \"data\": {\n" +
               "    \"productId\": 1,\n" +
               "    \"message\": {\n" +
               "      \"recommendedSize\": \"L\",\n" +
               "      \"backupSize\": \"M\",\n" +
               "      \"reason\": [\"Lý do 1\", \"Lý do 2\"],\n" +
               "      \"productFitComment\": \"Form oversize, mặc rộng thoải mái\",\n" +
               "      \"closingQuestion\": \"Bạn thích mặc vừa hay rộng hơn?\"\n" +
               "    }\n" +
               "  }\n" +
               "}";
    }
    
    private String buildUserPrompt(SizeAdviceRequest request, Product product) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("THÔNG TIN KHÁCH HÀNG:\n");
        prompt.append("- Giới tính: ").append(request.getGender()).append("\n");
        prompt.append("- Chiều cao (cm): ").append(request.getHeight()).append("\n");
        prompt.append("- Cân nặng (kg): ").append(request.getWeight()).append("\n");
        prompt.append("- Phong cách mong muốn: ").append(
            StringUtils.isNotBlank(request.getFitPreference()) ? request.getFitPreference() : "chưa rõ"
        ).append("\n");
        prompt.append("- Mô tả thêm: ").append(
            StringUtils.isNotBlank(request.getNote()) ? request.getNote() : "không có"
        ).append("\n\n");
        
        prompt.append("THÔNG TIN SẢN PHẨM:\n");
        prompt.append("- Product ID: ").append(product.getId()).append("\n");
        prompt.append("- Tên sản phẩm: ").append(product.getName()).append("\n");
        prompt.append("- Form sản phẩm: ").append(extractProductForm(product)).append("\n\n");
        
        prompt.append("BẢNG ĐO SẢN PHẨM:\n");
        if (StringUtils.isNotBlank(product.getDescriptionSize())) {
            prompt.append(product.getDescriptionSize()).append("\n\n");
        } else {
            prompt.append("Không có bảng đo chi tiết\n\n");
        }
        
        prompt.append("BẢNG SIZE THAM CHIẾU CHUNG (CHỈ DÙNG KHI KHÔNG CÓ BẢNG ĐO):\n");
        prompt.append("Áo nam / unisex:\n");
        prompt.append("- 45–55kg: S\n");
        prompt.append("- 56–65kg: M\n");
        prompt.append("- 66–75kg: L\n");
        prompt.append("- 76–85kg: XL\n");
        prompt.append("- 86–95kg: XXL\n\n");
        
        prompt.append("Áo nữ:\n");
        prompt.append("- 40–47kg: S\n");
        prompt.append("- 48–55kg: M\n");
        prompt.append("- 56–62kg: L\n");
        prompt.append("- 63–70kg: XL\n\n");
        
        prompt.append("Tinh chỉnh chiều cao:\n");
        prompt.append("- >175cm: ưu tiên size đủ dài\n");
        prompt.append("- <160cm: tránh size quá dài hoặc quá rộng\n");
        
        return prompt.toString();
    }
    
    private String extractProductForm(Product product) {
        String description = product.getDescription();
        if (StringUtils.isBlank(description)) {
            return "chưa rõ";
        }
        
        String descLower = description.toLowerCase();
        if (descLower.contains("oversize") || descLower.contains("over size")) {
            return "oversize";
        } else if (descLower.contains("ôm") || descLower.contains("slim")) {
            return "ôm";
        } else if (descLower.contains("rộng") || descLower.contains("loose")) {
            return "rộng";
        } else if (descLower.contains("vừa") || descLower.contains("regular")) {
            return "vừa";
        }
        
        return "chưa rõ";
    }
    
    private SizeAdviceResponse parseAIResponse(String aiResponse, Long productId) throws Exception {
        try {
            String jsonContent = extractJsonFromResponse(aiResponse);
            JsonNode rootNode = objectMapper.readTree(jsonContent);
            
            JsonNode dataNode = rootNode.path("data");
            if (dataNode.isMissingNode()) {
                throw new Exception("Missing 'data' field in AI response");
            }
            
            JsonNode messageNode = dataNode.path("message");
            if (messageNode.isMissingNode()) {
                throw new Exception("Missing 'message' field in AI response");
            }
            
            String recommendedSize = messageNode.path("recommendedSize").asText();
            String backupSize = messageNode.path("backupSize").asText();
            
            if (StringUtils.isBlank(recommendedSize)) {
                throw new Exception("Missing 'recommendedSize' in AI response");
            }
            
            List<String> reasons = new ArrayList<>();
            JsonNode reasonNode = messageNode.path("reason");
            if (reasonNode.isArray()) {
                for (JsonNode reason : reasonNode) {
                    reasons.add(reason.asText());
                }
            }
            
            String productFitComment = messageNode.path("productFitComment").asText("");
            String closingQuestion = messageNode.path("closingQuestion").asText("");
            
            SizeAdviceMessage message = SizeAdviceMessage.builder()
                .recommendedSize(recommendedSize)
                .backupSize(backupSize)
                .reason(reasons)
                .productFitComment(productFitComment)
                .closingQuestion(closingQuestion)
                .build();
            
            SizeAdviceResponse.SizeAdviceData data = SizeAdviceResponse.SizeAdviceData.builder()
                .productId(productId)
                .message(message)
                .build();
            
            return SizeAdviceResponse.builder()
                .data(data)
                .build();
            
        } catch (Exception e) {
            log.error("Error parsing AI response: {}", e.getMessage());
            throw new Exception("Failed to parse AI response: " + e.getMessage(), e);
        }
    }
    
    private String extractJsonFromResponse(String response) {
        response = response.trim();
        
        int startIdx = response.indexOf("{");
        int endIdx = response.lastIndexOf("}");
        
        if (startIdx == -1 || endIdx == -1 || startIdx >= endIdx) {
            return response;
        }
        
        return response.substring(startIdx, endIdx + 1);
    }
    
    private SizeAdviceResponse fallbackToDefaultSize(SizeAdviceRequest request) {
        String gender = request.getGender().toLowerCase();
        Integer weight = request.getWeight();
        Integer height = request.getHeight();
        
        String recommendedSize = calculateDefaultSize(gender, weight, height);
        String backupSize = getBackupSize(recommendedSize);
        
        List<String> reasons = new ArrayList<>();
        reasons.add("Dựa trên bảng size tham chiếu chung");
        reasons.add("Chiều cao: " + height + "cm, Cân nặng: " + weight + "kg");
        
        SizeAdviceMessage message = SizeAdviceMessage.builder()
            .recommendedSize(recommendedSize)
            .backupSize(backupSize)
            .reason(reasons)
            .productFitComment("Tư vấn dựa trên bảng size tham chiếu")
            .closingQuestion("Bạn có thể thử size này")
            .build();
        
        SizeAdviceResponse.SizeAdviceData data = SizeAdviceResponse.SizeAdviceData.builder()
            .productId(request.getProductId())
            .message(message)
            .build();
        
        return SizeAdviceResponse.builder()
            .data(data)
            .build();
    }
    
    private String calculateDefaultSize(String gender, Integer weight, Integer height) {
        if ("female".equals(gender) || "nữ".equals(gender)) {
            if (weight >= 40 && weight <= 47) return "S";
            if (weight >= 48 && weight <= 55) return "M";
            if (weight >= 56 && weight <= 62) return "L";
            if (weight >= 63 && weight <= 70) return "XL";
        } else {
            if (weight >= 45 && weight <= 55) return "S";
            if (weight >= 56 && weight <= 65) return "M";
            if (weight >= 66 && weight <= 75) return "L";
            if (weight >= 76 && weight <= 85) return "XL";
            if (weight >= 86 && weight <= 95) return "XXL";
        }
        
        if (weight < 45) return "S";
        if (weight > 95) return "XXL";
        
        return "M";
    }
    
    private String getBackupSize(String recommendedSize) {
        if ("S".equals(recommendedSize)) return "M";
        if ("M".equals(recommendedSize)) return "S";
        if ("L".equals(recommendedSize)) return "M";
        if ("XL".equals(recommendedSize)) return "L";
        if ("XXL".equals(recommendedSize)) return "XL";
        return "M";
    }
}
