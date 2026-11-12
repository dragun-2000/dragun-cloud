package vn.co.cake.vnpay.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "vnpay_settings")
public class VnpaySettings {

    public static final short SINGLETON_ID = 1;

    @Id
    private short id = SINGLETON_ID;

    @Column(nullable = false)
    private boolean enabled = true;

    /** true = mọi user dùng VNPAY; false = chế độ pilot (cần mật khẩu). */
    @Column(name = "visible_to_all", nullable = false)
    private boolean visibleToAll = false;

    @Column(name = "pilot_password_hash", length = 255)
    private String pilotPasswordHash;

    private Date updated;

    @Column(length = 100)
    private String updater;
}
