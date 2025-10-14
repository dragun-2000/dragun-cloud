package vn.co.cake.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QOrderSequence is a Querydsl query type for OrderSequence
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QOrderSequence extends EntityPathBase<OrderSequence> {

    private static final long serialVersionUID = -1187161938L;

    public static final QOrderSequence orderSequence = new QOrderSequence("orderSequence");

    public final StringPath date = createString("date");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> maxOrder = createNumber("maxOrder", Integer.class);

    public QOrderSequence(String variable) {
        super(OrderSequence.class, forVariable(variable));
    }

    public QOrderSequence(Path<? extends OrderSequence> path) {
        super(path.getType(), path.getMetadata());
    }

    public QOrderSequence(PathMetadata metadata) {
        super(OrderSequence.class, metadata);
    }

}

