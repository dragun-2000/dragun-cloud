package vn.co.cake.payment.util;

import java.text.Normalizer;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import vn.co.cake.payment.PaymentConstants;

/**
 * Normalizes transfer content for VietQR (max 23 chars, no accents, no special chars).
 */
public final class VietQrContentNormalizer {

    private VietQrContentNormalizer() {
    }

    public static String normalizeOrderContent(String orderCode) {
        String raw = String.format(Locale.ROOT, "DH%s", orderCode);
        return normalize(raw);
    }

    public static String normalize(String input) {
        if (StringUtils.isEmpty(input)) {
            return "THANHTOAN";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutAccents = normalized.replaceAll("\\p{M}", "");
        String alphanumeric = withoutAccents.replaceAll("[^A-Za-z0-9]", "");
        if (StringUtils.isEmpty(alphanumeric)) {
            alphanumeric = "THANHTOAN";
        }
        if (alphanumeric.length() > PaymentConstants.VIETQR_CONTENT_MAX_LENGTH) {
            return alphanumeric.substring(0, PaymentConstants.VIETQR_CONTENT_MAX_LENGTH);
        }
        return alphanumeric.toUpperCase(Locale.ROOT);
    }
}
