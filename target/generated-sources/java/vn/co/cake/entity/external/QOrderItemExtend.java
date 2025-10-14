package vn.co.cake.entity.external;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QOrderItemExtend is a Querydsl query type for OrderItemExtend
 */
@Generated("com.querydsl.codegen.EmbeddableSerializer")
public class QOrderItemExtend extends BeanPath<OrderItemExtend> {

    private static final long serialVersionUID = -2061218631L;

    public static final QOrderItemExtend orderItemExtend = new QOrderItemExtend("orderItemExtend");

    public final StringPath imageUrl = createString("imageUrl");

    public final NumberPath<Double> price = createNumber("price", Double.class);

    public final StringPath productId = createString("productId");

    public final NumberPath<Integer> quantity = createNumber("quantity", Integer.class);

    public final NumberPath<Double> subtotal = createNumber("subtotal", Double.class);

    public QOrderItemExtend(String variable) {
        super(OrderItemExtend.class, forVariable(variable));
    }

    public QOrderItemExtend(Path<? extends OrderItemExtend> path) {
        super(path.getType(), path.getMetadata());
    }

    public QOrderItemExtend(PathMetadata metadata) {
        super(OrderItemExtend.class, metadata);
    }

}

