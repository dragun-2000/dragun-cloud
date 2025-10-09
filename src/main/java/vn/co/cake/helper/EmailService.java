package vn.co.cake.helper;

import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.co.cake.config.AWSS3Config;
import vn.co.cake.dto.GenericMailForm;
import vn.co.cake.enums.MailType;

@Slf4j
@Service
public class EmailService {

    private final AWSS3Config client;

    public EmailService(AWSS3Config client) {
        this.client = client;
    }

    public void sendEmail(String emailTo, GenericMailForm genericMailForm, MailType mailType) {
        Destination destination = new Destination().withToAddresses(emailTo);
        Content subject = new Content().withData(mailType.getSubject());
        String username = String.format("Tên người dùng: %s", genericMailForm.getAccountName());
        String phone = String.format("Số điện thoại: %s", genericMailForm.getPhone());
        String url = String.format("%s/SA/SA002/verify_user?accountId=%s&hash=%s", genericMailForm.getUrl(), genericMailForm.getKey(), genericMailForm.getHash());
        String content = String.format("%s\n%s\n%s\n%s", username, phone, "Vui lòng truy cập từ URL bên dưới và đặt lại mật khẩu của bạn.", url);
        Content body = new Content().withData(content);
        Body messageBody = new Body().withText(body);
        Message message = new Message().withSubject(subject).withBody(messageBody);

        String TO = "debase.1995@gmail.com";
        SendEmailRequest request = new SendEmailRequest().withSource(TO).withDestination(destination).withMessage(message);

        try {
            // Gửi email
            SendEmailResult result = client.sesClient().sendEmail(request);
            System.out.println("Email sent! Message ID: " + result.getMessageId());
        } catch (Exception e) {
            System.out.println("The email was not sent. Error message: " + e.getMessage());
        }
    }
    
    public void sendEmail(String emailTo, GenericMailForm genericMailForm) {
        Destination destination = new Destination().withToAddresses(emailTo);
        Content subject = new Content().withData("[ORDER] Có Đơn Hàng Mới");
        String username = String.format("Tên người dùng: %s", genericMailForm.getAccountName());
        String phone = String.format("Số điện thoại: %s", genericMailForm.getPhone());
        String url = String.format("%s/admin/orders", genericMailForm.getUrl());
        String content = String.format("%s\n%s", "Vui lòng truy cập từ URL bên dưới để xem chi tiết đơn hàng", url);
        Content body = new Content().withData(content);
        Body messageBody = new Body().withText(body);
        Message message = new Message().withSubject(subject).withBody(messageBody);

        String TO = "debase.1995@gmail.com";
        SendEmailRequest request = new SendEmailRequest().withSource(TO).withDestination(destination).withMessage(message);

        try {
            // Gửi email
            SendEmailResult result = client.sesClient().sendEmail(request);
            System.out.println("Email sent! Message ID: " + result.getMessageId());
        } catch (Exception e) {
            System.out.println("The email was not sent. Error message: " + e.getMessage());
        }
    }
}