package vn.co.cake.entity.thiyen;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "product_gallery")
@Data
public class ProductGallery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_url")
    private String imageUrl;

    private Integer position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private YProduct product;
}
