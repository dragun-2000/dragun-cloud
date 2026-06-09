package vn.co.cake.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NoHttpResponseException;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import vn.co.cake.dto.GenericMailForm;
import vn.co.cake.dto.PasswordResetMailModel;
import vn.co.cake.enums.MailType;

@Slf4j
@Service
public class EmailService {

    private static final String FROM_EMAIL = "reply@debase.vn"; // phải là email đã xác minh trong Brevo
    private static final String DEFAULT_STORE_URL = "https://debase.vn";

    /**
     * PHẢI dùng API key (tab "API keys & MCP" trong Brevo), KHÔNG dùng SMTP key (tab SMTP).
     * SMTP key (xsmtpsib-...) chỉ dùng cho kết nối SMTP → gọi REST API với SMTP key sẽ bị 401.
     */
    private final String brevoApiKey;
    private final String brevoApiBaseUrl;
    private final String brevoEmailEndpoint;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final TemplateEngine mailTemplateEngine;

    public EmailService(
            @Value("${brevo.api.key:}") String brevoApiKey,
            @Value("${brevo.api.base-url:https://api.brevo.com}") String brevoApiBaseUrl,
            @Value("${brevo.api.connect-timeout-ms:10000}") int connectTimeoutMs,
            @Value("${brevo.api.read-timeout-ms:60000}") int readTimeoutMs,
            RestTemplateBuilder restTemplateBuilder,
            @Qualifier("mailTemplateEngine") TemplateEngine mailTemplateEngine) {
        this.brevoApiKey = brevoApiKey != null ? brevoApiKey.trim() : "";
        this.brevoApiBaseUrl = brevoApiBaseUrl != null ? brevoApiBaseUrl.trim() : "https://api.brevo.com";
        this.brevoEmailEndpoint = this.brevoApiBaseUrl + "/v3/smtp/email";

        CloseableHttpClient apacheHttpClient = HttpClientBuilder.create()
                // Retry đúng 1 lần nếu server đóng socket trước khi phản hồi.
                .setRetryHandler((exception, executionCount, context) ->
                        executionCount <= 2 && exception instanceof NoHttpResponseException)
                // Tránh reuse stale keep-alive connection gây NoHttpResponseException.
                .setConnectionReuseStrategy(NoConnectionReuseStrategy.INSTANCE)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(apacheHttpClient);
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);

