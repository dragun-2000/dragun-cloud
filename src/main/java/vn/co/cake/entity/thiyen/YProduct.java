package vn.co.cake.entity.thiyen;

import lombok.Data;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.List;

@Data
@Entity
@Table(name = "thiyen_products")
public class YProduct {
    @Id
    @SequenceGenerator(name = "thiyen_products", sequenceName = "thiyen_products_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "thiyen_products_seq")
    private Long id;

    private String name;

    private Double price;

    @Column(name = "old_price")
    private Double oldPrice;

    @Column(name = "bulk_price")
    private Double bulkPrice;

    private Integer quantity;

    @Column(name = "bulk_quantity")
    private String bulkQuantity;

    private Integer discount;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "short_desc", columnDefinition = "TEXT")
    private String shortDesc;

    @Column(columnDefinition = "TEXT")
    private String benefits;

    @Column(name = "target_users", columnDefinition = "TEXT")
    private String targetUsers;

    @Column(columnDefinition = "TEXT")
    private String usage;

    private String manufacturer;

    @Column(columnDefinition = "TEXT")
    private String ingredients;

    @Column(name = "detailed_usage", columnDefinition = "TEXT")
    private String detailedUsage;

    @Column(columnDefinition = "TEXT")
    private String specifications;

    @Column(columnDefinition = "TEXT")
    private String technology;

    @Column(columnDefinition = "TEXT")
    private String storage;

    private String image;
    
    private String category;

    @Column(name = "is_new")
    private Boolean isNew;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductGallery> gallery;
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ProductVariant> variants;
    
    private boolean deleted;
}
