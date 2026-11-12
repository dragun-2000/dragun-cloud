package vn.co.cake.vnpay.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class VnpaySignerTest {

    private static final String SECRET = "SECRETKEY";
    /** Tính độc lập bằng Python hmac/urllib.parse.quote_plus cho bộ tham số bên dưới. */
    private static final String EXPECTED_HASH = "e8b4ee6aa57bfa261fe8cc90a1062a0bbe080a2c29d4272405d93c53bb9a21eb"
            + "b910ad05155424e137b2a5245d33ef61b0838e0f9c8cd2a68c51e138f8ae9ba3";

    private static Map<String, String> sampleParams() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", "TESTCODE");
        params.put("vnp_Amount", "10000000");
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", "DB0001");
        params.put("vnp_OrderInfo", "Thanh toan don hang DB0001");
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", "https://example.test/vnpay/return");
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put("vnp_CreateDate", "20261111100000");
        params.put("vnp_ExpireDate", "20261111101500");
        return params;
    }

    @Test
    void buildQuery_sortsByKeyAndEncodesLikeOfficialJavaSample() {
        assertEquals("vnp_Amount=10000000&vnp_Command=pay&vnp_CreateDate=20261111100000&vnp_CurrCode=VND"
                        + "&vnp_ExpireDate=20261111101500&vnp_IpAddr=127.0.0.1&vnp_Locale=vn"
                        + "&vnp_OrderInfo=Thanh+toan+don+hang+DB0001&vnp_OrderType=other"
                        + "&vnp_ReturnUrl=https%3A%2F%2Fexample.test%2Fvnpay%2Freturn&vnp_TmnCode=TESTCODE"
                        + "&vnp_TxnRef=DB0001&vnp_Version=2.1.0",
                VnpaySigner.buildQuery(sampleParams()));
    }

    @Test
    void sign_matchesIndependentHmacSha512() {
        assertEquals(EXPECTED_HASH, VnpaySigner.sign(sampleParams(), SECRET));
    }

    @Test
    void buildQuery_skipsEmptyValuesAndHashFields() {
        Map<String, String> params = sampleParams();
        params.put("vnp_BankCode", "");
        params.put("vnp_SecureHashType", "HmacSHA512");
        params.put("vnp_SecureHash", "abc");
        assertEquals(EXPECTED_HASH, VnpaySigner.sign(params, SECRET));
    }

    @Test
    void verify_acceptsCorrectHashCaseInsensitive() {
        Map<String, String> params = sampleParams();
        params.put("vnp_SecureHash", EXPECTED_HASH.toUpperCase());
        assertTrue(VnpaySigner.verify(params, SECRET));
    }

    @Test
    void verify_rejectsTamperedAmount() {
        Map<String, String> params = sampleParams();
        params.put("vnp_SecureHash", EXPECTED_HASH);
        params.put("vnp_Amount", "100");
        assertFalse(VnpaySigner.verify(params, SECRET));
    }

    @Test
    void verify_rejectsMissingHashOrSecret() {
        assertFalse(VnpaySigner.verify(sampleParams(), SECRET));
        Map<String, String> params = sampleParams();
        params.put("vnp_SecureHash", EXPECTED_HASH);
        assertFalse(VnpaySigner.verify(params, ""));
    }

    @Test
    void signPipe_joinsValuesWithPipeAndTreatsNullAsEmpty() {
        assertEquals("9e8d8af243858688da1524cfa0c8882148cf020b0caa541c44455d6255033dbb"
                        + "01be061b593ff24fb6ceee03038d2465cdccf6030bba7bc0c9577593c2852905",
                VnpaySigner.signPipe(SECRET, "a", "b", null, "c"));
    }
}
