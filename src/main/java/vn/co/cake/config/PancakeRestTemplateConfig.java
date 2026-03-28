package vn.co.cake.config;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class PancakeRestTemplateConfig {

    /** Connect timeout (ms). 15s dễ timeout khi mạng/Docker chậm → dùng 45s, cấu hình qua properties. */
    @Value("${pancake.pos.connect-timeout:45000}")
    private int pancakeConnectTimeout;

    /** Read timeout (ms) – chờ server Pancake trả lời. */
    @Value("${pancake.pos.read-timeout:60000}")
    private int pancakeReadTimeout;

    @Bean
    @Primary
    public RestTemplate pancakeRestTemplate() {

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(pancakeConnectTimeout)
                .setSocketTimeout(pancakeReadTimeout)
                .setConnectionRequestTimeout(10_000)
                .build();

        // Mỗi request dùng connection mới, không reuse → tránh lỗi khi server/Cloudflare đóng connection
        // (call bằng terminal/curl mỗi lần cũng là connection mới nên không lỗi)
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionReuseStrategy(NoConnectionReuseStrategy.INSTANCE)
                .disableConnectionState()
                .disableAutomaticRetries()
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        return new RestTemplate(factory);
    }

    @Bean("openaiRestTemplate")
    public RestTemplate openaiRestTemplate() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(30_000)
                .setSocketTimeout(60_000)
                .setConnectionRequestTimeout(5_000)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        return new RestTemplate(factory);
    }
}
