package vn.co.cake.entity.external;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "orders_extend")
public class OrderExtend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type; // e.g., DELIVERY or RESTAURANT

    @ElementCollection
    private List<OrderItemExtend> items;

    @Embedded
    private CustomerInfoExtend customerInfo;

    private double total;
    private String status;
    private Date createdAt;
    private Date updatedAt;

    // Getters and setters
}
