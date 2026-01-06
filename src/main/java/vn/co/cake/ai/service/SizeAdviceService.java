package vn.co.cake.ai.service;

import vn.co.cake.ai.dto.SizeAdviceRequest;
import vn.co.cake.ai.dto.SizeAdviceResponse;

public interface SizeAdviceService {
    SizeAdviceResponse getSizeAdvice(SizeAdviceRequest request);
}
