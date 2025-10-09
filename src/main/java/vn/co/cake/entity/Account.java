package vn.co.cake.entity;

import com.querydsl.core.annotations.QueryEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import vn.co.cake.dto.CartForm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Account
 */
@Entity
@Data
@QueryEntity
@EqualsAndHashCode(callSuper = true)
@Table(name = "account")
public class Account extends BaseEntity {

    @Id
    @SequenceGenerator(name = "AccountId", sequenceName = "account_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_seq")
    private Long id;

    @Column(length = 100)
    private String fullName;

    private String password;

    @Column(length = 200)
    private String mailAddress;

    @Column(length = 11)
    private String phone;

    @Column(length = 200)
    private String province;

    @Column(length = 200)
    private String district;

    @Column(length = 200)
    private String ward;

    @Column(length = 200)
    private String floor;
    
    @Column(length = 200)
    private String address;

    private String accountStatus;

    private Boolean logout;

    @Column(length = 20)
    private String authorities;

    @Column(length = 100)
    private String hash;

    private boolean deleted;

    @Column(name = "first_login", columnDefinition = "false")
    private boolean firstLogin;

    public Account() {
    }
}
