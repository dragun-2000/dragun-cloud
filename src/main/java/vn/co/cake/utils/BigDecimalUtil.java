package vn.co.cake.utils;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.text.DecimalFormat;

@Slf4j
public class BigDecimalUtil {

    public static boolean priceValid(String value) {
        try {
            long parseLong = Long.parseLong(value);
            if (parseLong < 0 || parseLong > 999999999) {
                log.error("BigDecimalUtil priceValid is not valid = {}", value);
                return false;      
            }
        } catch (Exception e) {
            log.error("BigDecimalUtil priceValid error with value = {}", value);      
            return false;
        }
        return true;
    }
    
    public static String formatDouble(String input) {
        double value = Double.parseDouble(input);
        if (value == (long) value) {
            return String.valueOf((long) value); 
        } else {
            return String.valueOf(value);
        }
    }
    
    public static boolean discountValid(String value) {
        try {
            long parseLong = Long.parseLong(value);
            if (parseLong < 0 || parseLong > 99) {
                log.error("BigDecimalUtil discountValid error with value = {}", value);
                return false;      
            }
        } catch (Exception e) {
            log.error("BigDecimalUtil discountValid error with value = {}", value);      
            return false;
        }
        return true;
    } 
    
    public static boolean discountValid(int value) {
        try {
            if (value < 0 || value > 99) {
                log.error("BigDecimalUtil discountValid error with value = {}", value);
                return false;      
            }
        } catch (Exception e) {
            log.error("BigDecimalUtil discountValid error with value = {}", value);      
            return false;
        }
        return true;
    } 
    
    public static boolean shippingFeeValid(int value) {
        try {
            if (value < 0 || value > 100000) {
                log.error("BigDecimalUtil discountValid error with value = {}", value);
                return false;      
            }
        } catch (Exception e) {
            log.error("BigDecimalUtil discountValid error with value = {}", value);      
            return false;
        }
        return true;
    } 
    
    public static boolean stockQuantityValid(Long value) {
        try {
            if (value == null || value < 0 || value > 99999) {
                log.error("BigDecimalUtil stockQuantityValid error with value = {}", value);
                return false;      
            }
        } catch (Exception e) {
            log.error("BigDecimalUtil stockQuantityValid error with value = {}", value);      
            return false;
        }
        return true;
    }
    
    public static String formatMoney(BigDecimal value) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(value);
    }
    
    public static String formatMoney(int value) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(value);
    }

    public static String formatMoney(String value) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(Long.parseLong(value));
    }
}
