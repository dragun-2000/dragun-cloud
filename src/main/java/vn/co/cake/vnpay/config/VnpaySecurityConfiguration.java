package vn.co.cake.vnpay.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * IPN / Return URL do VNPAY gọi hoặc redirect browser — không qua login form.
 * {@code /api/vnpay/**} vẫn kiểm tra session trong controller.
 */
@Configuration
@EnableWebSecurity
/** Order must be unique among WebSecurityConfigurerAdapter beans (VietQR already uses 0). */
@Order(-1)
public class VnpaySecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.requestMatchers().antMatchers("/vnpay/**")
                .and()
                .authorizeRequests()
                .antMatchers("/vnpay/**").permitAll()
                .and()
                .csrf()
                .ignoringAntMatchers("/vnpay/**");
    }
}
