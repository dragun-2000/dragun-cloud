package vn.co.cake.payment.support;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import vn.co.cake.payment.dto.VietQrGenerateResponse;

/**
 * Build URL trang thanh toán VietQR Pro: https://pro.vietqr.vn/test/qr-generated?token=...
 */
public final class VietQrPaymentUrlResolver {

    private static final Pattern TOKEN_QUERY_PARAM = Pattern.compile("[?&]token=([^&]+)");

    private VietQrPaymentUrlResolver() {
    }

    public static String resolve(VietQrGenerateResponse qr, String paymentPageBaseUrl) {
        if (qr == null) {
            return null;
        }
        if (StringUtils.isNotBlank(qr.getQrLink()) && isAbsoluteHttpUrl(qr.getQrLink())) {
            return qr.getQrLink().trim();
        }
        String token = extractToken(qr);
        if (StringUtils.isNotBlank(token) && StringUtils.isNotBlank(paymentPageBaseUrl)) {
            return appendToken(paymentPageBaseUrl.trim(), token);
        }
        return StringUtils.isNotBlank(qr.getQrLink()) ? qr.getQrLink().trim() : null;
    }

    private static boolean isAbsoluteHttpUrl(String url) {
        String lower = url.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private static String extractToken(VietQrGenerateResponse qr) {
        if (StringUtils.isNotBlank(qr.getTransactionRefId())) {
            return qr.getTransactionRefId().trim();
        }
        if (StringUtils.isNotBlank(qr.getQrLink())) {
            Matcher matcher = TOKEN_QUERY_PARAM.matcher(qr.getQrLink());
            if (matcher.find()) {
                return decode(matcher.group(1));
            }
            String link = qr.getQrLink().trim();
            if (!link.contains("/") && !link.contains("?")) {
                return link;
            }
        }
        return null;
    }

    private static String appendToken(String baseUrl, String token) {
        if (baseUrl.endsWith("=") || baseUrl.endsWith("/")) {
            return baseUrl + token;
        }
        if (baseUrl.contains("token=")) {
            return baseUrl + token;
        }
        return baseUrl + (baseUrl.contains("?") ? "&token=" : "?token=") + token;
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (Exception ex) {
            return value;
        }
    }
}
