package vn.co.cake.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import java.util.Date;

/**
 * BaseEntity
 */
@Data
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public class BaseEntity {

    // 作成日
    @CreatedDate
    private Date created;

    // 作成者
    @CreatedBy
    @Column(length = 100)
    private String creator;

    // 更新日
    @LastModifiedDate
    private Date updated;

    // 更新者
    @LastModifiedBy
    @Column(length = 100)
    private String updater;
}
