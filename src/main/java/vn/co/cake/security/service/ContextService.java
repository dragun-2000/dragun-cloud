package vn.co.cake.security.service;

import vn.co.cake.common.BaseConst;
import vn.co.cake.common.RequestPathConst;
import vn.co.cake.security.admin.AdminDetails;
import vn.co.cake.security.repository.ContextRepository;
import vn.co.cake.security.user.UserDetails;
import vn.co.cake.security.utils.AuthenticationUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpRequestResponseHolder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static vn.co.cake.common.RequestPathConst.AM004;
import static vn.co.cake.common.RequestPathConst.AM004_01;
import static vn.co.cake.common.RequestPathConst.AM004_02_SETTING;
import static vn.co.cake.common.RequestPathConst.AM004_DELETED;
import static vn.co.cake.common.RequestPathConst.AM004_REGISTER;
import static vn.co.cake.common.RequestPathConst.AM004_UPDATE;
import static vn.co.cake.common.StringConst.BOOKMARK_STAFF_KEY;
import static vn.co.cake.common.StringConst.BOOKMARK_USER_KEY;
import static vn.co.cake.common.StringConst.PREFIX_QUERY;

/**
 * ContextService
 */
@Configuration
@Slf4j
public class ContextService {

    @Value("${cake.base.url}")
    private String baseUrl;

    private final ContextRepository contextRepository;

    public ContextService(ContextRepository contextRepository) {
        this.contextRepository = contextRepository;
    }

    /**
     * Login successfully stores SecurityContext in Redis
     */
    public void storesSecurityContextInRedis(HttpServletRequest request) throws IOException {
        SecurityContext context = SecurityContextHolder.getContext();
        if (null != context && null != context.getAuthentication() && null != context.getAuthentication().getPrincipal()) {
            contextRepository.saveContext(
                    request.getSession().getId(),
                    AuthenticationUtils.toSerializableString(context), 15);
        }
    }

    /**
     * Remove security context by session id
     */
    public void removeSecurityContextBySessionId(HttpServletRequest request) {
        contextRepository.deleteKey(request.getSession().getId());
    }

    /**
     * Remove security context by cookie
     */
    public void removeSecurityContextByCookie(HttpServletRequest request) {
        contextRepository.deleteKey(AuthenticationUtils.getSid(request.getCookies()));
    }

    public SecurityContext getSecurityContext(HttpRequestResponseHolder holder, SecurityContext context, String id) throws IOException {
        if (null == context || null == context.getAuthentication() || null == context.getAuthentication().getPrincipal()) {
            Object contextString = contextRepository.findContextById(holder.getRequest().getSession().getId());
            if (null != contextString) {
                try {
                    context = (SecurityContext) AuthenticationUtils.fromSerializableString(contextString.toString());
                } catch (Exception e) {
                    log.error("Cannot deserialize security context !!!");
                }
            }
            
            if (null == contextString && context != null) {
                context.setAuthentication(null);
            } else if ((context.getAuthentication().getPrincipal() instanceof UserDetails && !BaseConst.UID.equals(id))
                    || (context.getAuthentication().getPrincipal() instanceof AdminDetails && !BaseConst.BID.equals(id))) {
                context.setAuthentication(null);
            }
        }

        String requestUrl = holder.getRequest().getRequestURL().toString();
        log.info("Request root = {}", requestUrl);
//        if (requestUrl.contains(PROTOCOL_LOCAL)) {
//            requestUrl = requestUrl.replaceAll(PROTOCOL_LOCAL, PROTOCOL_PUBLIC);
//        }
        
        // set bookmark url cake
        if (bookmarkValidForAM(requestUrl) && (context == null || context.getAuthentication() == null)) {

            if (StringUtils.isNotEmpty(holder.getRequest().getQueryString())) {
                requestUrl = requestUrl.concat(PREFIX_QUERY).concat(holder.getRequest().getQueryString());
            }

            log.info("Create bookmark url cake = {}", requestUrl);
            contextRepository.saveBookmarkUrl(BOOKMARK_STAFF_KEY, requestUrl);
        }

        // set bookmark url cake
        if ((context == null || context.getAuthentication() == null)) {

            if (StringUtils.isNotEmpty(holder.getRequest().getQueryString())) {
                requestUrl = requestUrl.concat(PREFIX_QUERY).concat(holder.getRequest().getQueryString());
            }

            log.info("Create bookmark url SA cake = {}", requestUrl);
            contextRepository.saveBookmarkUrl(BOOKMARK_USER_KEY, requestUrl);
        }

        log.info("Request final = {}", requestUrl);
        return context;
    }

    private boolean bookmarkValidForAM(String url) {
        String[] bookmarkUrls = {AM004, AM004_01, AM004_02_SETTING};

        boolean validUrl = false;
        for (String item : bookmarkUrls) {
            if (url.contains(item) && !url.contains(AM004_REGISTER) && !url.contains(AM004_UPDATE)
                    && !url.contains(AM004_DELETED) && !url.contains(RequestPathConst.AM001)) {
                validUrl = true;
                break;
            }
        }
        return validUrl;
    }
}
