package vn.co.cake.helper;

import sibApi.TransactionalEmailsApi;
import sibModel.*;
import sendinblue.ApiClient;
import sendinblue.ApiException;
import sendinblue.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.co.cake.dto.GenericMailForm;
import vn.co.cake.enums.MailType;

import java.util.Collections;

@Slf4j
@Service
public class EmailService {

    private static final String FROM_EMAIL = "reply@debase.vn"; // phải là email đã xác minh trong Brevo

    /**
     * PHẢI dùng API key (tab "API keys & MCP" trong Brevo), KHÔNG dùng SMTP key (tab SMTP).
     * SMTP key (xsmtpsib-...) chỉ dùng cho kết nối SMTP → gọi REST API với SMTP key sẽ bị 401.
     */
    private final String brevoApiKey;

    public EmailService(
            @Value("${brevo.api.key:}") String brevoApiKey) {
        this.brevoApiKey = brevoApiKey != null ? brevoApiKey.trim() : "";
        if (this.brevoApiKey.isEmpty()) {
            log.warn("brevo.api.key is empty - email sending will fail. Set in config or env BREVO_API_KEY. Use API key from Brevo: Settings > SMTP & API > API keys & MCP");
        }
    }

    /**
     * Gửi email xác minh tài khoản / đặt lại mật khẩu
     */
    public void sendEmail(String emailTo, GenericMailForm genericMailForm, MailType mailType) {
        String subject = mailType.getSubject();

        String username = String.format("Tên người dùng: %s", genericMailForm.getAccountName());
        String phone = String.format("Số điện thoại: %s", genericMailForm.getPhone());
        String url = String.format("%s/SA/SA002/verify_user?accountId=%s&hash=%s",
                genericMailForm.getUrl(), genericMailForm.getKey(), genericMailForm.getHash());

        String textContent = String.format("%s \n %s \n Vui lòng truy cập từ URL bên dưới và đặt lại mật khẩu của bạn: %s", username, phone, url);

        send(emailTo, subject, textContent);
    }

    /**
     * Gửi email thông báo đơn hàng mới
     */
    public void sendEmail(String emailTo, GenericMailForm genericMailForm) {
        String subject = "[ORDER] Có đơn hàng mới";
        String url = String.format("%s/admin/orders", genericMailForm.getUrl());

        String textContent = String.format("Vui lòng truy cập từ URL bên dưới để xem chi tiết đơn hàng:%s", url);

        send(emailTo, subject, textContent);
    }

    /**
     * Hàm xử lý gửi email qua Brevo REST API.
     * Yêu cầu: brevo.api.key phải là API key (lấy từ Brevo > Settings > SMTP & API > tab "API keys & MCP"), không phải SMTP key.
     */
    private void send(String emailTo, String subject, String textContent) {
        if (brevoApiKey == null || brevoApiKey.isEmpty()) {
            log.error("Cannot send email: brevo.api.key not set. Use API key from Brevo > API keys & MCP (not SMTP key).");
            return;
        }
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setApiKey(brevoApiKey);

        TransactionalEmailsApi apiInstance = new TransactionalEmailsApi(defaultClient);
        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();

        sendSmtpEmail.setSubject(subject);
        sendSmtpEmail.setTextContent(textContent);
        sendSmtpEmail.setSender(new SendSmtpEmailSender()
                .name("Debase")
                .email(FROM_EMAIL));
        sendSmtpEmail.setTo(Collections.singletonList(
                new SendSmtpEmailTo().email(emailTo)));

        try {
            CreateSmtpEmail response = apiInstance.sendTransacEmail(sendSmtpEmail);
            log.info("Email sent successfully to {} with subject '{}'. Message ID: {}", 
                    emailTo, subject, response.getMessageId());
        } catch (ApiException e) {
            log.error("Failed to send email to {} - Status: {}, Body: {}", 
                    emailTo, e.getCode(), e.getResponseBody(), e);
        }
    }
}
