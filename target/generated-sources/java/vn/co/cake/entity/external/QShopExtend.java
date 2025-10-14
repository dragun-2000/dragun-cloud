package vn.co.cake.entity.external;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QShopExtend is a Querydsl query type for ShopExtend
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QShopExtend extends EntityPathBase<ShopExtend> {

    private static final long serialVersionUID = 599085106L;

    public static final QShopExtend shopExtend = new QShopExtend("shopExtend");

    public final StringPath address = createString("address");

    public final StringPath category = createString("category");

    public final StringPath code = createString("code");

    public final DateTimePath<java.util.Date> createdAt = createDateTime("createdAt", java.util.Date.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageUrl = createString("imageUrl");

    public final StringPath name = createString("name");

    public final StringPath phone = createString("phone");

    public final DateTimePath<java.util.Date> updatedAt = createDateTime("updatedAt", java.util.Date.class);

    public QShopExtend(String variable) {
        super(ShopExtend.class, forVariable(variable));
    }

    public QShopExtend(Path<? extends ShopExtend> path) {
        super(path.getType(), path.getMetadata());
    }

    public QShopExtend(PathMetadata metadata) {
        super(ShopExtend.class, metadata);
    }

}

