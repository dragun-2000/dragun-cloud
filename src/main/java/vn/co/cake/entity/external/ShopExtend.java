package vn.co.cake.entity.external;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "shops_extend")
public class ShopExtend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private Long id; 
    
    @Column(name = "code", nullable = false)
    private String code; 

    @Column(name = "name", nullable = false)
    private String name; 

    @Column(name = "address", nullable = false)
    private String address; 
    
    @Column(name = "phone", nullable = false)
    private String phone; 
    
    @Column(name = "imageUrl", nullable = false)
    private String imageUrl; 
    
    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "created_at")
    private Date createdAt; 

    @Column(name = "updated_at")
    private Date updatedAt;
}

