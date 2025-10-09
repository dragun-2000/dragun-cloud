package vn.co.cake.security.admin;

import vn.co.cake.common.BaseConst;
import vn.co.cake.common.RequestPathConst;
import vn.co.cake.security.service.ContextService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * LogoutSuccessHandler
 */
@Component
public class AdminLogoutSuccessHandler implements org.springframework.security.web.authentication.logout.LogoutSuccessHandler {

    @Value("${cake.base.url}")
    protected String baseUrl;

    private final ContextService contextService;

    public AdminLogoutSuccessHandler(ContextService contextService) {
        this.contextService = contextService;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        request.getSession().removeAttribute(BaseConst.ADMIN_SESSION);

        contextService.removeSecurityContextBySessionId(request);

        String url = String.format("%s%s?logout", baseUrl, RequestPathConst.AM001);

        response.setStatus(HttpStatus.OK.value());
        response.sendRedirect(url);
    }
}
