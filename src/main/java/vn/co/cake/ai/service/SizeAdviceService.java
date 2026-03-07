package vn.co.cake.ai.service;

import vn.co.cake.ai.dto.SizeAdviceRequest;
import vn.co.cake.ai.dto.SizeAdviceResponse;
import vn.co.cake.ai.dto.SizeAdviceChatRequest;

public interface SizeAdviceService {
    SizeAdviceResponse getSizeAdvice(SizeAdviceRequest request);
    String chat(SizeAdviceChatRequest request) throws Exception;
}
