package vn.co.cake.security.admin;

import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.userdetails.UserDetailsAwareConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * AuthenticationConfigurer
 */
public class AdminAuthenticationConfigurer extends UserDetailsAwareConfigurer<AuthenticationManagerBuilder, UserDetailsService> {

    private AdminAuthenticationProvider provider = new AdminAuthenticationProvider();

    private final UserDetailsService userDetailsService;

    public AdminAuthenticationConfigurer(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
        this.provider.setUserDetailsService(userDetailsService);
    }

    public AdminAuthenticationConfigurer passwordEncoder(PasswordEncoder passwordEncoder) {
        this.provider.setPasswordEncoder(passwordEncoder);
        return this;
    }

    @Override
    public void configure(AuthenticationManagerBuilder builder) {
        this.provider = postProcess(this.provider);
        builder.authenticationProvider(this.provider);
    }

    @Override
    public UserDetailsService getUserDetailsService() {
        return this.userDetailsService;
    }
}
