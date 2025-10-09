package vn.co.cake.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static vn.co.cake.common.StringConst.MAIL_REGEX;
import static vn.co.cake.common.StringConst.PASSWORD_REGEX;

/**
 * StringUtil
 */
@Slf4j
public class StringUtil {
    private static final List<String> IP_HEADERS = Arrays.asList("X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR");

    public static List<String> splitStringByPattern(String sourceString, String pattern){
        return Arrays.asList(sourceString.split(pattern));
    }

    public static boolean checkPasswordValid(String text) {
        Pattern pattern = Pattern.compile(PASSWORD_REGEX);
        Matcher matcher = pattern.matcher(text);
        return matcher.matches();
    }

    public static boolean checkMailValid(String text) {
        Pattern pattern = Pattern.compile(MAIL_REGEX);
        Matcher matcher = pattern.matcher(text);
        return matcher.matches();
    }


    public static List<String> convertStringToArray(String text) {
        List<String> response = new ArrayList<>();
        if (StringUtils.isEmpty(text))
            return response;
        try {
            response = Arrays.stream(text.split(","))
                    .distinct()
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error(e.getMessage());    }
        return response;
    }

    public static String convertArrayToString(List<String> array) {
        String response = StringUtils.EMPTY;
        if (CollectionUtils.isEmpty(array)) return response;
        try {
            response = String.join(",", array);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return response;
    }

    public static String getClientIpAddr(HttpServletRequest request) {
        return IP_HEADERS.stream()
                .map(request::getHeader)
                .filter(Objects::nonNull)
                .filter(ip -> !ip.isEmpty() && !ip.equalsIgnoreCase("unknown"))
                .findFirst()
                .orElseGet(request::getRemoteAddr);
    }

    public static String formatWithLeadingZeros(BigDecimal number) {
        DecimalFormat decimalFormat = new DecimalFormat("00000"); // 5 ký tự, ví dụ: 00001, 00002, ...
        return decimalFormat.format(number);
    }
}
