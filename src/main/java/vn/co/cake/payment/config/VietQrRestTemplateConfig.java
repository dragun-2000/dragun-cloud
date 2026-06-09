package vn.co.cake.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class VietQrRestTemplateConfig {

    @Bean("vietQrRestTemplate")
    public RestTemplate vietQrRestTemplate() {
        return new RestTemplate();
    }
}
