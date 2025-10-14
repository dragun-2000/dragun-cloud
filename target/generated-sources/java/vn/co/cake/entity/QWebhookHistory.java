package vn.co.cake.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QWebhookHistory is a Querydsl query type for WebhookHistory
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QWebhookHistory extends EntityPathBase<WebhookHistory> {

    private static final long serialVersionUID = 1297411710L;

    public static final QWebhookHistory webhookHistory = new QWebhookHistory("webhookHistory");

    public final QBaseEntity _super = new QBaseEntity(this);

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    //inherited
    public final StringPath creator = _super.creator;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath payload = createString("payload");

    //inherited
    public final DateTimePath<java.util.Date> updated = _super.updated;

    //inherited
    public final StringPath updater = _super.updater;

    public QWebhookHistory(String variable) {
        super(WebhookHistory.class, forVariable(variable));
    }

    public QWebhookHistory(Path<? extends WebhookHistory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QWebhookHistory(PathMetadata metadata) {
        super(WebhookHistory.class, metadata);
    }

}

