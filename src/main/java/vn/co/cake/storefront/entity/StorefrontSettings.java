package vn.co.cake.storefront.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "storefront_settings")
public class StorefrontSettings {

    public static final short SINGLETON_ID = 1;

    @Id
    private short id = SINGLETON_ID;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_menu_style", length = 30)
    private SaleMenuStyle saleMenuStyle = SaleMenuStyle.SALE;

    private Date updated;

    @Column(length = 100)
    private String updater;
}
