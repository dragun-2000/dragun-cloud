package vn.co.cake.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "variations")
@NoArgsConstructor
public class Variation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    
    private String name;
    
    @Column(name = "display_id")
    private String displayId;
    
    @Column(name = "variation_id")
    private String variationId;
    
    @Column(name = "pancake_product_id")
    private String pancakeProductId;
    
    @Column(name = "retail_price")
    private Long retailPrice;
    
    @Column(name = "total_quantity")
    private Long totalQuantity;

    @Column(name = "remain_quantity")
    private Long remainQuantity;

    @Column(name = "actual_remain_quantity")
    private Long actualRemainQuantity;

    @Column(name = "waiting_quantity")
    private Long waitingQuantity;

    @Column(name = "returning_quantity")
    private Long returningQuantity;
    
    private String color;
    
    private String size;

    private String type;

    private String image;

    private boolean deleted;
}
