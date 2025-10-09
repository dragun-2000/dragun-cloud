package vn.co.cake.entity;

import com.querydsl.core.annotations.QueryEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import java.util.Date;

@Entity
@Data
@QueryEntity
public class MailHistory {

    @Id
    @SequenceGenerator(name = "MailHistoryId", sequenceName = "mail_history_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mail_history_seq")
    private Long id;

    private Long masterConvertId;

    private Date sendTime;

    private String emailAddress;

    private String content;

    @Column(columnDefinition="BOOLEAN DEFAULT false")
    private boolean approve;

    private String userCustomerCode;
}
