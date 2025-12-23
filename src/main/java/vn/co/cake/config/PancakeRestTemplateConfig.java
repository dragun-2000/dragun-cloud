package vn.co.cake.config;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class PancakeRestTemplateConfig {

    @Bean
    @Primary
    public RestTemplate pancakeRestTemplate() {

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(15_000)   // connect 443
                .setSocketTimeout(60_000)    // read timeout
                .setConnectionRequestTimeout(5_000)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                // 🔥 FIX CLOUDFlARE + LINUX
                .setConnectionReuseStrategy(NoConnectionReuseStrategy.INSTANCE)

                // 🔥 CHỈ HTTP/1.1
                .disableConnectionState()

                // 🔥 KHÔNG retry ngầm
                .disableAutomaticRetries()

                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        return new RestTemplate(factory);
    }
}
