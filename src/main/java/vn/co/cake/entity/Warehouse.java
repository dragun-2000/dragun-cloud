package vn.co.cake.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@Entity
@Table(name = "warehouse")
@NoArgsConstructor
public class Warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "district_id")
    private String districtId;
    @Column(name = "full_address")
    private String fullAddress;
    @Column(name = "name")
    private String name;
    @Column(name = "phone_number")
    private String phoneNumber;
    @Column(name = "province_id")
    private String provinceId;

    private boolean deleted;
}
