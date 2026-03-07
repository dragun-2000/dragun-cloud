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
import vn.co.cake.ai.dto.SizeAdviceChatRequest;
import vn.co.cake.ai.service.SizeAdviceService;
import vn.co.cake.ai.service.openai.OpenAIService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class SizeAdviceServiceImpl implements SizeAdviceService {
    
    // Constants
    private static final String NOT_AVAILABLE = "NOT_AVAILABLE";
    private static final String KHONG_PHU_HOP = "KHÔNG PHÙ HỢP";
    private static final String CONFLICT_KEYWORDS = "không có size phù hợp|không phù hợp|not_available";
    private static final Pattern SIZE_PATTERN = Pattern.compile("size\\s+([smlx]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SIZES_LIST_PATTERN = Pattern.compile("(?:sizes?|size)\\s*:?\\s*([smlx]+(?:\\s*,\\s*[smlx]+)*)", Pattern.CASE_INSENSITIVE);
    
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
            log.info("Processing size advice request for productId: {}, gender: {}, height: {}cm, weight: {}kg", 
                request.getProductId(), request.getGender(), request.getHeight(), request.getWeight());
            
            Product product = productRepository.findFirstByIdAndDeletedIsFalse(request.getProductId());
            if (product == null) {
                throw new IllegalArgumentException("Product not found: " + request.getProductId());
            }
            
            // Log thông tin sản phẩm để debug
            log.debug("Product found - ID: {}, Name: {}, Sizes: {}, Has descriptionSize: {}", 
                product.getId(), 
                product.getName(), 
                product.getSizes(),
                StringUtils.isNotBlank(product.getDescriptionSize()));
            
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(request, product);
            
            // Log đầy đủ prompt ra console
            log.info("========== SYSTEM PROMPT ==========");
            log.info(systemPrompt);
            log.info("========== USER PROMPT ==========");
            log.info(userPrompt);
            log.info("========== END PROMPT ==========");
            log.info("Total prompt length: {} characters", userPrompt.length());
            
            String aiResponse = openAIService.callGPT(systemPrompt, userPrompt);
            SizeAdviceResponse response = parseAIResponse(aiResponse, request.getProductId(), product);
            
            // Validate và sửa response - method này đã xử lý đầy đủ, không cần double-check
            response = validateAndFixSizeAvailability(response, product, request);
            
            log.info("Size advice generated successfully for productId: {}, recommendedSize: {}", 
                request.getProductId(), 
                response != null && response.getData() != null && response.getData().getMessage() != null
                    ? response.getData().getMessage().getRecommendedSize() 
                    : "N/A");
            
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
        return "Bạn là nhân viên tư vấn size quần áo chuyên nghiệp cho website thương mại điện tử.\n" +
               "Mục tiêu của bạn là đề xuất size CHÍNH XÁC NHẤT cho khách hàng dựa trên BẢNG ĐO SẢN PHẨM THỰC TẾ.\n\n" +
               "Nguyên tắc QUAN TRỌNG (TUÂN THỦ NGHIÊM NGẶT):\n" +
               "1. BẮT BUỘC sử dụng BẢNG ĐO SẢN PHẨM (THÔNG SỐ SIZE THỰC TẾ) để tính toán size phù hợp. Đây là thông tin CHÍNH XÁC NHẤT.\n" +
               "2. Tính toán CHÍNH XÁC dựa trên số đo:\n" +
               "   - So sánh chiều cao khách hàng với độ dài áo trong bảng đo:\n" +
               "     * Nếu khách cao hơn độ dài áo nhiều → cần size lớn hơn\n" +
               "     * Nếu khách thấp hơn hoặc bằng độ dài áo → size đó phù hợp\n" +
               "   - So sánh cân nặng/vòng ngực khách với vòng ngực trong bảng đo:\n" +
               "     * Nếu vòng ngực khách lớn hơn vòng ngực size → cần size lớn hơn\n" +
               "     * Nếu vòng ngực khách nhỏ hơn hoặc bằng → size đó phù hợp\n" +
               "   - Xem xét thông tin mẫu mặc (nếu có):\n" +
               "     * Nếu mẫu có chiều cao/cân nặng TƯƠNG TỰ hoặc GẦN BẰNG khách và mặc size X → khách cũng cần size X\n" +
               "     * Ví dụ: Mẫu 1m77 nặng 73kg mặc XL → Khách 1m73 nặng 73kg cũng cần XL (chiều cao gần bằng, cân nặng bằng)\n" +
               "     * Ví dụ: Mẫu 1m77 nặng 73kg mặc XL → Khách 1m60 nặng 48kg cần size nhỏ hơn (cả chiều cao và cân nặng đều nhỏ hơn nhiều)\n" +
               "3. QUAN TRỌNG NHẤT - QUY TRÌNH BẮT BUỘC:\n" +
               "   BƯỚC 1: Tính toán size phù hợp dựa trên bảng đo sản phẩm (ví dụ: tính ra size M)\n" +
               "   BƯỚC 2: Kiểm tra size M có trong danh sách SIZES CÓ SẴN không (danh sách được cung cấp trong user prompt)\n" +
               "   BƯỚC 3: QUYẾT ĐỊNH:\n" +
               "     * NẾU size M CÓ trong danh sách → ĐÂY LÀ SIZE PHÙ HỢP, TRẢ VỀ:\n" +
               "       - recommendedSize = \"M\" (CHỈ size thuần, không thêm text)\n" +
               "       - reason = [\"Dựa vào bảng đo sản phẩm, size M phù hợp với thông số của bạn\", \"Chiều cao X cm phù hợp với độ dài áo size M\", \"Cân nặng Y kg phù hợp với vòng ngực size M\"]\n" +
               "       - productFitComment = \"Form vừa, mặc vừa vặn\" (hoặc tương tự tích cực)\n" +
               "       - closingQuestion = \"Bạn có thể thử size này\" (hoặc tương tự tích cực)\n" +
               "       - TUYỆT ĐỐI KHÔNG được nói \"không có size phù hợp\", \"Nhưng sản phẩm chỉ có...\", \"liên hệ shop\", \"chọn sản phẩm khác\"\n" +
               "     * NẾU size M KHÔNG CÓ trong danh sách → TRẢ VỀ:\n" +
               "       - recommendedSize = \"NOT_AVAILABLE\" (chính xác chuỗi này)\n" +
               "       - reason = [\"Dựa vào bảng đo, bạn cần size M\", \"Nhưng size M hiện đang hết hàng\", \"Sản phẩm hiện chỉ còn sizes S, L\"]\n" +
               "       - productFitComment = \"Bạn cần size M nhưng size này hiện đang hết hàng\"\n" +
               "       - closingQuestion = \"Vui lòng liên hệ shop theo hotline để đặt hàng size M hoặc chọn sản phẩm khác có size phù hợp\"\n" +
               "   BƯỚC 4: KIỂM TRA LẠI TRƯỚC KHI TRẢ VỀ:\n" +
               "     - Nếu recommendedSize = \"M\" và M có trong danh sách → Đảm bảo KHÔNG có từ \"không có size phù hợp\" trong reason/productFitComment/closingQuestion\n" +
               "     - Nếu recommendedSize = \"NOT_AVAILABLE\" → Đảm bảo reason giải thích rõ tại sao không có size phù hợp\n" +
               "4. TUYỆT ĐỐI KHÔNG được tư vấn size nhỏ hơn hoặc lớn hơn nếu size phù hợp không có sẵn.\n" +
               "   Ví dụ: Khách cần XL nhưng sản phẩm chỉ có S, M → PHẢI báo \"NOT_AVAILABLE\" chứ KHÔNG được tư vấn M\n" +
               "   Ví dụ: Khách cần M và sản phẩm có S, M → PHẢI tư vấn M với reason \"size M phù hợp với thông số của bạn\"\n" +
               "5. TUYỆT ĐỐI KHÔNG được tạo ra response mâu thuẫn - ĐÂY LÀ LỖI NGHIÊM TRỌNG:\n" +
               "   - Nếu bạn tính toán size M là phù hợp và sản phẩm CÓ size M trong danh sách → PHẢI tư vấn size M với lý do tích cực\n" +
               "   - KHÔNG BAO GIỜ được nói \"Dựa vào bảng đo, bạn cần size M\" rồi lại nói \"Nhưng sản phẩm chỉ có sizes S, M\" và \"Không có size phù hợp\"\n" +
               "   - Nếu sản phẩm CÓ size M trong danh sách → size M LÀ phù hợp và có sẵn, PHẢI tư vấn size M\n" +
               "   - Nếu bạn thấy \"sản phẩm chỉ có sizes S, M\" và bạn tính toán cần size M → size M CÓ SẴN, PHẢI tư vấn size M\n" +
               "   - Ví dụ SAI (TUYỆT ĐỐI KHÔNG LÀM):\n" +
               "     * recommendedSize=\"M\", reason=[\"Dựa vào bảng đo, bạn cần size M\", \"Nhưng sản phẩm chỉ có S, M\"], productFitComment=\"Không có size phù hợp\"\n" +
               "     * → SAI vì: M có trong danh sách (S, M) nhưng lại nói \"không có size phù hợp\"\n" +
               "   - Ví dụ ĐÚNG:\n" +
               "     * recommendedSize=\"M\", reason=[\"Dựa vào bảng đo sản phẩm, size M phù hợp với thông số của bạn\", \"Chiều cao 156cm phù hợp với độ dài áo size M\", \"Cân nặng 55kg phù hợp với vòng ngực size M\"], productFitComment=\"Form vừa, mặc vừa vặn\"\n" +
               "     * → ĐÚNG vì: M có trong danh sách, tư vấn M với lý do tích cực\n" +
               "6. Khi báo \"NOT_AVAILABLE\", hãy:\n" +
               "   - Giải thích rõ và tích cực: \"Dựa vào bảng đo, bạn cần size X, nhưng size X hiện đang hết hàng\"\n" +
               "   - Thông báo sizes còn lại: \"Sản phẩm hiện chỉ còn sizes Y, Z\"\n" +
               "   - productFitComment: \"Bạn cần size X nhưng size này hiện đang hết hàng\" (KHÔNG nói \"không có size phù hợp\")\n" +
               "   - Đề xuất: \"Vui lòng liên hệ shop theo hotline để đặt hàng size X hoặc chọn sản phẩm khác có size phù hợp\"\n" +
               "   - CHỈ báo NOT_AVAILABLE khi size phù hợp THỰC SỰ không có trong danh sách\n" +
               "   - CÁCH NÓI: Dùng \"hết hàng\" thay vì \"không có size phù hợp\" để tích cực hơn\n" +
               "7. Xem xét form sản phẩm (ôm/vừa/rộng/oversize) để điều chỉnh size đề xuất (chỉ khi size phù hợp có sẵn).\n" +
               "8. Format recommendedSize: CHỈ trả về size thuần (ví dụ: \"M\", \"L\", \"XL\"), KHÔNG thêm prefix \"Size \" hoặc text khác.\n" +
               "9. Nếu sản phẩm KHÔNG CÓ bảng đo, hãy tư vấn như một nhân viên chuyên nghiệp.\n" +
               "10. Chỉ trả về JSON đúng format, không thêm nội dung ngoài JSON.\n" +
               "11. KIỂM TRA LẠI: Trước khi trả về response, đảm bảo:\n" +
               "    - Nếu recommendedSize là \"M\" (hoặc S, L, XL...) và M có trong danh sách sizes có sẵn → reason KHÔNG được chứa \"không có size phù hợp\"\n" +
               "    - Nếu recommendedSize là \"NOT_AVAILABLE\" → reason PHẢI giải thích tại sao không có size phù hợp\n\n" +
               "Format JSON response:\n" +
               "Khi CÓ size phù hợp:\n" +
               "{\n" +
               "  \"data\": {\n" +
               "    \"productId\": 1,\n" +
               "    \"message\": {\n" +
               "      \"recommendedSize\": \"M\",\n" +
               "      \"backupSize\": \"L\",\n" +
               "      \"reason\": [\"Chiều cao 160cm phù hợp với độ dài áo size M\", \"Cân nặng 48kg phù hợp với vòng ngực size M\"],\n" +
               "      \"productFitComment\": \"Form vừa, mặc vừa vặn\",\n" +
               "      \"closingQuestion\": \"Bạn có thể thử size này\"\n" +
               "    }\n" +
               "  }\n" +
               "}\n\n" +
               "Khi KHÔNG CÓ size phù hợp (size hết hàng):\n" +
               "{\n" +
               "  \"data\": {\n" +
               "    \"productId\": 1,\n" +
               "    \"message\": {\n" +
               "      \"recommendedSize\": \"NOT_AVAILABLE\",\n" +
               "      \"backupSize\": \"\",\n" +
               "      \"reason\": [\"Dựa vào bảng đo, bạn cần size L\", \"Nhưng size L hiện đang hết hàng\", \"Sản phẩm hiện chỉ còn sizes S, M\"],\n" +
               "      \"productFitComment\": \"Bạn cần size L nhưng size này hiện đang hết hàng\",\n" +
               "      \"closingQuestion\": \"Vui lòng liên hệ shop theo hotline để đặt hàng size L hoặc chọn sản phẩm khác có size phù hợp\"\n" +
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
        prompt.append("- Form sản phẩm: ").append(extractProductForm(product)).append("\n");
        
        // Thêm thông tin sizes có sẵn của sản phẩm - QUAN TRỌNG để kiểm tra
        String availableSizes = product.getSizes();
        if (StringUtils.isNotBlank(availableSizes)) {
            prompt.append("- Sizes có sẵn: ").append(availableSizes).append("\n");
            prompt.append("  ⚠️ QUAN TRỌNG: Sau khi tính toán size phù hợp, BẮT BUỘC kiểm tra size đó có trong danh sách này không.\n");
            prompt.append("  Nếu KHÔNG CÓ → Phải báo \"NOT_AVAILABLE\" và giải thích rõ ràng.\n");
        } else {
            prompt.append("- Sizes có sẵn: Không rõ\n");
        }
        prompt.append("\n");
        
        // Bảng đo sản phẩm - làm sạch HTML tags
        prompt.append("BẢNG ĐO SẢN PHẨM (THÔNG SỐ SIZE THỰC TẾ):\n");
        String descriptionSize = product.getDescriptionSize();
        if (StringUtils.isNotBlank(descriptionSize)) {
            // Làm sạch HTML tags và format lại
            String cleanSizeInfo = cleanHtmlTags(descriptionSize);
            prompt.append(cleanSizeInfo).append("\n\n");
            log.debug("Product {} - Size info found: {} characters", product.getId(), cleanSizeInfo.length());
        } else {
            prompt.append("Không có bảng đo chi tiết cho sản phẩm này.\n");
            prompt.append("Hãy tư vấn như một nhân viên chuyên nghiệp: giải thích rõ ràng, đưa ra gợi ý dựa trên thông tin có sẵn, và khuyên khách hàng nên đo cơ thể hoặc tham khảo thêm.\n\n");
            log.warn("Product {} - No descriptionSize found", product.getId());
        }
        
        String finalPrompt = prompt.toString();
        log.debug("Built user prompt for product {}: {} characters", product.getId(), finalPrompt.length());
        
        return finalPrompt;
    }
    
    /**
     * Làm sạch HTML tags từ descriptionSize để đảm bảo prompt sạch cho AI
     */
    private String cleanHtmlTags(String htmlText) {
        if (StringUtils.isBlank(htmlText)) {
            return "";
        }
        
        // Thay thế các HTML tags phổ biến bằng text tương ứng
        String cleaned = htmlText
            .replaceAll("<br\\s*/?>", "\n")  // <br> hoặc <br/> thành xuống dòng
            .replaceAll("<br\\s+/>", "\n")    // <br /> thành xuống dòng
            .replaceAll("\\n+", "\n")         // Nhiều xuống dòng thành một
            .replaceAll("<[^>]+>", "")        // Xóa tất cả HTML tags còn lại
            .replaceAll("&nbsp;", " ")        // Thay &nbsp; bằng space
            .replaceAll("&amp;", "&")         // Thay &amp; bằng &
            .replaceAll("&lt;", "<")           // Thay &lt; bằng <
            .replaceAll("&gt;", ">")           // Thay &gt; bằng >
            .replaceAll("&quot;", "\"")       // Thay &quot; bằng "
            .trim();
        
        return cleaned;
    }
    
    private String extractProductForm(Product product) {
        // Tìm form trong description
        String description = product.getDescription();
        if (StringUtils.isNotBlank(description)) {
            // Làm sạch HTML tags trước khi tìm
            String cleanDesc = cleanHtmlTags(description).toLowerCase();
            
            if (cleanDesc.contains("oversize") || cleanDesc.contains("over size") || cleanDesc.contains("over-size")) {
                log.debug("Product {} - Form detected: oversize from description", product.getId());
                return "oversize";
            } else if (cleanDesc.contains("ôm") || cleanDesc.contains("slim") || cleanDesc.contains("fit")) {
                log.debug("Product {} - Form detected: ôm from description", product.getId());
                return "ôm";
            } else if (cleanDesc.contains("rộng") || cleanDesc.contains("loose") || cleanDesc.contains("relaxed")) {
                log.debug("Product {} - Form detected: rộng from description", product.getId());
                return "rộng";
            } else if (cleanDesc.contains("vừa") || cleanDesc.contains("regular") || cleanDesc.contains("standard")) {
                log.debug("Product {} - Form detected: vừa from description", product.getId());
                return "vừa";
            }
        }
        
        // Tìm form trong descriptionSize nếu không tìm thấy trong description
        String descriptionSize = product.getDescriptionSize();
        if (StringUtils.isNotBlank(descriptionSize)) {
            String cleanSizeDesc = cleanHtmlTags(descriptionSize).toLowerCase();
            if (cleanSizeDesc.contains("oversize") || cleanSizeDesc.contains("over size")) {
                log.debug("Product {} - Form detected: oversize from descriptionSize", product.getId());
                return "oversize";
            } else if (cleanSizeDesc.contains("ôm") || cleanSizeDesc.contains("slim")) {
                log.debug("Product {} - Form detected: ôm from descriptionSize", product.getId());
                return "ôm";
            } else if (cleanSizeDesc.contains("rộng") || cleanSizeDesc.contains("loose")) {
                log.debug("Product {} - Form detected: rộng from descriptionSize", product.getId());
                return "rộng";
            } else if (cleanSizeDesc.contains("vừa") || cleanSizeDesc.contains("regular")) {
                log.debug("Product {} - Form detected: vừa from descriptionSize", product.getId());
                return "vừa";
            }
        }
        
        log.warn("Product {} - Form not detected, using default: chưa rõ", product.getId());
        return "chưa rõ";
    }
    
    /**
     * Validate và sửa response nếu size đề xuất không có sẵn hoặc có mâu thuẫn
     */
    private SizeAdviceResponse validateAndFixSizeAvailability(SizeAdviceResponse response, Product product, SizeAdviceRequest request) {
        if (!isValidResponse(response)) {
            return response;
        }
        
        SizeAdviceMessage message = response.getData().getMessage();
        String recommendedSize = message.getRecommendedSize();
        
        // Nếu đã báo NOT_AVAILABLE và không có size nào phù hợp, giữ nguyên
        if (isNotAvailable(recommendedSize)) {
            return response;
        }
        
        String availableSizes = product.getSizes();
        if (StringUtils.isBlank(availableSizes)) {
            log.warn("Product {} - No sizes available, cannot validate", product.getId());
            return response;
        }
        
        // Parse và normalize sizes
        List<String> availableSizeList = parseAvailableSizes(availableSizes);
        String normalizedRecommendedSize = normalizeSize(recommendedSize);
        String recommendedSizeUpper = normalizedRecommendedSize.toUpperCase().trim();
        
        log.info("Product {} - Validating size: original='{}', normalized='{}', available={}", 
            product.getId(), recommendedSize, recommendedSizeUpper, availableSizeList);
        
        // Kiểm tra và sửa size từ reason nếu cần
        SizeValidationResult validationResult = validateSizeAvailability(
            recommendedSize, normalizedRecommendedSize, recommendedSizeUpper, 
            availableSizeList, message.getReason());
        
        log.info("Product {} - Size validation result: sizeAvailable={}, size='{}', availableSizeList={}", 
            product.getId(), validationResult.isAvailable(), validationResult.getSize(), availableSizeList);
        
        // Xử lý theo kết quả validation
        if (validationResult.isAvailable()) {
            return handleAvailableSize(response, message, validationResult.getSize(), product, request);
        } else {
            return handleUnavailableSize(response, validationResult.getSize(), availableSizes);
        }
    }
    
    /**
     * Kiểm tra response có hợp lệ không
     */
    private boolean isValidResponse(SizeAdviceResponse response) {
        return response != null && 
               response.getData() != null && 
               response.getData().getMessage() != null;
    }
    
    /**
     * Kiểm tra recommendedSize có phải NOT_AVAILABLE không
     */
    private boolean isNotAvailable(String recommendedSize) {
        return NOT_AVAILABLE.equalsIgnoreCase(recommendedSize) || 
               KHONG_PHU_HOP.equalsIgnoreCase(recommendedSize);
    }
    
    /**
     * Parse danh sách sizes có sẵn
     */
    private List<String> parseAvailableSizes(String availableSizes) {
        List<String> sizeList = new ArrayList<>();
        String[] sizes = availableSizes.split(",");
        for (String size : sizes) {
            String normalized = normalizeSize(size);
            if (StringUtils.isNotBlank(normalized)) {
                sizeList.add(normalized.toUpperCase());
            }
        }
        log.info("Available sizes (normalized): {}", sizeList);
        return sizeList;
    }
    
    /**
     * Kết quả validation size
     */
    private static class SizeValidationResult {
        private final boolean available;
        private final String size;
        
        SizeValidationResult(boolean available, String size) {
            this.available = available;
            this.size = size;
        }
        
        boolean isAvailable() { return available; }
        String getSize() { return size; }
    }
    
    /**
     * Validate size availability và extract size từ reason nếu cần
     */
    private SizeValidationResult validateSizeAvailability(
            String originalSize, String normalizedSize, String sizeUpper,
            List<String> availableSizeList, List<String> reasons) {
        
        boolean sizeAvailable = availableSizeList.contains(sizeUpper);
        
        // Nếu không tìm thấy, thử extract từ reason
        if (!sizeAvailable && reasons != null) {
            String detectedSize = extractSizeFromReason(reasons, availableSizeList);
            if (detectedSize != null && availableSizeList.contains(detectedSize.toUpperCase())) {
                log.info("Detected size '{}' from reason, size is available", detectedSize);
                return new SizeValidationResult(true, detectedSize);
            }
        }
        
        return new SizeValidationResult(sizeAvailable, normalizedSize);
    }
    
    /**
     * Xử lý khi size có sẵn - kiểm tra và sửa conflict
     */
    private SizeAdviceResponse handleAvailableSize(
            SizeAdviceResponse response, SizeAdviceMessage message, 
            String normalizedSize, Product product, SizeAdviceRequest request) {
        
        ConflictInfo conflict = detectConflict(message, normalizedSize, product.getSizes());
        
        if (conflict.hasConflict()) {
            log.warn("Product {} - Detected conflict (type: {}): size='{}' is available but response says 'no suitable size'", 
                product.getId(), conflict.getType(), normalizedSize);
            return fixConflictResponse(response, message, normalizedSize, request);
        }
        
        return response;
    }
    
    /**
     * Xử lý khi size không có sẵn
     */
    private SizeAdviceResponse handleUnavailableSize(
            SizeAdviceResponse response, String normalizedSize, String availableSizes) {
        
        log.warn("Product {} - Recommended size '{}' not in available sizes: {}", 
            response.getData().getProductId(), normalizedSize, availableSizes);
        
        List<String> newReasons = new ArrayList<>();
        newReasons.add("Dựa vào bảng đo sản phẩm, bạn cần size " + normalizedSize);
        newReasons.add("Nhưng size " + normalizedSize + " hiện đang hết hàng");
        if (StringUtils.isNotBlank(availableSizes)) {
            newReasons.add("Sản phẩm hiện chỉ còn sizes: " + availableSizes);
        }
        
        SizeAdviceMessage newMessage = SizeAdviceMessage.builder()
            .recommendedSize(NOT_AVAILABLE)
            .backupSize("")
            .reason(newReasons)
            .productFitComment("Bạn cần size " + normalizedSize + " nhưng size này hiện đang hết hàng")
            .closingQuestion("Vui lòng liên hệ shop theo hotline để đặt hàng size " + normalizedSize + " hoặc chọn sản phẩm khác có size phù hợp")
            .build();
        
        return rebuildResponse(response, newMessage);
    }
    
    /**
     * Thông tin conflict
     */
    private static class ConflictInfo {
        private final boolean hasConflict;
        private final String type;
        
        ConflictInfo(boolean hasConflict, String type) {
            this.hasConflict = hasConflict;
            this.type = type;
        }
        
        boolean hasConflict() { return hasConflict; }
        String getType() { return type; }
    }
    
    /**
     * Phát hiện conflict trong response
     */
    private ConflictInfo detectConflict(SizeAdviceMessage message, String normalizedSize, String availableSizes) {
        // Kiểm tra reason
        if (hasConflictInReasons(message.getReason(), normalizedSize, availableSizes)) {
            return new ConflictInfo(true, "reason");
        }
        
        // Kiểm tra productFitComment
        if (hasConflictInText(message.getProductFitComment())) {
            return new ConflictInfo(true, "productFitComment");
        }
        
        // Kiểm tra closingQuestion
        if (hasConflictInClosingQuestion(message.getClosingQuestion())) {
            return new ConflictInfo(true, "closingQuestion");
        }
        
        return new ConflictInfo(false, null);
    }
    
    /**
     * Kiểm tra conflict trong reasons
     */
    private boolean hasConflictInReasons(List<String> reasons, String normalizedSize, String availableSizes) {
        if (reasons == null) return false;
        
        List<String> availableSizeList = parseAvailableSizes(availableSizes);
        
        for (String reason : reasons) {
            if (reason == null) continue;
            
            String reasonLower = reason.toLowerCase();
            if (reasonLower.matches(".*(" + CONFLICT_KEYWORDS + ").*")) {
                // Kiểm tra xem size có thực sự có sẵn không
                if (reasonLower.contains("chỉ có") || reasonLower.contains("có sizes")) {
                    String extractedSize = extractSizeFromReason(java.util.Collections.singletonList(reason), availableSizeList);
                    if (extractedSize != null && availableSizeList.contains(extractedSize.toUpperCase())) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Kiểm tra conflict trong text (productFitComment hoặc closingQuestion)
     */
    private boolean hasConflictInText(String text) {
        if (StringUtils.isBlank(text)) return false;
        return text.toLowerCase().matches(".*(" + CONFLICT_KEYWORDS + ").*");
    }
    
    /**
     * Kiểm tra conflict trong closingQuestion
     */
    private boolean hasConflictInClosingQuestion(String closingQuestion) {
        if (StringUtils.isBlank(closingQuestion)) return false;
        String questionLower = closingQuestion.toLowerCase();
        return questionLower.contains("liên hệ shop") && 
               (questionLower.contains("không có size") || questionLower.contains("chọn sản phẩm khác"));
    }
    
    /**
     * Sửa conflict response
     */
    private SizeAdviceResponse fixConflictResponse(
            SizeAdviceResponse response, SizeAdviceMessage message, 
            String normalizedSize, SizeAdviceRequest request) {
        
        List<String> fixedReasons = buildFixedReasons(normalizedSize, request);
        String fixedProductFitComment = buildFixedProductFitComment(message.getProductFitComment());
        String fixedClosingQuestion = buildFixedClosingQuestion(message.getClosingQuestion());
        String fixedRecommendedSize = isNotAvailable(message.getRecommendedSize()) ? normalizedSize : message.getRecommendedSize();
        
        SizeAdviceMessage fixedMessage = SizeAdviceMessage.builder()
            .recommendedSize(fixedRecommendedSize)
            .backupSize(StringUtils.isNotBlank(message.getBackupSize()) ? message.getBackupSize() : null)
            .reason(fixedReasons)
            .productFitComment(fixedProductFitComment)
            .closingQuestion(fixedClosingQuestion)
            .build();
        
        log.info("Fixed conflicting response: recommendedSize='{}' is available, updated reason to be consistent", normalizedSize);
        return rebuildResponse(response, fixedMessage);
    }
    
    /**
     * Tạo reasons đã sửa
     */
    private List<String> buildFixedReasons(String normalizedSize, SizeAdviceRequest request) {
        List<String> reasons = new ArrayList<>();
        reasons.add("Dựa vào bảng đo sản phẩm, size " + normalizedSize + " phù hợp với thông số của bạn");
        if (request != null) {
            if (request.getHeight() != null) {
                reasons.add("Chiều cao " + request.getHeight() + "cm phù hợp với độ dài áo size " + normalizedSize);
            }
            if (request.getWeight() != null) {
                reasons.add("Cân nặng " + request.getWeight() + "kg phù hợp với vòng ngực size " + normalizedSize);
            }
        }
        return reasons;
    }
    
    /**
     * Tạo productFitComment đã sửa
     */
    private String buildFixedProductFitComment(String originalComment) {
        if (StringUtils.isBlank(originalComment) || hasConflictInText(originalComment)) {
            return "Form vừa, mặc vừa vặn";
        }
        return originalComment;
    }
    
    /**
     * Tạo closingQuestion đã sửa
     */
    private String buildFixedClosingQuestion(String originalQuestion) {
        if (StringUtils.isBlank(originalQuestion) || hasConflictInClosingQuestion(originalQuestion)) {
            return "Bạn có thể thử size này";
        }
        return originalQuestion;
    }
    
    /**
     * Rebuild response với message mới
     */
    private SizeAdviceResponse rebuildResponse(SizeAdviceResponse response, SizeAdviceMessage message) {
        SizeAdviceResponse.SizeAdviceData data = SizeAdviceResponse.SizeAdviceData.builder()
            .productId(response.getData().getProductId())
            .message(message)
            .build();
        
        return SizeAdviceResponse.builder().data(data).build();
    }
    
    /**
     * Extract size từ reason nếu có đề cập
     * Ví dụ: "Dựa vào bảng đo, bạn cần size M" -> "M"
     * "Nhưng sản phẩm chỉ có sizes S, M" -> "M" (nếu M có trong availableSizeList)
     */
    private String extractSizeFromReason(List<String> reasons, List<String> availableSizeList) {
        if (reasons == null || reasons.isEmpty() || availableSizeList == null || availableSizeList.isEmpty()) {
            return null;
        }
        
        // Tìm size được đề cập trong reason
        for (String reason : reasons) {
            if (reason == null) continue;
            
            // Tìm pattern "size X" hoặc "sizes X, Y"
            String reasonLower = reason.toLowerCase();
            
            // Pattern 1: "bạn cần size M" hoặc "size M phù hợp"
            Matcher matcher1 = SIZE_PATTERN.matcher(reason);
            while (matcher1.find()) {
                String foundSize = normalizeSize(matcher1.group(1));
                if (StringUtils.isNotBlank(foundSize) && availableSizeList.contains(foundSize.toUpperCase())) {
                    return foundSize;
                }
            }
            
            // Pattern 2: "chỉ có sizes S, M" - tìm size cuối cùng (thường là size phù hợp)
            if (reasonLower.contains("chỉ có") || reasonLower.contains("có sizes")) {
                Matcher matcher2 = SIZES_LIST_PATTERN.matcher(reason);
                if (matcher2.find()) {
                    String sizesStr = matcher2.group(1);
                    String[] sizes = sizesStr.split(",");
                    // Lấy size cuối cùng (thường là size lớn nhất/phù hợp nhất)
                    for (int i = sizes.length - 1; i >= 0; i--) {
                        String foundSize = normalizeSize(sizes[i]);
                        if (StringUtils.isNotBlank(foundSize) && availableSizeList.contains(foundSize.toUpperCase())) {
                            return foundSize;
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Normalize size string - loại bỏ prefix "Size ", "size ", trim
     * Ví dụ: "Size M" -> "M", "size L " -> "L", "M" -> "M", "M " -> "M"
     */
    private String normalizeSize(String size) {
        if (StringUtils.isBlank(size)) {
            return "";
        }
        
        String normalized = size.trim();
        
        // Loại bỏ prefix "Size " hoặc "size " (case insensitive)
        String lowerNormalized = normalized.toLowerCase();
        if (lowerNormalized.startsWith("size ")) {
            normalized = normalized.substring(5).trim();
        }
        
        // Loại bỏ các ký tự đặc biệt không cần thiết (nhưng giữ lại chữ và số)
        // Chỉ loại bỏ ký tự đặc biệt ở đầu/cuối, giữ lại nội dung size
        normalized = normalized.replaceAll("^[^A-Za-z0-9]+|[^A-Za-z0-9]+$", "").trim();
        
        // Nếu sau khi normalize vẫn còn ký tự đặc biệt ở giữa, chỉ lấy phần chữ/số đầu tiên
        // Ví dụ: "M-L" -> "M", "Size M (Large)" -> "M"
        if (normalized.matches(".*[^A-Za-z0-9].*")) {
            String[] parts = normalized.split("[^A-Za-z0-9]+");
            if (parts.length > 0 && StringUtils.isNotBlank(parts[0])) {
                normalized = parts[0].trim();
            }
        }
        
        return normalized;
    }
    
    private SizeAdviceResponse parseAIResponse(String aiResponse, Long productId, Product product) throws Exception {
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
    
    @Override
    public String chat(SizeAdviceChatRequest request) throws Exception {
        try {
            log.info("Processing chat request for productId: {}, message: {}", 
                request.getProductId(), request.getMessage());
            
            Product product = productRepository.findFirstByIdAndDeletedIsFalse(request.getProductId());
            if (product == null) {
                throw new IllegalArgumentException("Product not found: " + request.getProductId());
            }
            
            // Parse thông tin mới từ message và cập nhật context
            SizeAdviceChatRequest updatedRequest = parseAndUpdateContext(request);
            
            // Xây dựng system prompt cho chat tiếp theo với thông tin sản phẩm
            String systemPrompt = buildChatSystemPrompt(product, updatedRequest);
            
            // Xây dựng conversation history
            List<Map<String, String>> conversationHistory = buildConversationHistory(updatedRequest);
            
            log.info("========== CHAT SYSTEM PROMPT ==========");
            log.info(systemPrompt);
            log.info("========== CONVERSATION HISTORY ==========");
            log.info("History size: {}", conversationHistory.size());
            log.info("========== END CHAT PROMPT ==========");
            
            // Gọi OpenAI với conversation history
            String aiResponse = openAIService.callGPTWithHistory(systemPrompt, conversationHistory);
            
            log.info("Chat response generated successfully for productId: {}", request.getProductId());
            return aiResponse;
            
        } catch (Exception e) {
            log.error("Error processing chat request", e);
            throw e;
        }
    }
    
    /**
     * Parse thông tin mới từ message và cập nhật context
     */
    private SizeAdviceChatRequest parseAndUpdateContext(SizeAdviceChatRequest request) {
        String message = request.getMessage();
        if (StringUtils.isBlank(message)) {
            return request;
        }
        
        // Tạo request mới với context được cập nhật
        SizeAdviceChatRequest updated = new SizeAdviceChatRequest();
        updated.setProductId(request.getProductId());
        updated.setMessage(message);
        updated.setConversationHistory(request.getConversationHistory());
        
        // Copy context cũ
        updated.setGender(request.getGender());
        updated.setHeight(request.getHeight());
        updated.setWeight(request.getWeight());
        updated.setFitPreference(request.getFitPreference());
        updated.setNote(request.getNote());
        
        // Parse height từ message (ví dụ: "160cm", "160 cm", "cao 160")
        Pattern heightPattern = Pattern.compile("(?:cao|chiều cao|height)\\s*(?:là|:|)?\\s*(\\d+)\\s*(?:cm|centimet)?", Pattern.CASE_INSENSITIVE);
        Matcher heightMatcher = heightPattern.matcher(message);
        if (heightMatcher.find()) {
            try {
                int height = Integer.parseInt(heightMatcher.group(1));
                if (height > 0 && height < 300) {
                    updated.setHeight(height);
                    log.debug("Parsed height from message: {}cm", height);
                }
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        
        // Parse weight từ message (ví dụ: "47kg", "47 kg", "nặng 47")
        Pattern weightPattern = Pattern.compile("(?:nặng|cân nặng|weight)\\s*(?:là|:|)?\\s*(\\d+)\\s*(?:kg|kilogram)?", Pattern.CASE_INSENSITIVE);
        Matcher weightMatcher = weightPattern.matcher(message);
        if (weightMatcher.find()) {
            try {
                int weight = Integer.parseInt(weightMatcher.group(1));
                if (weight > 0 && weight < 300) {
                    updated.setWeight(weight);
                    log.debug("Parsed weight from message: {}kg", weight);
                }
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        
        // Parse gender từ message
        String messageLower = message.toLowerCase();
        if (messageLower.contains("nam") || messageLower.contains("male") || messageLower.contains("anh")) {
            updated.setGender("male");
            log.debug("Parsed gender from message: male");
        } else if (messageLower.contains("nữ") || messageLower.contains("female") || messageLower.contains("chị") || messageLower.contains("em")) {
            updated.setGender("female");
            log.debug("Parsed gender from message: female");
        }
        
        // Parse fit preference từ message
        if (messageLower.contains("rộng") || messageLower.contains("oversize") || messageLower.contains("loose")) {
            updated.setFitPreference("rộng");
            log.debug("Parsed fitPreference from message: rộng");
        } else if (messageLower.contains("ôm") || messageLower.contains("slim") || messageLower.contains("fit")) {
            updated.setFitPreference("ôm");
            log.debug("Parsed fitPreference from message: ôm");
        } else if (messageLower.contains("vừa") || messageLower.contains("regular") || messageLower.contains("normal")) {
            updated.setFitPreference("vừa");
            log.debug("Parsed fitPreference from message: vừa");
        }
        
        return updated;
    }
    
    /**
     * Xây dựng conversation history từ request
     */
    private List<Map<String, String>> buildConversationHistory(SizeAdviceChatRequest request) {
        List<Map<String, String>> history = new ArrayList<>();
        
        // Nếu có conversation history từ frontend, sử dụng nó
        if (request.getConversationHistory() != null && !request.getConversationHistory().isEmpty()) {
            history.addAll(request.getConversationHistory());
        } else {
            // Nếu không có history, tạo message đầu tiên với context
            Map<String, String> initialMessage = new HashMap<>();
            initialMessage.put("role", "user");
            
            StringBuilder initialContent = new StringBuilder();
            initialContent.append("Tôi muốn tư vấn size cho sản phẩm này. ");
            if (request.getHeight() != null) {
                initialContent.append("Chiều cao: ").append(request.getHeight()).append("cm. ");
            }
            if (request.getWeight() != null) {
                initialContent.append("Cân nặng: ").append(request.getWeight()).append("kg. ");
            }
            if (StringUtils.isNotBlank(request.getGender())) {
                String genderText = "male".equals(request.getGender()) ? "Nam" : 
                                   "female".equals(request.getGender()) ? "Nữ" : "Unisex";
                initialContent.append("Giới tính: ").append(genderText).append(". ");
            }
            if (StringUtils.isNotBlank(request.getFitPreference())) {
                initialContent.append("Phong cách mặc: ").append(request.getFitPreference()).append(". ");
            }
            
            initialMessage.put("content", initialContent.toString());
            history.add(initialMessage);
        }
        
        // Thêm message hiện tại
        Map<String, String> currentMessage = new HashMap<>();
        currentMessage.put("role", "user");
        currentMessage.put("content", request.getMessage());
        history.add(currentMessage);
        
        return history;
    }
    
    /**
     * Xây dựng system prompt cho chat tiếp theo với thông tin sản phẩm
     */
    private String buildChatSystemPrompt(Product product, SizeAdviceChatRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Bạn là nhân viên tư vấn size quần áo chuyên nghiệp cho website thương mại điện tử.\n");
        prompt.append("Bạn đang tiếp tục cuộc trò chuyện với khách hàng sau khi đã tư vấn size ban đầu.\n\n");
        
        // Thông tin sản phẩm
        prompt.append("THÔNG TIN SẢN PHẨM:\n");
        prompt.append("- Tên sản phẩm: ").append(product.getName()).append("\n");
        prompt.append("- Sizes có sẵn: ").append(product.getSizes() != null ? product.getSizes() : "Chưa có thông tin").append("\n");
        
        // Bảng đo sản phẩm
        if (StringUtils.isNotBlank(product.getDescriptionSize())) {
            prompt.append("- Bảng đo sản phẩm:\n");
            prompt.append(cleanHtmlTags(product.getDescriptionSize())).append("\n");
        }
        
        // Form sản phẩm
        String productForm = extractProductForm(product);
        if (!"chưa rõ".equals(productForm)) {
            prompt.append("- Form sản phẩm: ").append(productForm).append("\n");
        }
        
        // Thông tin khách hàng hiện tại (có thể được cập nhật trong conversation)
        prompt.append("\nTHÔNG TIN KHÁCH HÀNG HIỆN TẠI:\n");
        if (StringUtils.isNotBlank(request.getGender())) {
            String genderText = "male".equals(request.getGender()) ? "Nam" : 
                               "female".equals(request.getGender()) ? "Nữ" : "Unisex";
            prompt.append("- Giới tính: ").append(genderText).append("\n");
        }
        if (request.getHeight() != null) {
            prompt.append("- Chiều cao: ").append(request.getHeight()).append("cm\n");
        }
        if (request.getWeight() != null) {
            prompt.append("- Cân nặng: ").append(request.getWeight()).append("kg\n");
        }
        if (StringUtils.isNotBlank(request.getFitPreference())) {
            prompt.append("- Phong cách mặc: ").append(request.getFitPreference()).append("\n");
        }
        if (StringUtils.isNotBlank(request.getNote())) {
            prompt.append("- Mô tả thêm: ").append(request.getNote()).append("\n");
        }
        
        prompt.append("\nNguyên tắc:\n");
        prompt.append("1. Trả lời câu hỏi của khách hàng một cách chuyên nghiệp, thân thiện và tận tâm.\n");
        prompt.append("2. QUAN TRỌNG: Nếu khách hàng cung cấp thông tin mới (chiều cao, cân nặng, giới tính, phong cách mặc) trong câu hỏi, hãy CẬP NHẬT và sử dụng thông tin mới nhất để tư vấn.\n");
        prompt.append("3. Sử dụng thông tin về sản phẩm và thông số khách hàng (mới nhất) để trả lời chính xác.\n");
        prompt.append("4. Nếu khách hỏi về size, hãy tham khảo lại bảng đo sản phẩm và thông số của khách (mới nhất).\n");
        prompt.append("5. Nếu khách hỏi về form sản phẩm, hãy giải thích rõ ràng.\n");
        prompt.append("6. Nếu khách hỏi về cách chọn size, hãy đưa ra lời khuyên cụ thể dựa trên thông tin mới nhất.\n");
        prompt.append("7. Trả lời ngắn gọn, dễ hiểu, không quá dài dòng.\n");
        prompt.append("8. Luôn thể hiện sự chuyên nghiệp và sẵn sàng hỗ trợ.\n");
        prompt.append("9. Trả lời bằng tiếng Việt, tự nhiên như đang nói chuyện với khách hàng.\n");
        prompt.append("10. Khi khách cung cấp thông tin mới, hãy xác nhận lại thông tin đó trước khi tư vấn.\n");
        
        return prompt.toString();
    }
}
