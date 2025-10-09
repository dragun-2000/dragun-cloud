package vn.co.cake.controller.external;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.co.cake.helper.EmailService;
import vn.co.cake.service.aws.S3Service;

@RestController
@RequestMapping("/sms")
public class SMSController {

    private final EmailService emailService;
    private final S3Service s3Service;

    public SMSController(EmailService emailService, S3Service s3Service) {
        this.emailService = emailService;
        this.s3Service = s3Service;
    }

    @GetMapping("/send")
    public String sendSMS() {
        s3Service.sendSms("+84367209194", "test message");
        return "";
    }
}
