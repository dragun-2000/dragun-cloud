package vn.co.cake.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QVariation is a Querydsl query type for Variation
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QVariation extends EntityPathBase<Variation> {

    private static final long serialVersionUID = 1690262802L;

    public static final QVariation variation = new QVariation("variation");

    public final QBaseEntity _super = new QBaseEntity(this);

    public final NumberPath<Long> actualRemainQuantity = createNumber("actualRemainQuantity", Long.class);

    public final StringPath color = createString("color");

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    //inherited
    public final StringPath creator = _super.creator;

    public final BooleanPath deleted = createBoolean("deleted");

    public final StringPath displayId = createString("displayId");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath image = createString("image");

    public final StringPath name = createString("name");

    public final StringPath pancakeProductId = createString("pancakeProductId");

    public final NumberPath<Long> remainQuantity = createNumber("remainQuantity", Long.class);

    public final NumberPath<Long> retailPrice = createNumber("retailPrice", Long.class);

    public final NumberPath<Long> returningQuantity = createNumber("returningQuantity", Long.class);

    public final StringPath size = createString("size");

    public final NumberPath<Long> totalQuantity = createNumber("totalQuantity", Long.class);

    //inherited
    public final DateTimePath<java.util.Date> updated = _super.updated;

    //inherited
    public final StringPath updater = _super.updater;

    public final StringPath variationId = createString("variationId");

    public final NumberPath<Long> waitingQuantity = createNumber("waitingQuantity", Long.class);

    public QVariation(String variable) {
        super(Variation.class, forVariable(variable));
    }

    public QVariation(Path<? extends Variation> path) {
        super(path.getType(), path.getMetadata());
    }

    public QVariation(PathMetadata metadata) {
        super(Variation.class, metadata);
    }

}

