package vn.co.cake.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QPancakeProperties is a Querydsl query type for PancakeProperties
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QPancakeProperties extends EntityPathBase<PancakeProperties> {

    private static final long serialVersionUID = -304831641L;

    public static final QPancakeProperties pancakeProperties = new QPancakeProperties("pancakeProperties");

    public final QBaseEntity _super = new QBaseEntity(this);

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    //inherited
    public final StringPath creator = _super.creator;

    public final BooleanPath deleted = createBoolean("deleted");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath pageId = createString("pageId");

    public final StringPath sellerId = createString("sellerId");

    public final StringPath shopId = createString("shopId");

    public final StringPath token = createString("token");

    //inherited
    public final DateTimePath<java.util.Date> updated = _super.updated;

    //inherited
    public final StringPath updater = _super.updater;

    public final StringPath warehouseId = createString("warehouseId");

    public QPancakeProperties(String variable) {
        super(PancakeProperties.class, forVariable(variable));
    }

    public QPancakeProperties(Path<? extends PancakeProperties> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPancakeProperties(PathMetadata metadata) {
        super(PancakeProperties.class, metadata);
    }

}

