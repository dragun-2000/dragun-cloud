package vn.co.cake.payment.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Data;
import vn.co.cake.entity.BaseEntity;
import vn.co.cake.entity.Order;
import vn.co.cake.entity.Variation;

@Data
@Entity
@Table(name = "inventory_reservation",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_reservation_order_variation",
                columnNames = {"order_id", "variation_id"}))
public class InventoryReservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variation_id", nullable = false)
    private Variation variation;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "expires_at", nullable = false)
    private Date expiresAt;
}
