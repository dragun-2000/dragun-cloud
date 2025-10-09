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
}
