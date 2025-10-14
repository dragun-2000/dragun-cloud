package vn.co.cake.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QAccount is a Querydsl query type for Account
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QAccount extends EntityPathBase<Account> {

    private static final long serialVersionUID = 978712812L;

    public static final QAccount account = new QAccount("account");

    public final QBaseEntity _super = new QBaseEntity(this);

    public final StringPath accountStatus = createString("accountStatus");

    public final StringPath address = createString("address");

    public final StringPath authorities = createString("authorities");

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    //inherited
    public final StringPath creator = _super.creator;

    public final BooleanPath deleted = createBoolean("deleted");

    public final StringPath district = createString("district");

    public final BooleanPath firstLogin = createBoolean("firstLogin");

    public final StringPath floor = createString("floor");

    public final StringPath fullName = createString("fullName");

    public final StringPath hash = createString("hash");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath logout = createBoolean("logout");

    public final StringPath mailAddress = createString("mailAddress");

    public final StringPath password = createString("password");

    public final StringPath phone = createString("phone");

    public final StringPath province = createString("province");

    //inherited
    public final DateTimePath<java.util.Date> updated = _super.updated;

    //inherited
    public final StringPath updater = _super.updater;

    public final StringPath ward = createString("ward");

    public QAccount(String variable) {
        super(Account.class, forVariable(variable));
    }

    public QAccount(Path<? extends Account> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAccount(PathMetadata metadata) {
        super(Account.class, metadata);
    }

}

