package vn.co.cake.security.user;

import vn.co.cake.common.RequestPathConst;
import vn.co.cake.security.repository.UserContextRepository;
import vn.co.cake.security.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import static vn.co.cake.common.RequestPathConst.SA001_REGISTER;
import static vn.co.cake.common.RequestPathConst.SA001_REGISTER_RESEND_OTP;
import static vn.co.cake.common.RequestPathConst.SA001_REGISTER_SEND_OTP;
import static vn.co.cake.common.RequestPathConst.SA001_REGISTER_VERIFY_OTP;
import static vn.co.cake.common.RequestPathConst.SA002_FORGET_PASSWORD;
import static vn.co.cake.common.RequestPathConst.SA002_RESET_PASSWORD;
import static vn.co.cake.common.RequestPathConst.SA002_SEND_MAIL;
import static vn.co.cake.common.RequestPathConst.SA002_VERIFY_USER;

/**
 * SecurityConfiguration
 */
@Configuration
@EnableWebSecurity
@Order(1)
public class UserSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Value("#{new Integer('${remember-me.session.timeout}')}")
    private Integer timeouts;

    private final UserDetailsServiceImpl userDetailsService;
    private final UserSuccessHandler userSuccessHandler;
    private final UserFailureHandler userFailureHandler;
    private final UserLogoutSuccessHandler userLogoutSuccessHandler;
    private final UserContextRepository userContextRepository;

    public UserSecurityConfiguration(UserDetailsServiceImpl userDetailsService, UserSuccessHandler userSuccessHandler,
                                     UserFailureHandler userFailureHandler, UserLogoutSuccessHandler userLogoutSuccessHandler,
                                     UserContextRepository userContextRepository) {
        this.userDetailsService = userDetailsService;
        this.userSuccessHandler = userSuccessHandler;
        this.userFailureHandler = userFailureHandler;
        this.userLogoutSuccessHandler = userLogoutSuccessHandler;
        this.userContextRepository = userContextRepository;
    }
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.securityContext().securityContextRepository(userContextRepository);

        http.requestMatchers().antMatchers("/SA/**", "/csv/**")
            .and()
            .authorizeRequests()
            .antMatchers("/favicon-16x16.png", RequestPathConst.SA001, RequestPathConst.SA001_LOGIN,
             SA002_FORGET_PASSWORD, SA002_SEND_MAIL, SA002_VERIFY_USER, SA002_RESET_PASSWORD).permitAll()
            .antMatchers(RequestPathConst.SA001.concat("/**"), RequestPathConst.PAGE_ERROR, SA001_REGISTER,
             SA001_REGISTER_SEND_OTP, SA001_REGISTER_VERIFY_OTP, SA001_REGISTER_RESEND_OTP).permitAll()
            .antMatchers("/SA/**").hasRole("USER")
            .antMatchers("/HO/**").hasRole("MANAGE")
            .antMatchers("/csv/**").hasAnyRole("ADMIN", "MANAGE", "USER", "STAFF")
            .anyRequest().authenticated()
            .and()
            .exceptionHandling()
            .accessDeniedHandler(userAccessDeniedHandler())
        ;

        http.formLogin()
            .loginPage(RequestPathConst.SA001)
            .loginProcessingUrl(RequestPathConst.SA001_LOGIN)
            .successHandler(userSuccessHandler)
            .failureHandler(userFailureHandler)
            .permitAll()
            .usernameParameter("username").passwordParameter("password")
            .and()
            .rememberMe().key("uniqueAndSecret").tokenValiditySeconds(1296000) // 15 days expired
        ;

        http.logout()
            .invalidateHttpSession(true)
            .clearAuthentication(true)
            .deleteCookies("JSESSIONID", "remember-me")
            .logoutRequestMatcher(new AntPathRequestMatcher(RequestPathConst.SA001_LOGOUT))
            .logoutSuccessHandler(userLogoutSuccessHandler)
            .permitAll()
            .and()
            .rememberMe()
            .key("uniqueAndSecret")
            .userDetailsService(userDetailsService)
        ;

        // Cookie-only session: tránh ;jsessionid=... trên URL (bị StrictHttpFirewall từ chối).
        http.sessionManagement().enableSessionUrlRewriting(false);

        http.csrf()
                .ignoringAntMatchers(RequestPathConst.SA, RequestPathConst.SA001, RequestPathConst.SA001_LOGIN,
                 SA002_FORGET_PASSWORD, SA001_REGISTER, SA001_REGISTER_SEND_OTP, SA001_REGISTER_VERIFY_OTP, SA001_REGISTER_RESEND_OTP,
                 SA002_SEND_MAIL, SA002_VERIFY_USER, SA002_RESET_PASSWORD)
        ;

        http.headers()
                .contentTypeOptions()
        ;

        http.headers()
                .frameOptions()
                .sameOrigin()
        ;

        http.headers()
                .xssProtection()
                .xssProtectionEnabled(true)
        ;

        http.headers()
                .httpStrictTransportSecurity()
                .maxAgeInSeconds(31536000)
                .includeSubDomains(true)
        ;
    }
    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers("/images/**", "/js/**", "/css/**");
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        UserAuthenticationConfigurer configurer = new UserAuthenticationConfigurer(userDetailsService).passwordEncoder(userPasswordEncoder());
        auth.apply(configurer);
    }
    
    @Bean(name = BeanIds.AUTHENTICATION_MANAGER)
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public PasswordEncoder userPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AccessDeniedHandler userAccessDeniedHandler() {
        return new UserAccessDeniedHandler();
    }
}
