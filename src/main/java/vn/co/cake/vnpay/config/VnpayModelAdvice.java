package vn.co.cake.vnpay.config;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import vn.co.cake.controller.store.CartController;

/** Đưa cờ {@code vnpayEnabled} vào trang thanh toán mà không phải sửa CartController. */
@ControllerAdvice(assignableTypes = CartController.class)
public class VnpayModelAdvice {

    private final VnpayProperties properties;

    public VnpayModelAdvice(VnpayProperties properties) {
        this.properties = properties;
    }

    @ModelAttribute
    public void addVnpayFlag(Model model) {
        model.addAttribute("vnpayEnabled", properties.isUsable());
    }
}
