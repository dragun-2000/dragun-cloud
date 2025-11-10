package vn.co.cake.handler;

import com.fasterxml.jackson.databind.JsonMappingException;
import vn.co.cake.common.ScreenPathConst;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.response.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GlobalExceptionHandler
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = {CommonServletException.class})
    public ModelAndView defaultErrorHandler(HttpServletRequest req, Exception e) {

        log.error("[URL] : {}", req.getRequestURL(), e);

        ModelAndView mav = new ModelAndView();
        mav.addObject("message", e.getMessage());
        mav.addObject("url", req.getRequestURL());
        mav.setViewName(ScreenPathConst.ERROR_SCREEN);
        return mav;
    }

    @ExceptionHandler(ConversionFailedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<String> handleConversion(RuntimeException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse> handleValidationException(MethodArgumentNotValidException ex) {

        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        List<String> errorMessages = new ArrayList<>();
        for (FieldError fieldError : fieldErrors) {
            errorMessages.add(fieldError.getDefaultMessage());
        }
        CommonResponse errorResponse = CommonResponse.builder()
                .result("Validation Failed")
                .message(String.join(", ", errorMessages))
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommonResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {

        if (ex.getCause() instanceof JsonMappingException) {
            JsonMappingException jsonMappingException = (JsonMappingException) ex.getCause();
            String fieldName = jsonMappingException.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .collect(Collectors.joining(", "));

            CommonResponse errorResponse = CommonResponse.builder()
                    .result("JSON parse error")
                    .message("Invalid data in field: " + fieldName)
                    .build();

            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        CommonResponse errorResponse = CommonResponse.builder()
                .result("Invalid Request")
                .message(ex.getMessage())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle ObjectOptimisticLockingFailureException (concurrent modification)
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public Object handleOptimisticLockingFailure(HttpServletRequest req, ObjectOptimisticLockingFailureException e) {
        log.warn("Optimistic locking failure - [URL]: {}, [Method]: {}, [Message]: {}", 
                req.getRequestURL(), req.getMethod(), e.getMessage());
        return handleGenericException(req, e, "Dữ liệu đã được cập nhật bởi người dùng khác. Vui lòng làm mới trang và thử lại.");
    }

    /**
     * Handle NullPointerException
     */
    @ExceptionHandler(NullPointerException.class)
    public Object handleNullPointerException(HttpServletRequest req, NullPointerException e) {
        log.error("NullPointerException occurred - [URL]: {}, [Method]: {}", 
                req.getRequestURL(), req.getMethod(), e);
        return handleGenericException(req, e, "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.");
    }

    /**
     * Handle IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgumentException(HttpServletRequest req, IllegalArgumentException e) {
        log.error("IllegalArgumentException occurred - [URL]: {}, [Method]: {}, [Message]: {}", 
                req.getRequestURL(), req.getMethod(), e.getMessage(), e);
        return handleGenericException(req, e, e.getMessage() != null ? e.getMessage() : "Tham số không hợp lệ.");
    }

    /**
     * Handle all unhandled exceptions
     */
    @ExceptionHandler(Exception.class)
    public Object handleAllExceptions(HttpServletRequest req, Exception e) {
        log.error("Unhandled exception occurred - [URL]: {}, [Method]: {}, [Exception]: {}", 
                req.getRequestURL(), req.getMethod(), e.getClass().getName(), e);
        return handleGenericException(req, e, "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau.");
    }

    /**
     * Generic exception handler helper method
     */
    private Object handleGenericException(HttpServletRequest req, Exception e, String userMessage) {
        // Check if it's an API request (has Accept header with application/json or starts with /api)
        String acceptHeader = req.getHeader("Accept");
        boolean isApiRequest = (acceptHeader != null && acceptHeader.contains("application/json")) 
                || req.getRequestURI().startsWith("/api");
        
        if (isApiRequest) {
            CommonResponse errorResponse = CommonResponse.builder()
                    .result("Internal Server Error")
                    .message(userMessage)
                    .build();
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            // For web requests, return error page
            ModelAndView mav = new ModelAndView();
            mav.addObject("message", userMessage);
            mav.addObject("url", req.getRequestURL());
            mav.setViewName(ScreenPathConst.ERROR_SCREEN);
            return mav;
        }
    }
}
