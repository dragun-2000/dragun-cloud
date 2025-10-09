package vn.co.cake.security.admin;

import vn.co.cake.common.BaseConst;
import vn.co.cake.common.RequestPathConst;
import vn.co.cake.enums.Authorities;
import vn.co.cake.security.service.ContextService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

/**
 * SuccessHandler
 */
@Component
public class AdminSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${cake.base.url}")
    protected String baseUrl;

    private final ContextService contextService;

    public AdminSuccessHandler(ContextService contextService) {
        this.contextService = contextService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        contextService.removeSecurityContextByCookie(request);

        AdminDetails simpleUser = (AdminDetails) authentication.getPrincipal();
        AdminLoginInfo adminLoginInfo = new AdminLoginInfo(simpleUser);
        redirectResponse(adminLoginInfo, request, response);
    }

    /**
     * Send redirect response with result checking login success or not
     */
    private void redirectResponse(AdminLoginInfo adminLoginInfo, HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.getSession().setAttribute(BaseConst.ADMIN_SESSION, adminLoginInfo);

        String requestUrl = request.getRequestURL().toString();
        boolean validate = validate(requestUrl, adminLoginInfo.getAuthority());
        
        if (!validate) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.sendRedirect(baseUrl);
        } else {
            contextService.storesSecurityContextInRedis(request);

            String url = baseUrl + RequestPathConst.AM002;
    
            response.setStatus(HttpStatus.OK.value());
            response.sendRedirect(url);
        }
    }
    
    private boolean validate(String requestUrl, Authorities authorities) {
        if (Objects.isNull(requestUrl)) {
            return false;
        }
        if (!requestUrl.contains(RequestPathConst.SA) && authorities.equals(Authorities.ROLE_USER)) {
            return false;
        }
        if (!requestUrl.contains(RequestPathConst.AM) && (authorities.equals(Authorities.ROLE_STAFF) || authorities.equals(Authorities.ROLE_ADMIN))) {
            return false;
        }
        return true;
    }
}