        this.restTemplate = restTemplateBuilder
                .requestFactory(() -> requestFactory)
                .build();
        this.objectMapper = new ObjectMapper();
        this.mailTemplateEngine = mailTemplateEngine;
        if (this.brevoApiKey.isEmpty()) {
            log.warn("brevo.api.key is empty - email sending will fail. Set in config or env BREVO_API_KEY. Use API key from Brevo: Settings > SMTP & API > API keys & MCP");
        } else {
            log.info("Brevo RestTemplate initialized: endpoint={}, connect={}ms, read={}ms",
                    this.brevoEmailEndpoint, connectTimeoutMs, readTimeoutMs);
        }
    }

    /**
     * Gửi email xác minh tài khoản / đặt lại mật khẩu
     */
    public boolean sendEmail(String emailTo, GenericMailForm genericMailForm, MailType mailType) {
        String subject = mailType.getSubject();
        String resetUrl = buildPasswordResetUrl(genericMailForm);
        String username = String.format("Tên người dùng: %s", genericMailForm.getAccountName());
        String phone = String.format("Số điện thoại: %s", genericMailForm.getPhone());
        String textContent = String.format(
                "%s \n %s \n Vui lòng truy cập từ URL bên dưới và đặt lại mật khẩu của bạn: %s",
                username, phone, resetUrl);

        if (mailType == MailType.SA_FORGET_PASSWORD) {
            try {
                String storeUrl = normalizeStoreUrl(genericMailForm.getUrl());
                PasswordResetMailModel model = PasswordResetMailModel.builder()
                        .accountName(StringUtils.defaultIfBlank(genericMailForm.getAccountName(), "Quý khách"))
                        .phone(StringUtils.defaultIfBlank(genericMailForm.getPhone(), "—"))
                        .resetUrl(resetUrl)
                        .storeUrl(storeUrl)
                        .storeDisplayUrl("debase.vn")
                        .build();
                Context context = new Context(Locale.forLanguageTag("vi"));
                context.setVariable("mail", model);
                String html = mailTemplateEngine.process(mailType.getFileName(), context);
                return send(emailTo, subject, textContent, html);
            } catch (Exception ex) {
                log.warn("Render password reset HTML failed, fallback to plain text: {}", ex.getMessage());
            }
        }
        return send(emailTo, subject, textContent, null);
    }

    private static String buildPasswordResetUrl(GenericMailForm genericMailForm) {
        String base = StringUtils.trimToEmpty(genericMailForm.getUrl());
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return String.format("%s/SA/SA002/verify_user?accountId=%s&hash=%s",
                base, genericMailForm.getKey(), genericMailForm.getHash());
    }

    private static String normalizeStoreUrl(String url) {
        String base = StringUtils.isNotBlank(url) ? url.trim() : DEFAULT_STORE_URL;
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            base = "https://" + base;
        }
        return base;
    }

    /**
     * Gửi email thông báo đơn hàng mới (admin).
     */
    public boolean sendEmail(String emailTo, GenericMailForm genericMailForm) {
        String subject = "[ORDER] Có đơn hàng mới";
        String url = String.format("%s/admin/orders", genericMailForm.getUrl());

        String textContent = String.format("Vui lòng truy cập từ URL bên dưới để xem chi tiết đơn hàng:%s", url);

        return send(emailTo, subject, textContent, null);
    }

    /**
     * Gửi email text tùy biến (dùng cho cảnh báo kỹ thuật).
     */
    public boolean sendPlainTextEmail(String emailTo, String subject, String textContent) {
        String safeSubject = subject != null && !subject.isBlank() ? subject : "System notification";
        return send(emailTo, safeSubject, textContent, null);
    }

    /**
     * Gửi email HTML (có bản text dự phòng cho client không hỗ trợ HTML).
     */
    public boolean sendHtmlEmail(String emailTo, String subject, String htmlContent, String textContent) {
        String safeSubject = subject != null && !subject.isBlank() ? subject : "Debase notification";
        String safeHtml = htmlContent != null && !htmlContent.isBlank() ? htmlContent : "<p></p>";
        String safeText = textContent != null && !textContent.isBlank()
                ? textContent
                : buildSafePlainTextContent(htmlContent);
        return send(emailTo, safeSubject, safeText, safeHtml);
    }

    /**
     * Hàm xử lý gửi email qua Brevo REST API.
     * Yêu cầu: brevo.api.key phải là API key (lấy từ Brevo > Settings > SMTP & API > tab "API keys & MCP"), không phải SMTP key.
     */
    private boolean send(String emailTo, String subject, String textContent, String htmlContent) {
        if (brevoApiKey == null || brevoApiKey.isEmpty()) {
            log.error("Cannot send email: brevo.api.key not set. Use API key from Brevo > API keys & MCP (not SMTP key).");
            return false;
        }

        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("subject", subject);
            payload.put("textContent", textContent);
            if (htmlContent != null && !htmlContent.isBlank()) {
                payload.put("htmlContent", htmlContent);
            }
            ObjectNode sender = payload.putObject("sender");
            sender.put("name", "Debase");
            sender.put("email", FROM_EMAIL);
            ArrayNode to = payload.putArray("to");
            ObjectNode toItem = to.addObject();
            toItem.put("email", emailTo);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(brevoEmailEndpoint, entity, String.class);
            HttpStatus status = response.getStatusCode();
            if (status.is2xxSuccessful()) {
                log.info("Email sent successfully to {} with subject '{}'. Status={}",
                        emailTo, subject, status.value());
                return true;
            }
            log.error("Failed to send email to {} - Status: {}, Body: {}",
                    emailTo, status.value(), response.getBody());
            return false;
        } catch (RestClientException e) {
            Throwable root = getRootCause(e);
            log.error("Transport error sending email to {} - RootCause: {}",
                    emailTo, root != null ? root.toString() : "n/a", e);
            return false;
        } catch (Exception e) {
            Throwable root = getRootCause(e);
            log.error("Unexpected error when sending email to {} - RootCause: {}",
                    emailTo, root != null ? root.toString() : "n/a", e);
            return false;
        }
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private String buildSafePlainTextContent(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return "No content.";
        }
        String normalized = rawContent.replace("\r\n", "\n").trim();
        int maxLen = 3500;
        if (normalized.length() <= maxLen) {
            return normalized;
        }
        return normalized.substring(0, maxLen) + "\n\n...[truncated]";
    }
}
