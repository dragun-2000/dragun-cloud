package vn.co.cake.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SendEmail
 */
@Data
public class SendEmail {

    private List<String> to;
    private List<String> cc;
    private List<String> bcc;
    private String subject;
    private String body;

    public SendEmail(List<String> to, List<String> cc, List<String> bcc, String subject, String body) {
        this.to = Optional.ofNullable(to).orElse(new ArrayList<>());
        this.cc = Optional.ofNullable(cc).orElse(new ArrayList<>());
        this.bcc = Optional.ofNullable(bcc).orElse(new ArrayList<>());
        this.subject = subject;
        this.body = body;
    }
}
