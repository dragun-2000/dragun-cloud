package vn.co.cake.helper;

import sibApi.TransactionalEmailsApi;
import sibModel.*;
import sendinblue.ApiClient;
import sendinblue.ApiException;
import sendinblue.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.co.cake.dto.GenericMailForm;
import vn.co.cake.enums.MailType;

import java.util.Collections;

@Slf4j
@Service
public class EmailService {

    private static final String FROM_EMAIL = "reply.debase@gmail.com"; // phải là email đã xác minh trong Brevo
    private final String brevoApiKey;

    public EmailService() {
        // Lấy API Key từ biến môi trường hoặc hardcode (tạm thời)
        String apiKey = System.getenv("BREVO_API_KEY");
        // if (apiKey == null || apiKey.isEmpty()) {
        //     // Fallback: có thể thêm vào application.properties sau
        //     // apiKey = apiKey; // Thay thế bằng API key thực tế
        // }
        this.brevoApiKey = apiKey;
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
     * Hàm xử lý gửi email qua Brevo API
     */
    private void send(String emailTo, String subject, String textContent) {
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
