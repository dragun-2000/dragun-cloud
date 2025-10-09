package vn.co.cake.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@Entity
@Table(name = "wards")
@NoArgsConstructor
public class Ward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String code;
    private String name;
    private String type;
    private String slug;
    @Column(name = "name_with_type")
    private String nameWithType;
    private String path;
    @Column(name = "path_with_type")
    private String pathWithType;
    @Column(name = "parent_code")
    private String parentCode;
}
