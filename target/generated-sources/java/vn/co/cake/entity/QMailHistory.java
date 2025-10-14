package vn.co.cake.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QMailHistory is a Querydsl query type for MailHistory
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QMailHistory extends EntityPathBase<MailHistory> {

    private static final long serialVersionUID = -1159994852L;

    public static final QMailHistory mailHistory = new QMailHistory("mailHistory");

    public final BooleanPath approve = createBoolean("approve");

    public final StringPath content = createString("content");

    public final StringPath emailAddress = createString("emailAddress");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> masterConvertId = createNumber("masterConvertId", Long.class);

    public final DateTimePath<java.util.Date> sendTime = createDateTime("sendTime", java.util.Date.class);

    public final StringPath userCustomerCode = createString("userCustomerCode");

    public QMailHistory(String variable) {
        super(MailHistory.class, forVariable(variable));
    }

    public QMailHistory(Path<? extends MailHistory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMailHistory(PathMetadata metadata) {
        super(MailHistory.class, metadata);
    }

}

