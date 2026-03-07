package vn.co.cake.request;

import lombok.Data;

/**
 * Request DTO cho OTP verification
 */
@Data
public class RegisterOtpRequest {
    
    /**
     * Số điện thoại
     */
    private String phone;
    
    /**
     * Mã OTP 6 số
     */
    private String otpCode;
}
