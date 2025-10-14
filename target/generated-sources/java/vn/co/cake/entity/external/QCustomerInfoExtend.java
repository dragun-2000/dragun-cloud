package vn.co.cake.entity.external;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QCustomerInfoExtend is a Querydsl query type for CustomerInfoExtend
 */
@Generated("com.querydsl.codegen.EmbeddableSerializer")
public class QCustomerInfoExtend extends BeanPath<CustomerInfoExtend> {

    private static final long serialVersionUID = -350573464L;

    public static final QCustomerInfoExtend customerInfoExtend = new QCustomerInfoExtend("customerInfoExtend");

    public final StringPath address = createString("address");

    public final StringPath name = createString("name");

    public final StringPath phone = createString("phone");

    public QCustomerInfoExtend(String variable) {
        super(CustomerInfoExtend.class, forVariable(variable));
    }

    public QCustomerInfoExtend(Path<? extends CustomerInfoExtend> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCustomerInfoExtend(PathMetadata metadata) {
        super(CustomerInfoExtend.class, metadata);
    }

}

