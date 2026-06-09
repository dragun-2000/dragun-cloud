package vn.co.cake.payment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
@Order(0)
public class VietQrSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.requestMatchers().antMatchers("/vqr/**")
                .and()
                .authorizeRequests()
                .antMatchers("/vqr/**").permitAll()
                .and()
                .csrf()
                .ignoringAntMatchers("/vqr/**");
    }
}
