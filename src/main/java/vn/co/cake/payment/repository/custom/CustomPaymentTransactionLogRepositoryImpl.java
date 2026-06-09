package vn.co.cake.payment.repository.custom;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;

import vn.co.cake.payment.PaymentConstants;
import vn.co.cake.payment.dto.PaymentOrderLogSummary;
import vn.co.cake.payment.entity.PaymentTransactionLog;
import vn.co.cake.payment.entity.QPaymentTransactionLog;
import vn.co.cake.repository.BaseRepository;
import vn.co.cake.request.PaymentLogSearchRequest;
import vn.co.cake.utils.DateUtil;

@Repository
public class CustomPaymentTransactionLogRepositoryImpl extends BaseRepository
        implements CustomPaymentTransactionLogRepository {

    private static final String GROUP_KEY_SQL =
            "COALESCE(NULLIF(TRIM(l.order_code), ''), NULLIF(TRIM(l.vietqr_order_id), ''))";

    private final EntityManager entityManager;

    public CustomPaymentTransactionLogRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Page<PaymentTransactionLog> findAllByCondition(PaymentLogSearchRequest searchRequest, Pageable pageable) {
        JPAQuery<PaymentTransactionLog> query = new JPAQuery<>(entityManager);
        QPaymentTransactionLog qLog = QPaymentTransactionLog.paymentTransactionLog;

        BooleanBuilder where = new BooleanBuilder();
        if (StringUtils.isNotEmpty(searchRequest.getOrderCode())) {
            where.and(qLog.orderCode.containsIgnoreCase(searchRequest.getOrderCode()));
        }
        if (StringUtils.isNotEmpty(searchRequest.getVietqrOrderId())) {
            where.and(qLog.vietqrOrderId.eq(searchRequest.getVietqrOrderId()));
        }
        if (StringUtils.isNotEmpty(searchRequest.getPaymentMethod())) {
            where.and(qLog.paymentMethod.eq(searchRequest.getPaymentMethod()));
        }
        if (StringUtils.isNotEmpty(searchRequest.getStatus())) {
            where.and(qLog.status.eq(searchRequest.getStatus()));
        }
        if (StringUtils.isNotEmpty(searchRequest.getEventType())) {
            where.and(qLog.eventType.eq(searchRequest.getEventType()));
        }
        if (Objects.nonNull(searchRequest.getLastUpdateDateFrom())) {
            where.and(qLog.created.after(searchRequest.getLastUpdateDateFrom()));
        }
        if (Objects.nonNull(searchRequest.getLastUpdateDateTo())) {
            Date toDate = DateUtil.addDays(searchRequest.getLastUpdateDateTo(), 1);
            where.and(qLog.created.before(toDate));
        }

        List<PaymentTransactionLog> rows = query.from(qLog)
                .where(where)
                .orderBy(qLog.created.desc())
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset())
                .fetch();

        long total = query.from(qLog).where(where).fetchCount();
        return new PageImpl<>(rows, pageable, total);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<PaymentOrderLogSummary> findOrderSummaryByCondition(PaymentLogSearchRequest searchRequest,
                                                                    Pageable pageable) {
        Map<String, Object> params = new HashMap<>();
        String where = buildNativeWhere(searchRequest, params);
        String statusFilter = StringUtils.trimToEmpty(searchRequest.getStatus());
        params.put("statusFilter", statusFilter);

        String having = " HAVING (CAST(:statusFilter AS varchar) = '' "
                + "OR (CAST(:statusFilter AS varchar) = 'FAILED' AND BOOL_OR(l.status = '"
                + PaymentConstants.LOG_STATUS_FAILED + "')) "
                + "OR (CAST(:statusFilter AS varchar) = 'SUCCESS' AND NOT BOOL_OR(l.status = '"
                + PaymentConstants.LOG_STATUS_FAILED + "') AND BOOL_OR(l.status = '"
                + PaymentConstants.LOG_STATUS_SUCCESS + "'))) ";

        String countSql = "SELECT COUNT(*) FROM (SELECT 1 FROM payment_transaction_log l WHERE " + where
                + " AND " + GROUP_KEY_SQL + " IS NOT NULL GROUP BY " + GROUP_KEY_SQL + having + ") cnt";

        String dataSql = "SELECT " + GROUP_KEY_SQL + " AS group_key, "
                + "MAX(l.created) AS last_created, "
                + "MAX(CASE WHEN l.order_code IS NOT NULL AND TRIM(l.order_code) <> '' THEN l.order_code END) AS order_code, "
                + "MAX(l.vietqr_order_id) AS vietqr_order_id, "
                + "MAX(l.payment_method) AS payment_method, "
                + "MAX(l.amount) AS amount, "
                + "BOOL_OR(l.status = '" + PaymentConstants.LOG_STATUS_FAILED + "') AS has_failed, "
                + "BOOL_OR(l.status = '" + PaymentConstants.LOG_STATUS_SUCCESS + "') AS has_success, "
                + "STRING_AGG(DISTINCT l.error_message, ' | ') FILTER (WHERE l.status = '"
                + PaymentConstants.LOG_STATUS_FAILED + "' AND l.error_message IS NOT NULL "
                + "AND TRIM(l.error_message) <> '') AS error_message, "
                + "MAX(l.id) FILTER (WHERE l.event_type IN ('" + PaymentConstants.EVENT_ORDER_CREATED_AFTER_PAYMENT
                + "', '" + PaymentConstants.EVENT_CHECKOUT_COD + "', '"
                + PaymentConstants.EVENT_VIETQR_WEBHOOK_SYNC + "')) AS detail_log_id "
                + "FROM payment_transaction_log l WHERE " + where + " AND " + GROUP_KEY_SQL + " IS NOT NULL "
                + "GROUP BY " + GROUP_KEY_SQL + having + " ORDER BY last_created DESC";

        Query countQuery = entityManager.createNativeQuery(countSql);
        Query dataQuery = entityManager.createNativeQuery(dataSql);
        bindParams(countQuery, params);
        bindParams(dataQuery, params);
        dataQuery.setFirstResult((int) pageable.getOffset());
        dataQuery.setMaxResults(pageable.getPageSize());

        long total = ((Number) countQuery.getSingleResult()).longValue();
        List<Object[]> rows = dataQuery.getResultList();
        List<PaymentOrderLogSummary> summaries = new ArrayList<>();
        for (Object[] row : rows) {
            summaries.add(mapSummaryRow(row));
        }
        return new PageImpl<>(summaries, pageable, total);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PaymentTransactionLog> findAllByGroupKey(String groupKey) {
        if (StringUtils.isBlank(groupKey)) {
            return new ArrayList<>();
        }
        return entityManager.createNativeQuery(
                        "SELECT l.* FROM payment_transaction_log l WHERE " + GROUP_KEY_SQL + " = :groupKey "
                                + "ORDER BY l.created ASC",
                        PaymentTransactionLog.class)
                .setParameter("groupKey", groupKey.trim())
                .getResultList();
    }

    private static String buildNativeWhere(PaymentLogSearchRequest searchRequest, Map<String, Object> params) {
        StringBuilder where = new StringBuilder("1=1");
        if (StringUtils.isNotEmpty(searchRequest.getOrderCode())) {
            where.append(" AND l.order_code ILIKE :orderCode");
            params.put("orderCode", "%" + searchRequest.getOrderCode().trim() + "%");
        }
        if (StringUtils.isNotEmpty(searchRequest.getVietqrOrderId())) {
            where.append(" AND l.vietqr_order_id = :vietqrOrderId");
            params.put("vietqrOrderId", searchRequest.getVietqrOrderId().trim());
        }
        if (StringUtils.isNotEmpty(searchRequest.getPaymentMethod())) {
            where.append(" AND l.payment_method = :paymentMethod");
            params.put("paymentMethod", searchRequest.getPaymentMethod().trim());
        }
        if (StringUtils.isNotEmpty(searchRequest.getEventType())) {
            where.append(" AND l.event_type = :eventType");
            params.put("eventType", searchRequest.getEventType().trim());
        }
        if (Objects.nonNull(searchRequest.getLastUpdateDateFrom())) {
            where.append(" AND l.created >= :dateFrom");
            params.put("dateFrom", searchRequest.getLastUpdateDateFrom());
        }
        if (Objects.nonNull(searchRequest.getLastUpdateDateTo())) {
            where.append(" AND l.created < :dateTo");
            params.put("dateTo", DateUtil.addDays(searchRequest.getLastUpdateDateTo(), 1));
        }
        return where.toString();
    }

    private static void bindParams(Query query, Map<String, Object> params) {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
    }

    private static PaymentOrderLogSummary mapSummaryRow(Object[] row) {
        String groupKey = row[0] != null ? row[0].toString() : null;
        Date lastCreated = toDate(row[1]);
        String orderCode = row[2] != null ? row[2].toString() : null;
        String vietqrOrderId = row[3] != null ? row[3].toString() : null;
        String paymentMethod = row[4] != null ? row[4].toString() : null;
        BigDecimal amount = row[5] != null ? new BigDecimal(row[5].toString()) : null;
        boolean hasFailed = Boolean.TRUE.equals(row[6]) || "t".equalsIgnoreCase(String.valueOf(row[6]));
        boolean hasSuccess = Boolean.TRUE.equals(row[7]) || "t".equalsIgnoreCase(String.valueOf(row[7]));
        String errorMessage = row[8] != null ? row[8].toString() : null;
        Long detailLogId = row[9] != null ? ((Number) row[9]).longValue() : null;

        String status;
        if (hasFailed) {
            status = PaymentConstants.LOG_STATUS_FAILED;
        } else if (hasSuccess) {
            status = PaymentConstants.LOG_STATUS_SUCCESS;
        } else {
            status = PaymentConstants.LOG_STATUS_INFO;
        }

        return PaymentOrderLogSummary.builder()
                .groupKey(groupKey)
                .lastCreated(lastCreated)
                .orderCode(StringUtils.isNotBlank(orderCode) ? orderCode : groupKey)
                .vietqrOrderId(vietqrOrderId)
                .paymentMethod(paymentMethod)
                .status(status)
                .amount(amount)
                .errorMessage(errorMessage)
                .detailLogId(detailLogId)
                .build();
    }

    private static Date toDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof Timestamp) {
            return new Date(((Timestamp) value).getTime());
        }
        return null;
    }
}
