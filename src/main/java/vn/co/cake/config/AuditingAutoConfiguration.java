package vn.co.cake.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;

/**
 * AuditingAutoConfiguration
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditingAutoConfiguration {

    private static final String SYSTEM = "system";

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of(getDefaultUser());
    }

    @Bean
    public DateTimeProvider dateTimeProvider() {
        return () -> Optional.of((new Date()).toInstant());
    }

    private String getDefaultUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (Objects.nonNull(authentication) && StringUtils.isNoneBlank(authentication.getName())) {
            return authentication.getName();
        }
        return SYSTEM;
    }
}
