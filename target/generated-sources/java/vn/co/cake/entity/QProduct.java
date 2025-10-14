package vn.co.cake.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QProduct is a Querydsl query type for Product
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QProduct extends EntityPathBase<Product> {

    private static final long serialVersionUID = 1846557614L;

    public static final QProduct product = new QProduct("product");

    public final QBaseEntity _super = new QBaseEntity(this);

    public final StringPath categories = createString("categories");

    public final StringPath code = createString("code");

    public final StringPath colors = createString("colors");

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    //inherited
    public final StringPath creator = _super.creator;

    public final BooleanPath deleted = createBoolean("deleted");

    public final StringPath description = createString("description");

    public final StringPath descriptionSize = createString("descriptionSize");

    public final NumberPath<java.math.BigDecimal> discount = createNumber("discount", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> discountPrice = createNumber("discountPrice", java.math.BigDecimal.class);

    public final StringPath displayId = createString("displayId");

    public final NumberPath<java.math.BigDecimal> finalPrice = createNumber("finalPrice", java.math.BigDecimal.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath image = createString("image");

    public final StringPath image1 = createString("image1");

    public final StringPath image2 = createString("image2");

    public final StringPath image3 = createString("image3");

    public final StringPath image4 = createString("image4");

    public final StringPath image5 = createString("image5");

    public final StringPath image6 = createString("image6");

    public final StringPath image7 = createString("image7");

    public final StringPath image8 = createString("image8");

    public final StringPath image9 = createString("image9");

    public final StringPath name = createString("name");

    public final NumberPath<java.math.BigDecimal> price = createNumber("price", java.math.BigDecimal.class);

    public final StringPath productPancakeId = createString("productPancakeId");

    public final StringPath relatedProduct1 = createString("relatedProduct1");

    public final StringPath relatedProduct2 = createString("relatedProduct2");

    public final StringPath relatedProduct3 = createString("relatedProduct3");

    public final StringPath relatedProduct4 = createString("relatedProduct4");

    public final StringPath sizes = createString("sizes");

    public final NumberPath<Long> stockQuantity = createNumber("stockQuantity", Long.class);

    public final StringPath subCode = createString("subCode");

    //inherited
    public final DateTimePath<java.util.Date> updated = _super.updated;

    //inherited
    public final StringPath updater = _super.updater;

    public final StringPath variationId = createString("variationId");

    public QProduct(String variable) {
        super(Product.class, forVariable(variable));
    }

    public QProduct(Path<? extends Product> path) {
        super(path.getType(), path.getMetadata());
    }

    public QProduct(PathMetadata metadata) {
        super(Product.class, metadata);
    }

}

