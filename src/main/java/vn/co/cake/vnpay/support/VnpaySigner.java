package vn.co.cake.vnpay.support;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;

import vn.co.cake.vnpay.VnpayConstants;

/**
 * Ký / xác thực checksum VNPAY (HMAC-SHA512) theo code mẫu Java chính thức:
 * tham số sắp tăng dần theo tên, bỏ giá trị rỗng, URL-encode US_ASCII, nối {@code key=value&...}.
 * Chuỗi được ký phải trùng khớp query string gửi đi.
 */
public final class VnpaySigner {

    private static final String HMAC_SHA512 = "HmacSHA512";

    private VnpaySigner() {
    }

    /** Query string đã encode, dùng cho cả hash data lẫn URL redirect. */
    public static String buildQuery(Map<String, String> params) {
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> entry : new TreeMap<>(params).entrySet()) {
            String key = entry.getKey();
            if (VnpayConstants.PARAM_SECURE_HASH.equals(key) || VnpayConstants.PARAM_SECURE_HASH_TYPE.equals(key)) {
                continue;
            }
            if (StringUtils.isEmpty(entry.getValue())) {
                continue;
            }
            joiner.add(encode(key) + "=" + encode(entry.getValue()));
        }
        return joiner.toString();
    }

    public static String sign(Map<String, String> params, String hashSecret) {
        return hmacSha512(hashSecret, buildQuery(params));
    }

    /** Xác thực vnp_SecureHash của IPN / Return URL (tham số đã được servlet decode). */
    public static boolean verify(Map<String, String> params, String hashSecret) {
        String received = params.get(VnpayConstants.PARAM_SECURE_HASH);
        if (StringUtils.isAnyBlank(received, hashSecret)) {
            return false;
        }
        String expected = sign(params, hashSecret);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                received.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }

    /** Checksum dạng {@code a|b|c} của API querydr / refund (không sort, không encode). */
    public static String signPipe(String hashSecret, String... values) {
        StringJoiner joiner = new StringJoiner("|");
        for (String value : values) {
            joiner.add(StringUtils.defaultString(value));
        }
        return hmacSha512(hashSecret, joiner.toString());
    }

    public static String hmacSha512(String key, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA512);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA512));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                hex.append(String.format(Locale.ROOT, "%02x", b & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Không ký được checksum VNPAY", ex);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }
}
