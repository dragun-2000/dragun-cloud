package vn.co.cake.security.repository;

import vn.co.cake.common.BaseConst;
import vn.co.cake.security.service.ContextService;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * UserContextRepository
 */
@Component
public class AdminContextRepository extends HttpSessionSecurityContextRepository {

    private ContextService contextService;

    public AdminContextRepository(ContextService contextService) {
        this.contextService = contextService;
    }

    @Override
    public SecurityContext loadContext(HttpRequestResponseHolder holder) {
        SecurityContext context = super.loadContext(holder);

        try {
            return contextService.getSecurityContext(holder, context, BaseConst.BID);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
