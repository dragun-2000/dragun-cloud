package vn.co.cake.vnpay.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import vn.co.cake.vnpay.VnpayConstants;

/** Định dạng dữ liệu theo quy ước VNPAY: ngày GMT+7, tiền × 100, OrderInfo không dấu. */
public final class VnpayFormats {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(VnpayConstants.DATE_PATTERN).withZone(VnpayConstants.ZONE_VN);
    private static final int ORDER_INFO_MAX_LENGTH = 255;

    private VnpayFormats() {
    }

    public static String formatDate(Date date) {
        return DATE_FORMATTER.format(date.toInstant());
    }

    public static Date parseDate(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        Instant instant = LocalDateTime.parse(value, DateTimeFormatter.ofPattern(VnpayConstants.DATE_PATTERN))
                .atZone(VnpayConstants.ZONE_VN).toInstant();
        return Date.from(instant);
    }

    /** VND → giá trị vnp_Amount (không thập phân). */
    public static String toVnpAmount(BigDecimal vnd) {
        return vnd.setScale(0, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(VnpayConstants.AMOUNT_MULTIPLIER))
                .toPlainString();
    }

    /** vnp_Amount → VND; null nếu không phải số. */
    public static BigDecimal fromVnpAmount(String vnpAmount) {
        if (StringUtils.isBlank(vnpAmount) || !StringUtils.isNumeric(vnpAmount)) {
            return null;
        }
        return new BigDecimal(vnpAmount).divide(BigDecimal.valueOf(VnpayConstants.AMOUNT_MULTIPLIER), 2,
                RoundingMode.HALF_UP);
    }

    /** So sánh số tiền VND đã lưu với vnp_Amount nhận về (theo số nguyên VND). */
    public static boolean amountMatches(BigDecimal expectedVnd, String vnpAmount) {
        BigDecimal received = fromVnpAmount(vnpAmount);
        return expectedVnd != null && received != null
                && expectedVnd.setScale(0, RoundingMode.HALF_UP).compareTo(received) == 0;
    }

    /** vnp_OrderInfo: tiếng Việt không dấu, chỉ chữ/số/khoảng trắng. */
    public static String toOrderInfo(String text) {
        String noAccent = Normalizer.normalize(StringUtils.defaultString(text), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        String cleaned = noAccent.replaceAll("[^A-Za-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        return StringUtils.left(cleaned, ORDER_INFO_MAX_LENGTH);
    }
}
