package vn.co.cake.security.repository;

import vn.co.cake.common.BaseConst;
import vn.co.cake.security.service.ContextService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * UserContextRepository
 */
@Component
public class UserContextRepository extends HttpSessionSecurityContextRepository {

    private ContextService contextService;
    private Authentication authentication;

    public UserContextRepository(ContextService contextService) {
        this.contextService = contextService;
    }

    @Override
    public SecurityContext loadContext(HttpRequestResponseHolder holder) {
        SecurityContext context = super.loadContext(holder);

        try {
            SecurityContext securityContext = contextService.getSecurityContext(holder, context, BaseConst.UID);
            authentication = securityContext.getAuthentication();
            return securityContext;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
