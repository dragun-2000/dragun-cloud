package vn.co.cake.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QWard is a Querydsl query type for Ward
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QWard extends EntityPathBase<Ward> {

    private static final long serialVersionUID = -1046563811L;

    public static final QWard ward = new QWard("ward");

    public final StringPath code = createString("code");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final StringPath nameWithType = createString("nameWithType");

    public final StringPath parentCode = createString("parentCode");

    public final StringPath path = createString("path");

    public final StringPath pathWithType = createString("pathWithType");

    public final StringPath slug = createString("slug");

    public final StringPath type = createString("type");

    public QWard(String variable) {
        super(Ward.class, forVariable(variable));
    }

    public QWard(Path<? extends Ward> path) {
        super(path.getType(), path.getMetadata());
    }

    public QWard(PathMetadata metadata) {
        super(Ward.class, metadata);
    }

}

