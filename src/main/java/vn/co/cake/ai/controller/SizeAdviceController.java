package vn.co.cake.ai.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.co.cake.ai.dto.SizeAdviceRequest;
import vn.co.cake.ai.dto.SizeAdviceResponse;
import vn.co.cake.ai.service.SizeAdviceService;

@Slf4j
@RestController
@RequestMapping("/api/size-advice")
public class SizeAdviceController {
    
    private final SizeAdviceService sizeAdviceService;
    
    public SizeAdviceController(SizeAdviceService sizeAdviceService) {
        this.sizeAdviceService = sizeAdviceService;
    }
    
    @PostMapping
    public ResponseEntity<SizeAdviceResponse> getSizeAdvice(
            @RequestBody SizeAdviceRequest request) {
        try {
            String validationError = validateRequest(request);
            if (validationError != null) {
                log.error("Validation error: {}", validationError);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            
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
