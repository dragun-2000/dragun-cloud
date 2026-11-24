package vn.co.cake.ai.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import vn.co.cake.ai.dto.SizeAdviceChatRequest;
import vn.co.cake.ai.dto.SizeAdviceMessage;
import vn.co.cake.ai.dto.SizeAdviceRequest;
import vn.co.cake.ai.dto.SizeAdviceResponse;
import vn.co.cake.ai.service.openai.OpenAIService;
import vn.co.cake.entity.Product;
import vn.co.cake.repository.ProductRepository;

/**
 * Characterization tests for {@link SizeAdviceServiceImpl}: pins current behavior (incl. odd behavior).
 */
class SizeAdviceServiceImplTest {

    private static final long PRODUCT_ID = 7L;
    private static final String NOT_AVAILABLE = "NOT_AVAILABLE";
    private static final String FALLBACK_REASON_HEADER = "Dựa trên bảng size tham chiếu chung";
    private static final String FALLBACK_FIT_COMMENT = "Tư vấn dựa trên bảng size tham chiếu";
    private static final String TRY_THIS_SIZE = "Bạn có thể thử size này";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ProductRepository productRepository;
    private OpenAIService openAIService;
    private SizeAdviceServiceImpl service;
    private Product product;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        openAIService = mock(OpenAIService.class);
        service = new SizeAdviceServiceImpl(productRepository, openAIService);

