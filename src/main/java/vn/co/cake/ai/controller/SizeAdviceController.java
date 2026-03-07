package vn.co.cake.ai.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.co.cake.ai.dto.SizeAdviceRequest;
import vn.co.cake.ai.dto.SizeAdviceResponse;
import vn.co.cake.ai.dto.SizeAdviceChatRequest;
import vn.co.cake.ai.service.SizeAdviceService;
import vn.co.cake.common.BaseConst;
import vn.co.cake.entity.Account;
import vn.co.cake.repository.AccountRepository;
import vn.co.cake.security.user.UserLoginInfo;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/size-advice")
public class SizeAdviceController {
    
    private final SizeAdviceService sizeAdviceService;
    private final AccountRepository accountRepository;
    
    public SizeAdviceController(SizeAdviceService sizeAdviceService,
                               AccountRepository accountRepository) {
        this.sizeAdviceService = sizeAdviceService;
        this.accountRepository = accountRepository;
    }
    
    @PostMapping
    public ResponseEntity<SizeAdviceResponse> getSizeAdvice(
            @RequestBody SizeAdviceRequest request,
            HttpSession session) {
        try {
            String validationError = validateRequest(request);
            if (validationError != null) {
                log.error("Validation error: {}", validationError);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            
            // Lưu height và weight nếu user đang login
            saveUserHeightWeight(request, session);
            
            SizeAdviceResponse response = sizeAdviceService.getSizeAdvice(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error processing size advice request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Lưu height và weight của user nếu đang login
     */
    private void saveUserHeightWeight(SizeAdviceRequest request, HttpSession session) {
        try {
            UserLoginInfo loginInfo = (UserLoginInfo) session.getAttribute(BaseConst.USER_SESSION);
            if (loginInfo != null && loginInfo.getId() != null) {
                Account account = accountRepository.findFirstByIdAndDeletedIsFalse(loginInfo.getId());
                if (account != null) {
                    boolean needUpdate = false;
                    
                    // Cập nhật height nếu có
                    if (request.getHeight() != null && request.getHeight() > 0) {
                        account.setHeight(request.getHeight());
                        needUpdate = true;
                    }
                    
                    // Cập nhật weight nếu có
                    if (request.getWeight() != null && request.getWeight() > 0) {
                        account.setWeight(request.getWeight());
                        needUpdate = true;
                    }
                    
                    if (needUpdate) {
                        accountRepository.save(account);
                        log.info("Saved height: {}cm, weight: {}kg for accountId: {}", 
                            account.getHeight(), account.getWeight(), account.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to save user height/weight: {}", e.getMessage());
            // Không throw exception để không ảnh hưởng đến flow chính
        }
    }
    
    @PostMapping("/save-profile")
    public ResponseEntity<Map<String, String>> saveUserProfile(
            @RequestBody Map<String, Integer> profileData,
            HttpSession session) {
        Map<String, String> response = new HashMap<>();
        try {
            UserLoginInfo loginInfo = (UserLoginInfo) session.getAttribute(BaseConst.USER_SESSION);
            if (loginInfo == null || loginInfo.getId() == null) {
                response.put("success", "false");
                response.put("message", "User not logged in");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            Account account = accountRepository.findFirstByIdAndDeletedIsFalse(loginInfo.getId());
            if (account == null) {
                response.put("success", "false");
                response.put("message", "Account not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            if (profileData.containsKey("height") && profileData.get("height") != null) {
                account.setHeight(profileData.get("height"));
            }
            if (profileData.containsKey("weight") && profileData.get("weight") != null) {
                account.setWeight(profileData.get("weight"));
            }
            
            accountRepository.save(account);
            response.put("success", "true");
            response.put("message", "Profile updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error saving user profile", e);
            response.put("success", "false");
            response.put("message", "Error saving profile: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(
            @RequestBody Map<String, Object> request,
            HttpSession session) {
        Map<String, String> response = new HashMap<>();
        try {
            String message = (String) request.get("message");
            Long productId = request.get("productId") != null ? 
                Long.parseLong(request.get("productId").toString()) : null;
            
            if (StringUtils.isBlank(message)) {
                response.put("message", "Vui lòng nhập câu hỏi của bạn.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            if (productId == null) {
                response.put("message", "Không tìm thấy thông tin sản phẩm.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Tạo chat request từ map
            SizeAdviceChatRequest chatRequest = new SizeAdviceChatRequest();
            chatRequest.setProductId(productId);
            chatRequest.setMessage(message);
            
            // Lấy context từ request (nếu có)
            if (request.containsKey("gender")) {
                chatRequest.setGender((String) request.get("gender"));
            }
            if (request.containsKey("height")) {
                Object heightObj = request.get("height");
                if (heightObj != null) {
                    chatRequest.setHeight(Integer.parseInt(heightObj.toString()));
                }
            }
            if (request.containsKey("weight")) {
                Object weightObj = request.get("weight");
                if (weightObj != null) {
                    chatRequest.setWeight(Integer.parseInt(weightObj.toString()));
                }
            }
            if (request.containsKey("fitPreference")) {
                chatRequest.setFitPreference((String) request.get("fitPreference"));
            }
            if (request.containsKey("note")) {
                chatRequest.setNote((String) request.get("note"));
            }
            
            // Lấy conversation history từ request (nếu có)
            if (request.containsKey("conversationHistory")) {
                @SuppressWarnings("unchecked")
                List<Map<String, String>> history = (List<Map<String, String>>) request.get("conversationHistory");
                chatRequest.setConversationHistory(history);
            }
            
            // Gọi service để xử lý chat
            String aiResponse = sizeAdviceService.chat(chatRequest);
            response.put("message", aiResponse);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid chat request: {}", e.getMessage());
            response.put("message", "Thông tin không hợp lệ: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("Error processing chat request", e);
            response.put("message", "Xin lỗi, có lỗi xảy ra. Vui lòng thử lại sau.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    private String validateRequest(SizeAdviceRequest request) {
        if (request == null) {
            return "Request body is required";
        }
        if (request.getProductId() == null) {
            return "productId is required";
        }
        if (StringUtils.isBlank(request.getGender())) {
            return "gender is required";
        }
        if (request.getHeight() == null || request.getHeight() <= 0) {
            return "height is required and must be greater than 0";
        }
        if (request.getWeight() == null || request.getWeight() <= 0) {
            return "weight is required and must be greater than 0";
        }
        return null;
    }
}
