package vn.co.cake.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneValidator {

    // Regex cho số điện thoại Việt Nam
    // Hỗ trợ: 0xxxxxxxxx (10 số), +84xxxxxxxxx, 84xxxxxxxxx
    // Đầu số hợp lệ: 032-039, 056-059, 070-079, 081-089, 090-094, 096-099
    private static final String VIETNAM_PHONE_REGEX = "^(0|84|\\+84)(3[2-9]|5[689]|7[06-9]|8[1-689]|9[0-46-9])[0-9]{7}$";
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(VIETNAM_PHONE_REGEX);

    /**
     * Validate số điện thoại Việt Nam
     * Hỗ trợ format: 0xxxxxxxxx, 84xxxxxxxxx, +84xxxxxxxxx
     * 
     * @param phoneNumber Số điện thoại cần validate
     * @return true nếu hợp lệ, false nếu không hợp lệ
     */
    public static boolean validateVietnamPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        // Loại bỏ khoảng trắng và ký tự đặc biệt không cần thiết
        String cleanedPhone = phoneNumber.trim().replaceAll("[\\s\\-\\(\\)]", "");
        
        // Kiểm tra format
        Matcher matcher = PHONE_PATTERN.matcher(cleanedPhone);
        return matcher.matches();
    }

    /**
     * Normalize số điện thoại về format 0xxxxxxxxx (10 số)
     * 
     * @param phoneNumber Số điện thoại cần normalize
     * @return Số điện thoại đã normalize hoặc null nếu không hợp lệ
     */
    public static String normalizeVietnamPhone(String phoneNumber) {
        if (!validateVietnamPhoneNumber(phoneNumber)) {
            return null;
        }
        
        String cleanedPhone = phoneNumber.trim().replaceAll("[\\s\\-\\(\\)]", "");
        
        // Convert về format 0xxxxxxxxx
        if (cleanedPhone.startsWith("+84")) {
            cleanedPhone = "0" + cleanedPhone.substring(3);
        } else if (cleanedPhone.startsWith("84")) {
            cleanedPhone = "0" + cleanedPhone.substring(2);
        }
        
        return cleanedPhone;
    }

    /**
     * Hàm validate số điện thoại hợp lệ (định dạng quốc tế)
     * Giữ lại để backward compatibility
     * 
     * @param phoneNumber Số điện thoại cần validate
     * @return true nếu hợp lệ, false nếu không hợp lệ
     */
    public static boolean validatePhoneNumber(String phoneNumber) {
        return validateVietnamPhoneNumber(phoneNumber);
    }
}
