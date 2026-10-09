package vn.co.cake.ai.service;

import vn.co.cake.ai.dto.storechat.StoreChatRequest;
import vn.co.cake.ai.dto.storechat.StoreChatResponse;

import javax.servlet.http.HttpSession;

public interface StoreChatService {
    StoreChatResponse chat(StoreChatRequest request, HttpSession session);
}
