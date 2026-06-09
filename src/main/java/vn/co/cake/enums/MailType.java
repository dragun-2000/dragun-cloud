package vn.co.cake.enums;

import vn.co.cake.common.StringConst;
import lombok.Getter;

/**
 * MailType — template HTML trong {@code templates/template/mail/}.
 */
@Getter
public enum MailType {

    SA_FORGET_PASSWORD(StringConst.SEND_MAIL_SA_FORGET_PASSWORD_TEMPLATE, StringConst.SEND_MAIL_SA_FORGET_PASSWORD_SUBJECT);

    private final String fileName;
    private final String subject;

    MailType(String fileName, String subject) {
        this.fileName = fileName;
        this.subject = subject;
    }
}
