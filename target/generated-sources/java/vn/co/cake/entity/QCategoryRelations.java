package vn.co.cake.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QCategoryRelations is a Querydsl query type for CategoryRelations
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QCategoryRelations extends EntityPathBase<CategoryRelations> {

    private static final long serialVersionUID = -1482771816L;

    public static final QCategoryRelations categoryRelations = new QCategoryRelations("categoryRelations");

    public final QBaseEntity _super = new QBaseEntity(this);

    public final NumberPath<Long> categoryId = createNumber("categoryId", Long.class);

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    //inherited
    public final StringPath creator = _super.creator;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> productId = createNumber("productId", Long.class);

    //inherited
    public final DateTimePath<java.util.Date> updated = _super.updated;

    //inherited
    public final StringPath updater = _super.updater;

    public QCategoryRelations(String variable) {
        super(CategoryRelations.class, forVariable(variable));
    }

    public QCategoryRelations(Path<? extends CategoryRelations> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCategoryRelations(PathMetadata metadata) {
        super(CategoryRelations.class, metadata);
    }

}

