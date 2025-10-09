package vn.co.cake.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "vouchers")
@NoArgsConstructor
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String code;
    
    private String name;
    
    @Column(name = "discount_percent")
    private Integer discountPercent;
    
    @Column(name = "shipping_fee")
    private Integer shippingFee;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "deleted", columnDefinition = "false")
    private boolean deleted;
}
