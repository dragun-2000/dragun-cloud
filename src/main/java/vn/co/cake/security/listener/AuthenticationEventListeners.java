package vn.co.cake.security.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureCredentialsExpiredEvent;
import org.springframework.security.authentication.event.AuthenticationFailureDisabledEvent;
import org.springframework.security.authentication.event.AuthenticationFailureExpiredEvent;
import org.springframework.security.authentication.event.AuthenticationFailureLockedEvent;
import org.springframework.security.authentication.event.AuthenticationFailureServiceExceptionEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthenticationEventListeners
 */
@Component
@Transactional(rollbackForClassName = "Exception")
@Slf4j
public class AuthenticationEventListeners {

    @EventListener
    public void handleInteractiveAuthenticationSuccess(InteractiveAuthenticationSuccessEvent event) {
        log.debug("InteractiveAuthenticationSuccessEvent. username : {}", event.getAuthentication().getName());
    }

    @EventListener
    public void handleBadCredentials(AuthenticationFailureBadCredentialsEvent event) {
        log.debug("Bad credentials is detected. username : {}", event.getAuthentication().getName());
    }

    @EventListener
    public void handleDisabled(AuthenticationFailureDisabledEvent event) {
        log.debug("Disabled is detected. username : {}", event.getAuthentication().getName());
    }

    @EventListener
    public void handleLocked(AuthenticationFailureLockedEvent event) {
        log.debug("Locked is detected. username : {}", event.getAuthentication().getName());
    }

    @EventListener
    public void handleExpired(AuthenticationFailureExpiredEvent event) {
        log.debug("Expired is detected. username : {}", event.getAuthentication().getName());
    }

    @EventListener
    public void handleCredentialsExpired(AuthenticationFailureCredentialsExpiredEvent event) {
        log.debug("CredentialsExpired is detected. username : {}", event.getAuthentication().getName());
    }

    @EventListener
    public void handleServiceException(AuthenticationFailureServiceExceptionEvent event) {
        log.debug("ServiceException is detected. username : {}", event.getAuthentication().getName());
    }
}
