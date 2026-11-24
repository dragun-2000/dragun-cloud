package vn.co.cake.handler;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.ModelAndView;
import vn.co.cake.common.ScreenPathConst;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.response.CommonResponse;

import java.io.EOFException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Characterization tests for {@link GlobalExceptionHandler}; handler methods are invoked directly.
 */
class GlobalExceptionHandlerTest {

    private static final String ERROR_VIEW_NAME = "error-page";
    private static final String MODEL_KEY_MESSAGE = "message";
    private static final String MODEL_KEY_URL = "url";
    private static final String RESULT_INTERNAL_SERVER_ERROR = "Internal Server Error";
    private static final String WEB_URI = "/products/1";
    private static final String WEB_URL = "http://localhost/products/1";
    private static final String API_URI = "/api/orders";
    private static final String ACCEPT_HEADER = "Accept";
    private static final String NPE_MESSAGE = "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.";
    private static final String ILLEGAL_ARGUMENT_FALLBACK_MESSAGE = "Tham số không hợp lệ.";
    private static final String GENERIC_MESSAGE = "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau.";
    private static final String OPTIMISTIC_LOCK_MESSAGE =
            "Dữ liệu đã được cập nhật bởi người dùng khác. Vui lòng làm mới trang và thử lại.";
    private static final String MULTIPART_PREFIX = "Lỗi khi xử lý form upload. ";

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    private static MockHttpServletRequest webRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", WEB_URI);
        request.addHeader(ACCEPT_HEADER, "text/html");
        return request;
    }

    private static MockHttpServletRequest apiUriRequest() {
        return new MockHttpServletRequest("POST", API_URI);
    }

    private static MockHttpServletRequest jsonAcceptRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", WEB_URI);
        request.addHeader(ACCEPT_HEADER, "application/json, text/plain");
        return request;
    }

    @SuppressWarnings("unchecked")
    private static ResponseEntity<CommonResponse> asResponseEntity(Object result) {
        assertTrue(result instanceof ResponseEntity);
        return (ResponseEntity<CommonResponse>) result;
    }

    private static void assertErrorPage(Object result, String expectedMessage) {
        assertTrue(result instanceof ModelAndView);
        ModelAndView mav = (ModelAndView) result;
        assertEquals(ERROR_VIEW_NAME, mav.getViewName());
        assertEquals(expectedMessage, mav.getModel().get(MODEL_KEY_MESSAGE));
        // current behavior: "url" is the StringBuffer from getRequestURL(), not a String
        assertEquals(WEB_URL, mav.getModel().get(MODEL_KEY_URL).toString());
        assertNull(mav.getStatus());
    }

    private static void assertApiError(Object result, String expectedMessage) {
        ResponseEntity<CommonResponse> response = asResponseEntity(result);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(RESULT_INTERNAL_SERVER_ERROR, response.getBody().getResult());
        assertEquals(expectedMessage, response.getBody().getMessage());
    }

    @SuppressWarnings("unused")
    private void sampleEndpoint(String body) {
        // used only to build a MethodParameter
    }

    private static MethodParameter sampleMethodParameter() throws NoSuchMethodException {
        return new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("sampleEndpoint", String.class), 0);
    }

    @Test
    void errorScreenConstantIsErrorPage() {
        assertEquals(ERROR_VIEW_NAME, ScreenPathConst.ERROR_SCREEN);
    }

    @Test
    void defaultErrorHandlerReturnsErrorPageWithExceptionMessageAndNoStatus() {
        ModelAndView mav = handler.defaultErrorHandler(webRequest(), new CommonServletException("denied"));

        assertErrorPage(mav, "denied");
        assertEquals(2, mav.getModel().size());
    }

    @Test
    void defaultErrorHandlerReturnsErrorPageEvenForApiRequests() {
        ModelAndView mav = handler.defaultErrorHandler(apiUriRequest(), new CommonServletException("denied"));

        // current behavior: no API detection for CommonServletException
        assertEquals(ERROR_VIEW_NAME, mav.getViewName());
        assertEquals("http://localhost/api/orders", mav.getModel().get(MODEL_KEY_URL).toString());
        assertNull(mav.getStatus());
    }

    @Test
    void handleConversionReturnsBadRequestWithRawExceptionMessage() {
        ConversionFailedException ex = new ConversionFailedException(
                TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class),
                "abc", new NumberFormatException("bad number"));

        ResponseEntity<String> response = handler.handleConversion(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ex.getMessage(), response.getBody());
        assertTrue(response.getBody().startsWith(
                "Failed to convert from type [java.lang.String] to type [java.lang.Integer] for value 'abc'"));
    }

    @Test
    void handleValidationExceptionJoinsFieldErrorDefaultMessagesAndIgnoresGlobalErrors()
            throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "form");
        bindingResult.addError(new FieldError("form", "phone", "Phone is invalid"));
        bindingResult.addError(new ObjectError("form", "Global problem"));
        bindingResult.addError(new FieldError("form", "fullName", "Name is required"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(sampleMethodParameter(), bindingResult);

        ResponseEntity<CommonResponse> response = handler.handleValidationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation Failed", response.getBody().getResult());
        assertEquals("Phone is invalid, Name is required", response.getBody().getMessage());
    }

    @Test
    void handleValidationExceptionRendersNullDefaultMessageAndEmptyErrors() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "form");
        bindingResult.addError(new FieldError("form", "phone", null));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(sampleMethodParameter(), bindingResult);

        ResponseEntity<CommonResponse> response = handler.handleValidationException(ex);

        // current behavior: a null default message is joined as the literal "null"
        assertEquals("null", response.getBody().getMessage());

        MethodArgumentNotValidException emptyEx = new MethodArgumentNotValidException(
                sampleMethodParameter(), new BeanPropertyBindingResult(new Object(), "form"));
        assertEquals("", handler.handleValidationException(emptyEx).getBody().getMessage());
    }

    @Test
    void handleHttpMessageNotReadableWithJsonMappingCauseListsPathFieldNames() {
        JsonMappingException cause = new JsonMappingException(null, "cannot deserialize");
        cause.prependPath(new JsonMappingException.Reference(null, "qty"));
        cause.prependPath(new JsonMappingException.Reference(null, 0));
        cause.prependPath(new JsonMappingException.Reference(null, "items"));
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error", cause, new MockHttpInputMessage(new byte[0]));

        ResponseEntity<CommonResponse> response = handler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("JSON parse error", response.getBody().getResult());
        // current behavior: array index path elements have no field name and render as "null"
        assertEquals("Invalid data in field: items, null, qty", response.getBody().getMessage());
    }

    @Test
    void handleHttpMessageNotReadableWithJsonMappingCauseAndEmptyPath() {
        JsonMappingException cause = new JsonMappingException(null, "cannot deserialize");
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error", cause, new MockHttpInputMessage(new byte[0]));

        ResponseEntity<CommonResponse> response = handler.handleHttpMessageNotReadable(ex);

        assertEquals("Invalid data in field: ", response.getBody().getMessage());
    }

    @Test
    void handleHttpMessageNotReadableWithoutJsonCauseReturnsInvalidRequest() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "Required request body is missing", new MockHttpInputMessage(new byte[0]));

        ResponseEntity<CommonResponse> response = handler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid Request", response.getBody().getResult());
        assertEquals("Required request body is missing", response.getBody().getMessage());
    }

    @Test
    void handleHttpMessageNotReadableWithNonJsonCauseUsesNestedMessage() {
        IOException cause = new IOException("stream closed");
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "I/O error", cause, new MockHttpInputMessage(new byte[0]));

        ResponseEntity<CommonResponse> response = handler.handleHttpMessageNotReadable(ex);

        assertEquals("Invalid Request", response.getBody().getResult());
        assertEquals(ex.getMessage(), response.getBody().getMessage());
    }

    @Test
    void nullPointerExceptionOnWebRequestReturnsErrorPage() {
        assertErrorPage(handler.handleNullPointerException(webRequest(), new NullPointerException("x")), NPE_MESSAGE);
    }

    @Test
    void nullPointerExceptionOnApiUriReturnsInternalServerError() {
        assertApiError(handler.handleNullPointerException(apiUriRequest(), new NullPointerException()), NPE_MESSAGE);
    }

    @Test
    void nullPointerExceptionWithJsonAcceptHeaderReturnsInternalServerError() {
        assertApiError(handler.handleNullPointerException(jsonAcceptRequest(), new NullPointerException()),
                NPE_MESSAGE);
    }

    @Test
    void illegalArgumentExceptionUsesExceptionMessageOrFallback() {
        assertErrorPage(handler.handleIllegalArgumentException(webRequest(), new IllegalArgumentException("bad id")),
                "bad id");
        assertErrorPage(handler.handleIllegalArgumentException(webRequest(), new IllegalArgumentException()),
                ILLEGAL_ARGUMENT_FALLBACK_MESSAGE);
        // current behavior: the raw exception message is exposed to API clients with a 500 status
        assertApiError(handler.handleIllegalArgumentException(apiUriRequest(), new IllegalArgumentException("bad id")),
                "bad id");
    }

    @Test
    void missingParameterBuildsMessageFromParameterName() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("phone", "String");
        String expected = "Thiếu thông tin bắt buộc: phone. Vui lòng kiểm tra lại form.";

        assertErrorPage(handler.handleMissingParameter(webRequest(), ex), expected);
        // current behavior: a missing parameter on an API request is reported as 500, not 400
        assertApiError(handler.handleMissingParameter(apiUriRequest(), ex), expected);
    }

    @Test
    void handleAllExceptionsUsesGenericMessage() {
        assertErrorPage(handler.handleAllExceptions(webRequest(), new IllegalStateException("secret")),
                GENERIC_MESSAGE);
        assertApiError(handler.handleAllExceptions(apiUriRequest(), new IllegalStateException("secret")),
                GENERIC_MESSAGE);
    }

    @Test
    void apiDetectionUsesUriPrefixWithoutPathBoundary() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/apiary/docs");

        // current behavior: any URI starting with "/api" (e.g. "/apiary") is treated as an API request
        assertApiError(handler.handleAllExceptions(request, new Exception()), GENERIC_MESSAGE);
    }

    @Test
    void apiDetectionIgnoresWildcardAcceptAndMissingAccept() {
        MockHttpServletRequest wildcard = new MockHttpServletRequest("GET", WEB_URI);
        wildcard.addHeader(ACCEPT_HEADER, "*/*");
        MockHttpServletRequest noAccept = new MockHttpServletRequest("GET", WEB_URI);

        assertErrorPage(handler.handleAllExceptions(wildcard, new Exception()), GENERIC_MESSAGE);
        assertErrorPage(handler.handleAllExceptions(noAccept, new Exception()), GENERIC_MESSAGE);
    }

    @Test
    void optimisticLockingFailureUsesConcurrentModificationMessage() {
        ObjectOptimisticLockingFailureException ex =
                new ObjectOptimisticLockingFailureException("stale", new IllegalStateException("stale"));

        assertErrorPage(handler.handleOptimisticLockingFailure(webRequest(), ex), OPTIMISTIC_LOCK_MESSAGE);
        assertApiError(handler.handleOptimisticLockingFailure(apiUriRequest(), ex), OPTIMISTIC_LOCK_MESSAGE);
    }

    @Test
    void multipartExceptionWithEofCauseReportsConnectionAborted() {
        MultipartException ex = new MultipartException("upload failed", new EOFException());
        String expected = String.format("%s%s%s%s", MULTIPART_PREFIX,
                "Kết nối bị ngắt trong quá trình upload. ",
                "Có thể do: file quá lớn, mất kết nối mạng, hoặc timeout. ",
                "Vui lòng thử lại với file nhỏ hơn hoặc kiểm tra kết nối mạng.");

        assertErrorPage(handler.handleMultipartException(webRequest(), ex), expected);
    }

    @Test
    void multipartExceptionWithSizeInMessageReportsSizeLimit() {
        MultipartException ex = new MultipartException("Maximum upload size exceeded");

        assertErrorPage(handler.handleMultipartException(webRequest(), ex),
                String.format("%s%s", MULTIPART_PREFIX, "Kích thước file vượt quá giới hạn cho phép (100MB)."));
    }

    @Test
    void multipartExceptionWithUnrecognizedMessageAppendsDetail() {
        MultipartException ex = new MultipartException("weird failure");

        assertErrorPage(handler.handleMultipartException(webRequest(), ex),
                String.format("%s%s", MULTIPART_PREFIX, "Chi tiết: weird failure"));
    }

    @Test
    void multipartExceptionWithNullMessageChecksContentType() {
        MockHttpServletRequest multipartRequest = webRequest();
        multipartRequest.setContentType("multipart/form-data; boundary=x");

        assertErrorPage(handler.handleMultipartException(webRequest(), new MultipartException(null)),
                String.format("%s%s", MULTIPART_PREFIX,
                        "Request không phải là multipart/form-data. Vui lòng kiểm tra lại form."));
        assertErrorPage(handler.handleMultipartException(multipartRequest, new MultipartException(null)),
                String.format("%s%s", MULTIPART_PREFIX, "Vui lòng kiểm tra lại file và thử lại."));
    }
}
