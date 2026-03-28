package vn.co.cake.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Chạy một GET tới Pancake POS (cùng URL kiểu curl) ngay sau khi app khởi động.
 * Chỉ bật khi: pancake.pos.diagnostic-on-startup=true
 *
 * Mục đích: phân biệt
 * - GET từ Java OK → vấn đề có thể chỉ ở POST createOrder (read timeout / server chậm).
 * - GET từ Java cũng lỗi → vấn đề ở kết nối HTTPS từ JVM (DNS, IPv6, TLS).
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "pancake.pos.diagnostic-on-startup", havingValue = "true")
public class PancakePosDiagnosticRunner implements ApplicationRunner {

    private final RestTemplate pancakeRestTemplate;
    private final String baseUrl;
    private final String shopId;
    private final String token;

    public PancakePosDiagnosticRunner(
            @Qualifier("pancakeRestTemplate") RestTemplate pancakeRestTemplate,
            @Value("${pancake.pos.api.url:}") String baseUrl,
            @Value("${pancake.pos.api.shop-id:}") String shopId,
            @Value("${pancake.pos.api.token:}") String token) {
        this.pancakeRestTemplate = pancakeRestTemplate;
        this.baseUrl = baseUrl;
        this.shopId = shopId;
        this.token = token;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (baseUrl == null || baseUrl.isEmpty() || shopId == null || shopId.isEmpty() || token == null || token.isEmpty()) {
            log.warn("Pancake POS diagnostic skipped: missing pancake.pos.api.url / shop-id / token");
            return;
        }
        String url = baseUrl + "/shops/" + shopId + "/orders?api_key=" + token + "&page_size=1&page_number=1";
        log.info("Pancake POS diagnostic: GET (same as curl) ...");

        long start = System.currentTimeMillis();
        try {
            var response = pancakeRestTemplate.getForEntity(url, String.class);
            long cost = System.currentTimeMillis() - start;
            if (response.getStatusCode() == HttpStatus.OK) {
                int bodyLen = response.getBody() != null ? response.getBody().length() : 0;
                log.info("Pancake POS diagnostic: SUCCESS. GET {} ms, status={}, bodyLength={}", cost, response.getStatusCode(), bodyLen);
            } else {
                log.warn("Pancake POS diagnostic: GET returned status={} in {} ms", response.getStatusCode(), cost);
            }
        } catch (ResourceAccessException e) {
            long cost = System.currentTimeMillis() - start;
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            String rootClass = root.getClass().getSimpleName();
            log.error("Pancake POS diagnostic: FAILED after {} ms. [{}] {}", cost, rootClass, root.getMessage());
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.error("Pancake POS diagnostic: FAILED after {} ms. {}", cost, e.getMessage(), e);
        }
    }
}