        product = new Product();
        product.setId(PRODUCT_ID);
        product.setName("Ao thun");
        product.setSizes("S,M,L");
        product.setDescription("Ao form slim fit");
        product.setDescriptionSize("<p>S: dai 65</p><br/>M: dai 67&nbsp;cm");
        when(productRepository.findFirstByIdAndDeletedIsFalse(PRODUCT_ID)).thenReturn(product);
    }

    private static SizeAdviceRequest request(String gender, Integer height, Integer weight) {
        SizeAdviceRequest request = new SizeAdviceRequest();
        request.setProductId(PRODUCT_ID);
        request.setGender(gender);
        request.setHeight(height);
        request.setWeight(weight);
        return request;
    }

    private String aiJson(String recommendedSize, String backupSize, List<String> reasons,
                          String productFitComment, String closingQuestion) {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode data = root.putObject("data");
        data.put("productId", 999);
        ObjectNode message = data.putObject("message");
        message.put("recommendedSize", recommendedSize);
        message.put("backupSize", backupSize);
        ArrayNode reasonArray = message.putArray("reason");
        reasons.forEach(reasonArray::add);
        message.put("productFitComment", productFitComment);
        message.put("closingQuestion", closingQuestion);
        return root.toString();
    }

    private SizeAdviceMessage adviceWithAiFailing(String gender, Integer height, Integer weight) throws Exception {
        when(openAIService.callGPT(anyString(), anyString())).thenThrow(new RuntimeException("quota exceeded"));
        return service.getSizeAdvice(request(gender, height, weight)).getData().getMessage();
    }

    // ---------------------------------------------------------------- fallback path

    @ParameterizedTest
    @CsvSource({
        "female,40,S", "female,47,S", "female,48,M", "female,55,M", "female,56,L", "female,62,L",
        "female,63,XL", "female,70,XL", "female,39,S", "female,71,M", "female,80,M", "female,95,M",
        "female,96,XXL", "nữ,50,M", "Nữ,60,L", "FEMALE,45,S"
    })
    void fallbackUsesFemaleWeightTableWhenOpenAiFails(String gender, int weight, String expectedSize) throws Exception {
        // current behavior: female weights 71..95 fall through the table and get the default "M"
        assertEquals(expectedSize, adviceWithAiFailing(gender, 160, weight).getRecommendedSize());
    }

    @ParameterizedTest
    @CsvSource({
        "male,45,S", "male,55,S", "male,56,M", "male,65,M", "male,66,L", "male,75,L", "male,76,XL",
        "male,85,XL", "male,86,XXL", "male,95,XXL", "male,44,S", "male,0,S", "male,96,XXL",
        "unisex,70,L", "'',60,M", "'female ',63,M"
    })
    void fallbackUsesDefaultWeightTableForNonFemaleGender(String gender, int weight, String expectedSize)
            throws Exception {
        // current behavior: gender is only lower-cased, not trimmed -> "female " uses the male table
        assertEquals(expectedSize, adviceWithAiFailing(gender, 170, weight).getRecommendedSize());
    }

    @ParameterizedTest
    @CsvSource({ "male,50,S,M", "male,60,M,S", "male,70,L,M", "male,80,XL,L", "male,90,XXL,XL" })
    void fallbackBackupSizeMapping(String gender, int weight, String expectedSize, String expectedBackup)
            throws Exception {
        SizeAdviceMessage message = adviceWithAiFailing(gender, 170, weight);
        assertEquals(expectedSize, message.getRecommendedSize());
        // current behavior: backup for M is the smaller S, for others the next smaller size except S -> M
        assertEquals(expectedBackup, message.getBackupSize());
    }

    @Test
    void fallbackMessageContentIgnoresHeightAndPrintsNullHeight() throws Exception {
        SizeAdviceResponse response;
        when(openAIService.callGPT(anyString(), anyString())).thenThrow(new RuntimeException("boom"));
        response = service.getSizeAdvice(request("male", null, 70));

        assertEquals(Long.valueOf(PRODUCT_ID), response.getData().getProductId());
        SizeAdviceMessage message = response.getData().getMessage();
        assertEquals("L", message.getRecommendedSize());
        // current behavior: a null height is rendered literally as "null"
        assertEquals(Arrays.asList(FALLBACK_REASON_HEADER, "Chiều cao: nullcm, Cân nặng: 70kg"),
            message.getReason());
        assertEquals(FALLBACK_FIT_COMMENT, message.getProductFitComment());
        assertEquals(TRY_THIS_SIZE, message.getClosingQuestion());
    }

    @Test
    void productNotFoundFallsBackWithoutCallingOpenAi() throws Exception {
        when(productRepository.findFirstByIdAndDeletedIsFalse(PRODUCT_ID)).thenReturn(null);

        SizeAdviceResponse response = service.getSizeAdvice(request("female", 160, 50));

        // current behavior: missing product is swallowed and answered with the weight table
        assertEquals("M", response.getData().getMessage().getRecommendedSize());
        verify(openAIService, never()).callGPT(anyString(), anyString());
    }

    @Test
    void unparseableAiResponseFallsBack() throws Exception {
        when(openAIService.callGPT(anyString(), anyString())).thenReturn("Sorry, I cannot help {\"foo\": 1}");

        SizeAdviceResponse response = service.getSizeAdvice(request("male", 170, 80));

        assertEquals("XL", response.getData().getMessage().getRecommendedSize());
        assertEquals(FALLBACK_FIT_COMMENT, response.getData().getMessage().getProductFitComment());
    }

    @Test
    void fallbackWithNullGenderThrowsNullPointerException() throws Exception {
        when(openAIService.callGPT(anyString(), anyString())).thenThrow(new RuntimeException("boom"));

        // current behavior: the fallback itself is not null-safe, so the NPE escapes getSizeAdvice
        assertThrows(NullPointerException.class, () -> service.getSizeAdvice(request(null, 170, 70)));
    }

    // ---------------------------------------------------------------- happy path

    @Test
    void availableSizeWithoutConflictIsReturnedAsParsedFromWrappedJson() throws Exception {
        List<String> reasons = Arrays.asList("Size M vừa với bạn", "Chiều cao ổn");
        String json = aiJson("M", "L", reasons, "Form vừa", "Bạn thử nhé");
        when(openAIService.callGPT(anyString(), anyString()))
            .thenReturn(String.format("Here is the answer:%n%s%nThanks!", json));

        SizeAdviceResponse response = service.getSizeAdvice(request("female", 160, 50));

        // current behavior: productId comes from the request, not from the AI JSON
        assertEquals(Long.valueOf(PRODUCT_ID), response.getData().getProductId());
        SizeAdviceMessage message = response.getData().getMessage();
        assertEquals("M", message.getRecommendedSize());
        assertEquals("L", message.getBackupSize());
        assertEquals(reasons, message.getReason());
        assertEquals("Form vừa", message.getProductFitComment());
        assertEquals("Bạn thử nhé", message.getClosingQuestion());

        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        verify(openAIService).callGPT(anyString(), userPrompt.capture());
        assertTrue(userPrompt.getValue().contains("- Sizes có sẵn: S,M,L"));
        assertTrue(userPrompt.getValue().contains("- Form sản phẩm: ôm"));
        assertTrue(userPrompt.getValue().contains("S: dai 65\nM: dai 67 cm"));
    }

    @Test
    void lowerCaseRecommendedSizeMatchesAvailableSizesAndIsKeptAsIs() throws Exception {
        product.setSizes(" s , m ");
        when(openAIService.callGPT(anyString(), anyString()))
            .thenReturn(aiJson("m", "s", Collections.singletonList("ok"), "", ""));

        SizeAdviceMessage message = service.getSizeAdvice(request("male", 170, 60)).getData().getMessage();

        // current behavior: comparison is case-insensitive but the original casing is returned
        assertEquals("m", message.getRecommendedSize());
        assertEquals("", message.getProductFitComment());
    }

    @Test
    void unavailableSizeIsRewrittenToNotAvailable() throws Exception {
        when(openAIService.callGPT(anyString(), anyString()))
            .thenReturn(aiJson("Size XL", "L", Collections.singletonList("Bạn cần cỡ lớn"), "Vừa", "Thử nhé"));

        SizeAdviceMessage message = service.getSizeAdvice(request("male", 180, 80)).getData().getMessage();

        assertEquals(NOT_AVAILABLE, message.getRecommendedSize());
        assertEquals("", message.getBackupSize());
        assertEquals(Arrays.asList(
            "Dựa vào bảng đo sản phẩm, bạn cần size XL",
            "Nhưng size XL hiện đang hết hàng",
            "Sản phẩm hiện chỉ còn sizes: S,M,L"), message.getReason());
        assertEquals("Bạn cần size XL nhưng size này hiện đang hết hàng", message.getProductFitComment());
        assertEquals("Vui lòng liên hệ shop theo hotline để đặt hàng size XL hoặc chọn sản phẩm khác có size phù hợp",
            message.getClosingQuestion());
    }

    @Test
    void unavailableSizeButAvailableSizeInReasonKeepsOriginalUnavailableSize() throws Exception {
        List<String> reasons = Collections.singletonList("Theo bảng đo thì size M phù hợp");
        when(openAIService.callGPT(anyString(), anyString()))
            .thenReturn(aiJson("XL", "L", reasons, "Form vừa", "Thử nhé"));

        SizeAdviceMessage message = service.getSizeAdvice(request("male", 175, 70)).getData().getMessage();

        // current behavior: size M is detected from the reason as available, but since there is no
        // conflict the original response (recommending unavailable XL) is returned unchanged
        assertEquals("XL", message.getRecommendedSize());
        assertEquals(reasons, message.getReason());
    }

    @Test
    void availableSizeWithConflictingCommentIsRewritten() throws Exception {
        when(openAIService.callGPT(anyString(), anyString()))
            .thenReturn(aiJson("M", "", Collections.singletonList("Bạn cần size M"),
                "Không có size phù hợp", "Hỏi thêm nhé"));

        SizeAdviceMessage message = service.getSizeAdvice(request("female", 158, 52)).getData().getMessage();

        assertEquals("M", message.getRecommendedSize());
        // current behavior: blank backupSize is turned into null when a conflict is fixed
        assertNull(message.getBackupSize());
        assertEquals(Arrays.asList(
            "Dựa vào bảng đo sản phẩm, size M phù hợp với thông số của bạn",
            "Chiều cao 158cm phù hợp với độ dài áo size M",
            "Cân nặng 52kg phù hợp với vòng ngực size M"), message.getReason());
        assertEquals("Form vừa, mặc vừa vặn", message.getProductFitComment());
        assertEquals("Hỏi thêm nhé", message.getClosingQuestion());
    }

    @Test
    void notAvailableFromAiIsReturnedUnchangedEvenWhenSizesExist() throws Exception {
        List<String> reasons = Collections.singletonList("Bạn cần size M");
        when(openAIService.callGPT(anyString(), anyString()))
            .thenReturn(aiJson("not_available", "", reasons, "x", "y"));

        SizeAdviceMessage message = service.getSizeAdvice(request("male", 170, 60)).getData().getMessage();

        // current behavior: NOT_AVAILABLE (case-insensitive) short-circuits validation entirely
        assertEquals("not_available", message.getRecommendedSize());
        assertEquals(reasons, message.getReason());
    }

    // ---------------------------------------------------------------- chat

    @Test
    void chatParsesHeightWeightAndTreatsFemaleAsMale() throws Exception {
        when(openAIService.callGPTWithHistory(anyString(), anyList())).thenReturn("AI reply");
        SizeAdviceChatRequest chatRequest = new SizeAdviceChatRequest();
        chatRequest.setProductId(PRODUCT_ID);
        chatRequest.setGender("female");
        chatRequest.setHeight(150);
        chatRequest.setWeight(45);
        chatRequest.setMessage("I am female, cao 170cm nặng 60kg");

        String reply = service.chat(chatRequest);

        assertEquals("AI reply", reply);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Map<String, String>>> history = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
        verify(openAIService).callGPTWithHistory(systemPrompt.capture(), history.capture());

        // current behavior: "female" contains "male", so the message is parsed as male ("Nam")
        assertTrue(systemPrompt.getValue().contains("- Giới tính: Nam\n"));
        assertTrue(systemPrompt.getValue().contains("- Chiều cao: 170cm\n"));
        assertTrue(systemPrompt.getValue().contains("- Cân nặng: 60kg\n"));
        assertTrue(systemPrompt.getValue().contains("- Form sản phẩm: ôm\n"));
        assertEquals(2, history.getValue().size());
        assertEquals("Tôi muốn tư vấn size cho sản phẩm này. Chiều cao: 170cm. Cân nặng: 60kg. Giới tính: Nam. ",
            history.getValue().get(0).get("content"));
        assertEquals("user", history.getValue().get(1).get("role"));
        assertEquals("I am female, cao 170cm nặng 60kg", history.getValue().get(1).get("content"));
        // current behavior: the caller's request object is not mutated
        assertEquals("female", chatRequest.getGender());
    }

    @Test
    void chatKeepsContextWhenMessageHasNoNewInfoAndAppendsToGivenHistory() throws Exception {
        when(openAIService.callGPTWithHistory(anyString(), anyList())).thenReturn("ok");
        Map<String, String> previous = new HashMap<>();
        previous.put("role", "assistant");
        previous.put("content", "Bạn nên mặc M");
        SizeAdviceChatRequest chatRequest = new SizeAdviceChatRequest();
        chatRequest.setProductId(PRODUCT_ID);
        chatRequest.setGender("other");
        chatRequest.setHeight(165);
        chatRequest.setMessage("Cao 300 thì sao?");
        chatRequest.setConversationHistory(new ArrayList<>(Collections.singletonList(previous)));

        service.chat(chatRequest);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Map<String, String>>> history = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
        verify(openAIService).callGPTWithHistory(systemPrompt.capture(), history.capture());

        // current behavior: parsed height 300 is out of range (<300 required) so the old 165 is kept;
        // unknown gender is rendered as "Unisex"
        assertTrue(systemPrompt.getValue().contains("- Chiều cao: 165cm\n"));
        assertTrue(systemPrompt.getValue().contains("- Giới tính: Unisex\n"));
        assertEquals(2, history.getValue().size());
        assertSame(previous, history.getValue().get(0));
        assertEquals("Cao 300 thì sao?", history.getValue().get(1).get("content"));
    }

    @Test
    void chatRethrowsWhenProductNotFound() throws Exception {
        when(productRepository.findFirstByIdAndDeletedIsFalse(PRODUCT_ID)).thenReturn(null);
        SizeAdviceChatRequest chatRequest = new SizeAdviceChatRequest();
        chatRequest.setProductId(PRODUCT_ID);
        chatRequest.setMessage("hi");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.chat(chatRequest));
        assertEquals(String.format("Product not found: %d", PRODUCT_ID), ex.getMessage());
        verify(openAIService, never()).callGPTWithHistory(anyString(), anyList());
    }
}
