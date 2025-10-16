package vn.co.cake.helper;

import com.sendgrid.Email;
import com.sendgrid.Content;
import com.sendgrid.Mail;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.co.cake.dto.GenericMailForm;
import vn.co.cake.enums.MailType;

import java.io.IOException;

@Slf4j
@Service
public class EmailService {

    private static final String FROM_EMAIL = "dragun.eran2000@gmail.com"; // phải là email đã xác minh trong SendGrid debase.1995@gmail.com
    private final SendGrid sendGridClient;

    public EmailService() {
        // Lấy API Key từ biến môi trường
        String apiKey = System.getenv("SENDGRID_API_KEY");
        this.sendGridClient = new SendGrid(apiKey);
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
     * Hàm xử lý gửi email qua SendGrid API
     */
    private void send(String emailTo, String subject, String textContent) {
        Email from = new Email(FROM_EMAIL);
        Email to = new Email(emailTo);
        Content content = new Content("text/plain", textContent);
        Mail mail = new Mail(from, subject, to, content);

        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sendGridClient.api(request);

            if (response.getStatusCode() == 202) {
                log.info("✅ Email sent successfully to {} with subject '{}'", emailTo, subject);
            } else {
                log.warn("⚠️ SendGrid returned code {}: {}", response.getStatusCode(), response.getBody());
            }
        } catch (IOException e) {
            log.error("❌ Failed to send email to {} - {}", emailTo, e.getMessage(), e);
        }
    }
}
