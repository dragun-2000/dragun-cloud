package vn.co.cake.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

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
