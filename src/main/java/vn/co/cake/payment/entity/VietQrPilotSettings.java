package vn.co.cake.payment.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "vietqr_pilot_settings")
public class VietQrPilotSettings {

  public static final short SINGLETON_ID = 1;

  @Id
  private short id = SINGLETON_ID;

  @Column(name = "visible_to_all", nullable = false)
  private boolean visibleToAll = true;

  @Column(name = "pilot_password_hash", length = 255)
  private String pilotPasswordHash;

  private Date updated;

  @Column(length = 100)
  private String updater;
}
