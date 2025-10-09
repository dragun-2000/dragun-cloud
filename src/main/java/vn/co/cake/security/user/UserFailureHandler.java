package vn.co.cake.security.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * FailureHandler
 */
@Component
public class UserFailureHandler implements AuthenticationFailureHandler {

    @Value("${cake.base.url-sa}")
    protected String baseUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        String url = String.format("%s?error", baseUrl);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.sendRedirect(url);
    }
}
