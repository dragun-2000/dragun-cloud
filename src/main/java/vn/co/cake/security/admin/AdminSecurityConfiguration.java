package vn.co.cake.security.admin;

import vn.co.cake.common.RequestPathConst;
import vn.co.cake.security.repository.AdminContextRepository;
import vn.co.cake.security.service.AdminDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * SecurityConfiguration
 */
@Configuration
@EnableWebSecurity
@Order(2)
public class AdminSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Value("#{new Integer('${remember-me.session.timeout}')}")
    private Integer timeouts;

    private final AdminDetailsServiceImpl adminDetailsService;
    private final AdminSuccessHandler adminSuccessHandler;
    private final AdminFailureHandler adminFailureHandler;
    private final AdminLogoutSuccessHandler adminLogoutSuccessHandler;
    private final AdminContextRepository adminContextRepository;

    public AdminSecurityConfiguration(AdminDetailsServiceImpl adminDetailsService,
                                      AdminSuccessHandler adminSuccessHandler,
                                      AdminFailureHandler adminFailureHandler,
                                      AdminLogoutSuccessHandler adminLogoutSuccessHandler,
                                      AdminContextRepository adminContextRepository) {
        this.adminDetailsService = adminDetailsService;
        this.adminSuccessHandler = adminSuccessHandler;
        this.adminFailureHandler = adminFailureHandler;
        this.adminLogoutSuccessHandler = adminLogoutSuccessHandler;
        this.adminContextRepository = adminContextRepository;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.securityContext().securityContextRepository(adminContextRepository);
        http.sessionManagement().enableSessionUrlRewriting(true);

        http.requestMatchers().antMatchers("/AM/**", "/HO/**", "/csv/**")
            .and()
            .authorizeRequests()
            .antMatchers(RequestPathConst.HOME, RequestPathConst.AM, RequestPathConst.AM001, RequestPathConst.AM001_LOGIN, RequestPathConst.PAGE_ERROR, RequestPathConst.SA001_REGISTER).permitAll()
            .antMatchers("/favicon-16x16.png", RequestPathConst.AM001.concat("/**"), RequestPathConst.SA001.concat("/**")).permitAll()
            .antMatchers("/AM/**").hasAnyRole("ADMIN", "STAFF")
            .antMatchers("/HO/**").hasRole("MANAGE")
            .antMatchers("/csv/**").hasAnyRole("ADMIN", "MANAGE", "USER", "STAFF")
            .anyRequest().authenticated()
            .and()
            .exceptionHandling()
            .accessDeniedHandler(adminAccessDeniedHandler())
        ;

        http.formLogin()
            .loginPage(RequestPathConst.AM001)
            .loginProcessingUrl(RequestPathConst.AM001_LOGIN)
            .successHandler(adminSuccessHandler)
            .failureHandler(adminFailureHandler)
            .permitAll()
            .usernameParameter("username").passwordParameter("password")
            .and()
            .rememberMe().key("uniqueAndSecret").tokenValiditySeconds(1296000) // 15 days expired
        ;

        http.logout()
            .invalidateHttpSession(true)
            .clearAuthentication(true)
            .deleteCookies("JSESSIONID", "remember-me")
            .logoutRequestMatcher(new AntPathRequestMatcher(RequestPathConst.AM001_LOGOUT))
            .logoutSuccessHandler(adminLogoutSuccessHandler)
            .permitAll()
            .and()
            .rememberMe()
            .key("uniqueAndSecret")
            .userDetailsService(adminDetailsService)
        ;

        http.sessionManagement()
                .enableSessionUrlRewriting(true)
        ;

        http.csrf()
                .ignoringAntMatchers(RequestPathConst.HOME, RequestPathConst.AM, RequestPathConst.AM001, RequestPathConst.AM001_LOGIN, RequestPathConst.SA001_LOGIN, RequestPathConst.SA001_REGISTER)
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
        AdminAuthenticationConfigurer configurer = new AdminAuthenticationConfigurer(adminDetailsService).passwordEncoder(adminPasswordEncoder());
        auth.apply(configurer);
    }

    @Bean
    public PasswordEncoder adminPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AccessDeniedHandler adminAccessDeniedHandler() {
        return new AdminAccessDeniedHandler();
    }
}
