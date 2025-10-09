package vn.co.cake.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@Entity
@Table(name = "category_relations")
@NoArgsConstructor
public class CategoryRelations extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private Long productId;
    private Long categoryId;
}
