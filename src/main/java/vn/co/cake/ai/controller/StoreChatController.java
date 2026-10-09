package vn.co.cake.ai.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.co.cake.ai.dto.storechat.StoreChatRequest;
import vn.co.cake.ai.dto.storechat.StoreChatResponse;
import vn.co.cake.ai.service.StoreChatService;

import javax.servlet.http.HttpSession;

@Slf4j
@RestController
@RequestMapping("/api/store-chat")
public class StoreChatController {

    private final StoreChatService storeChatService;

    public StoreChatController(StoreChatService storeChatService) {
        this.storeChatService = storeChatService;
    }

    @PostMapping
    public ResponseEntity<StoreChatResponse> chat(@RequestBody StoreChatRequest request, HttpSession session) {
        try {
            if (request == null || StringUtils.isBlank(request.getMessage())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(StoreChatResponse.builder()
                                .message("Vui lòng nhập câu hỏi của bạn.")
                                .build());
            }
            return ResponseEntity.ok(storeChatService.chat(request, session));
        } catch (Exception e) {
            log.error("Store chat error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(StoreChatResponse.builder()
                            .message("Xin lỗi, tư vấn đang bận. Bạn thử lại sau giúp mình nhé.")
                            .build());
        }
    }
}
