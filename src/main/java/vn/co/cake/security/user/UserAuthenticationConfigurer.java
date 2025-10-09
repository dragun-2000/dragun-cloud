package vn.co.cake.security.user;

import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.userdetails.UserDetailsAwareConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * AuthenticationConfigurer
 */
public class UserAuthenticationConfigurer extends UserDetailsAwareConfigurer<AuthenticationManagerBuilder, UserDetailsService> {

    private UserAuthenticationProvider provider = new UserAuthenticationProvider();

    private final UserDetailsService userDetailsService;

    public UserAuthenticationConfigurer(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
        this.provider.setUserDetailsService(userDetailsService);
    }

    public UserAuthenticationConfigurer passwordEncoder(PasswordEncoder passwordEncoder) {
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
