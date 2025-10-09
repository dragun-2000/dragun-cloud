package vn.co.cake.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneValidator {

    // Hàm validate số điện thoại hợp lệ (định dạng quốc tế)
    public static boolean validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }

        // Biểu thức chính quy để kiểm tra số điện thoại theo định dạng quốc tế
        String regex = "^(\\+\\d{1,4})?\\d{10,11}$";  // +84xxxxxxxxx cho Việt Nam, xxxxxxxx cho các quốc gia khác

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(phoneNumber);

        return matcher.matches();
    }
}
