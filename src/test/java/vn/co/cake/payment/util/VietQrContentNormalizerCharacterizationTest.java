package vn.co.cake.payment.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VietQrContentNormalizerCharacterizationTest {

    private static final String FALLBACK_CONTENT = "THANHTOAN";

    @Test
    void normalizeOrderContentPrefixesDhAndUpperCases() {
        assertEquals("DHDB001", VietQrContentNormalizer.normalizeOrderContent("db001"));
    }

    @Test
    void normalizeOrderContentStripsNonAlphanumericCharacters() {
        assertEquals("DHDB2026001", VietQrContentNormalizer.normalizeOrderContent("DB-2026_001 #"));
    }

    @Test
    void normalizeOrderContentWithNullCodeProducesDhNull() {
        // current behavior: String.format renders null as "null", so a null order code becomes "DHNULL"
        assertEquals("DHNULL", VietQrContentNormalizer.normalizeOrderContent(null));
    }

    @Test
    void normalizeOrderContentWithEmptyCodeProducesDhOnly() {
        assertEquals("DH", VietQrContentNormalizer.normalizeOrderContent(""));
    }

    @Test
    void normalizeReturnsFallbackForNullAndEmpty() {
        assertEquals(FALLBACK_CONTENT, VietQrContentNormalizer.normalize(null));
        assertEquals(FALLBACK_CONTENT, VietQrContentNormalizer.normalize(""));
    }

    @Test
    void normalizeReturnsFallbackWhenNothingAlphanumericRemains() {
        assertEquals(FALLBACK_CONTENT, VietQrContentNormalizer.normalize("   "));
        assertEquals(FALLBACK_CONTENT, VietQrContentNormalizer.normalize("#-!@ "));
    }

    @Test
    void normalizeStripsAccentsButDropsLetterDStroke() {
        // current behavior: 'Đ'/'đ' do not decompose under NFD, so they are removed instead of mapped to 'D'
        assertEquals("ONHANG", VietQrContentNormalizer.normalize("Đơn hàng"));
        assertEquals("THANHTOANON", VietQrContentNormalizer.normalize("Thanh toán đơn"));
    }

    @Test
    void normalizeUpperCasesContentOfExactlyMaxLength() {
        String input = "abcdefghijklmnopqrstuvw";
        assertEquals(23, input.length());
        assertEquals("ABCDEFGHIJKLMNOPQRSTUVW", VietQrContentNormalizer.normalize(input));
    }

    @Test
    void normalizeTruncatesTo23CharactersWithoutUpperCasing() {
        // current behavior: when content exceeds 23 chars it is truncated but NOT upper-cased
        assertEquals("abcdefghijklmnopqrstuvw", VietQrContentNormalizer.normalize("abcdefghijklmnopqrstuvwxyz"));
    }

    @Test
    void normalizeOrderContentTruncatesLongCodeWithoutUpperCasing() {
        // current behavior: "DH" + 25 chars -> first 23 chars kept with original case
        String code = "abcdefghijklmnopqrstuvwxy";
        assertEquals("DHabcdefghijklmnopqrstu", VietQrContentNormalizer.normalizeOrderContent(code));
    }

    @Test
    void normalizeCountsLengthAfterStrippingSpecialCharacters() {
        String input = "a-b-c-d-e-f-g-h-i-j-k-l-m-n-o-p-q-r-s-t-u-v-w";
        assertEquals("ABCDEFGHIJKLMNOPQRSTUVW", VietQrContentNormalizer.normalize(input));
    }
}
