package vn.co.cake.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QDistrict is a Querydsl query type for District
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QDistrict extends EntityPathBase<District> {

    private static final long serialVersionUID = -1593533265L;

    public static final QDistrict district = new QDistrict("district");

    public final StringPath code = createString("code");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final StringPath nameWithType = createString("nameWithType");

    public final StringPath parentCode = createString("parentCode");

    public final StringPath path = createString("path");

    public final StringPath pathWithType = createString("pathWithType");

    public final StringPath slug = createString("slug");

    public final StringPath type = createString("type");

    public QDistrict(String variable) {
        super(District.class, forVariable(variable));
    }

    public QDistrict(Path<? extends District> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDistrict(PathMetadata metadata) {
        super(District.class, metadata);
    }

}

