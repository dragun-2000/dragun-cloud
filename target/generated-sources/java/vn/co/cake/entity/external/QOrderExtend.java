package vn.co.cake.entity.external;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOrderExtend is a Querydsl query type for OrderExtend
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QOrderExtend extends EntityPathBase<OrderExtend> {

    private static final long serialVersionUID = -1437403706L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOrderExtend orderExtend = new QOrderExtend("orderExtend");

    public final DateTimePath<java.util.Date> createdAt = createDateTime("createdAt", java.util.Date.class);

    public final QCustomerInfoExtend customerInfo;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ListPath<OrderItemExtend, QOrderItemExtend> items = this.<OrderItemExtend, QOrderItemExtend>createList("items", OrderItemExtend.class, QOrderItemExtend.class, PathInits.DIRECT2);

    public final StringPath status = createString("status");

    public final NumberPath<Double> total = createNumber("total", Double.class);

    public final StringPath type = createString("type");

    public final DateTimePath<java.util.Date> updatedAt = createDateTime("updatedAt", java.util.Date.class);

    public QOrderExtend(String variable) {
        this(OrderExtend.class, forVariable(variable), INITS);
    }

    public QOrderExtend(Path<? extends OrderExtend> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOrderExtend(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOrderExtend(PathMetadata metadata, PathInits inits) {
        this(OrderExtend.class, metadata, inits);
    }

    public QOrderExtend(Class<? extends OrderExtend> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.customerInfo = inits.isInitialized("customerInfo") ? new QCustomerInfoExtend(forProperty("customerInfo")) : null;
    }

}

