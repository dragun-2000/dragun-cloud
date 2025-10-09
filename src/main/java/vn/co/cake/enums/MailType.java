package vn.co.cake.enums;

import vn.co.cake.common.StringConst;
import lombok.Getter;

/**
 * MailType
 */
@Getter
public enum MailType {

    //AM
    CREATE_ACCOUNT(StringConst.SEND_MAIL_CREATE_ACCOUNT_TEMPLATE, StringConst.SEND_MAIL_CREATE_ACCOUNT_SUBJECT),
    CREATE_ACCOUNT_SA(StringConst.SEND_MAIL_CREATE_ACCOUNT_SA_TEMPLATE, StringConst.SEND_MAIL_CREATE_ACCOUNT_SA_SUBJECT),
    CHANGE_PASSWORD(StringConst.SEND_MAIL_CHANGE_PASSWORD_TEMPLATE, StringConst.SEND_MAIL_CHANGE_PASSWORD_SUBJECT),
    FORGET_PASSWORD(StringConst.SEND_MAIL_FORGET_PASSWORD_TEMPLATE, StringConst.SEND_MAIL_FORGET_PASSWORD_SUBJECT),
    SA_FORGET_PASSWORD(StringConst.SEND_MAIL_SA_FORGET_PASSWORD_TEMPLATE, StringConst.SEND_MAIL_SA_FORGET_PASSWORD_SUBJECT);

    private final String fileName;
    private final String subject;

    MailType(String fileName, String subject) {
        this.fileName = fileName;
        this.subject = subject;
    }
}
