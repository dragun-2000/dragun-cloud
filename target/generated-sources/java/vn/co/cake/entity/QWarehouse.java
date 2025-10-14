package vn.co.cake.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QWarehouse is a Querydsl query type for Warehouse
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QWarehouse extends EntityPathBase<Warehouse> {

    private static final long serialVersionUID = -225380926L;

    public static final QWarehouse warehouse = new QWarehouse("warehouse");

    public final BooleanPath deleted = createBoolean("deleted");

    public final StringPath districtId = createString("districtId");

    public final StringPath fullAddress = createString("fullAddress");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final StringPath phoneNumber = createString("phoneNumber");

    public final StringPath provinceId = createString("provinceId");

    public QWarehouse(String variable) {
        super(Warehouse.class, forVariable(variable));
    }

    public QWarehouse(Path<? extends Warehouse> path) {
        super(path.getType(), path.getMetadata());
    }

    public QWarehouse(PathMetadata metadata) {
        super(Warehouse.class, metadata);
    }

}

