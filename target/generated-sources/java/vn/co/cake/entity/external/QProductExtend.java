package vn.co.cake.entity.external;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QProductExtend is a Querydsl query type for ProductExtend
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QProductExtend extends EntityPathBase<ProductExtend> {

    private static final long serialVersionUID = -2014558425L;

    public static final QProductExtend productExtend = new QProductExtend("productExtend");

    public final StringPath category = createString("category");

    public final DateTimePath<java.util.Date> createdAt = createDateTime("createdAt", java.util.Date.class);

    public final StringPath description = createString("description");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageUrl = createString("imageUrl");

    public final StringPath name = createString("name");

    public final NumberPath<Double> price = createNumber("price", Double.class);

    public final NumberPath<Long> shopId = createNumber("shopId", Long.class);

    public final DateTimePath<java.util.Date> updatedAt = createDateTime("updatedAt", java.util.Date.class);

    public QProductExtend(String variable) {
        super(ProductExtend.class, forVariable(variable));
    }

    public QProductExtend(Path<? extends ProductExtend> path) {
        super(path.getType(), path.getMetadata());
    }

    public QProductExtend(PathMetadata metadata) {
        super(ProductExtend.class, metadata);
    }

}

