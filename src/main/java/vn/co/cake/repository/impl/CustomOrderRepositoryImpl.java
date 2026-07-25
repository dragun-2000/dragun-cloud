package vn.co.cake.repository.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import vn.co.cake.entity.Order;
import vn.co.cake.entity.QOrder;
import vn.co.cake.enums.OrderStatus;
import vn.co.cake.repository.BaseRepository;
import vn.co.cake.repository.CustomOrderRepository;
import vn.co.cake.request.SearchRequest;
import vn.co.cake.utils.DateUtil;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Repository
public class CustomOrderRepositoryImpl extends BaseRepository implements CustomOrderRepository {
    private final EntityManager entityManager;

    public CustomOrderRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Page<Order> findAllByCondition(SearchRequest searchRequest, Pageable pageable) {
        JPAQuery<Order> query = new JPAQuery<>(entityManager);
        QOrder qOrder = QOrder.order;

        BooleanBuilder where = new BooleanBuilder();
        where.and(qOrder.deleted.eq(false));
        if (StringUtils.isNotEmpty(searchRequest.getCode())) {
            where.and(qOrder.code.containsIgnoreCase(searchRequest.getCode()));
        }
        if (StringUtils.isNotEmpty(searchRequest.getEmail())) {
            where.and(qOrder.account.mailAddress.containsIgnoreCase(searchRequest.getEmail()));
        }
        if (StringUtils.isNotEmpty(searchRequest.getStatus())) {
            where.and(qOrder.status.eq(searchRequest.getStatus()));
        }
        if (Objects.nonNull(searchRequest.getLastUpdateDateFrom())) {
            where.and(qOrder.created.after(searchRequest.getLastUpdateDateFrom()));
        }
        if (Objects.nonNull(searchRequest.getLastUpdateDateTo())) {
            Date toDate = DateUtil.addDays(searchRequest.getLastUpdateDateTo(), 1);
            where.and(qOrder.created.before(toDate));
        }

        List<Order> orders = query.from(qOrder)
                .where(where)
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset())
                .orderBy(qOrder.code.desc())
                .fetch();

        long total = new JPAQuery<>(entityManager)
                .from(qOrder)
                .where(where)
                .fetchCount();

        return new PageImpl<>(orders, pageable, total);
    }

    @Override
    public Page<Order> findAllSyncFailOrders(SearchRequest searchRequest, Pageable pageable) {
        JPAQuery<Order> query = new JPAQuery<>(entityManager);
        QOrder qOrder = QOrder.order;

        BooleanBuilder where = new BooleanBuilder();
        where.and(qOrder.deleted.eq(false));
        where.and(qOrder.status.in(
                OrderStatus.SYNC_FAIL.getValue(),
                OrderStatus.PAYMENT_RECEIVED_UNFULFILLABLE.getValue()));
        
        if (StringUtils.isNotEmpty(searchRequest.getCode())) {
            where.and(qOrder.code.containsIgnoreCase(searchRequest.getCode()));
        }
        if (StringUtils.isNotEmpty(searchRequest.getEmail())) {
            where.and(qOrder.account.mailAddress.containsIgnoreCase(searchRequest.getEmail()));
        }
        if (Objects.nonNull(searchRequest.getLastUpdateDateFrom())) {
            where.and(qOrder.created.after(searchRequest.getLastUpdateDateFrom()));
        }
        if (Objects.nonNull(searchRequest.getLastUpdateDateTo())) {
            Date toDate = DateUtil.addDays(searchRequest.getLastUpdateDateTo(), 1);
            where.and(qOrder.created.before(toDate));
        }

        List<Order> orders = query.from(qOrder)
                .where(where)
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset())
                .orderBy(qOrder.code.desc())
                .fetch();

        long total = new JPAQuery<>(entityManager)
                .from(qOrder)
                .where(where)
                .fetchCount();

        return new PageImpl<>(orders, pageable, total);
    }
}
