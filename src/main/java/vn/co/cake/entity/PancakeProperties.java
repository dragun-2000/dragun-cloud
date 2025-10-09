package vn.co.cake.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "pancake_properties")
public class PancakeProperties extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String token;
    private String shopId;
    private String warehouseId;
    private String sellerId;
    private String pageId;
    private boolean deleted;
}
