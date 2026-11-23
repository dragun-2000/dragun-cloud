package vn.co.cake.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.co.cake.entity.Order;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, CustomOrderRepository {

    @Query("SELECT DISTINCT o FROM Order o "
            + "LEFT JOIN FETCH o.orderItems oi "
            + "LEFT JOIN FETCH oi.variation "
            + "WHERE o.id = :id AND o.deleted = false")
    Optional<Order> findWithItemsAndVariationsById(@Param("id") Long id);
    Order findFirstByCode(String code);
    Order findFirstByAccountIdOrderByCreatedDesc(Long accountId);
    List<Order> findByStatusAndCountErrorLessThanAndDeletedFalse(String status, int maxCountError);
    List<Order> findByPhoneAndDeletedFalseOrderByCreatedDesc(String phone);

    /** Hiện đơn đang xử lý + đơn đã hủy sau khi lên Pancake; ẩn hết hạn thanh toán (CANCELLED chưa có pancake_order_id). */
    @Query("select o from Order o where o.phone = :phone and o.deleted = false "
            + "and o.status <> 'AWAITING_PAYMENT' "
            + "and (o.status <> 'CANCELLED' or o.pancakeOrderId is not null) "
            + "order by o.created desc")
    List<Order> findVisibleOrderHistoryByPhone(@Param("phone") String phone);

    Optional<Order> findFirstByCodeAndAccountIdAndDeletedFalse(String code, Long accountId);

    @Query(
        value = "SELECT * FROM orders o WHERE o.deleted = false "
                + "AND o.status NOT IN ('AWAITING_PAYMENT', 'CANCELLED') "
                + "AND o.phone IS NOT NULL AND TRIM(o.phone) <> '' "
                + "AND (o.pancake_status_name IS NULL OR TRIM(o.pancake_status_name) = '' "
                + "OR o.shipping_partner IS NULL OR TRIM(o.shipping_partner) = '') "
                + "ORDER BY o.created DESC LIMIT :limit",
        nativeQuery = true
    )
    List<Order> findOrdersNeedingPancakeEnrichment(@Param("limit") int limit);

    /**
     * Job003: poll khi name blank, name thuộc Debase hourly values, hoặc code thuộc hourly codes.
     * {@code names}/{@code codes} lấy từ {@code PancakeFulfillmentStatus} — không hardcode lệch.
     */
    @Query(
        value = "SELECT * FROM orders o WHERE o.deleted = false "
                + "AND o.status NOT IN ('AWAITING_PAYMENT', 'CANCELLED') "
                + "AND o.phone IS NOT NULL AND TRIM(o.phone) <> '' "
                + "AND ("
                + "o.pancake_status_name IS NULL OR TRIM(o.pancake_status_name) = '' "
                + "OR o.pancake_status_name IN (:names) "
                + "OR o.pancake_status_code IN (:codes)"
                + ") "
                + "ORDER BY o.pancake_synced_at NULLS FIRST, o.pancake_synced_at ASC "
                + "LIMIT :limit",
        nativeQuery = true
    )
    List<Order> findOrdersForPancakeHourlySync(@Param("names") List<String> names,
                                               @Param("codes") List<Integer> codes,
                                               @Param("limit") int limit);

    @Query(
        value = "SELECT o.* " +
                "FROM orders o " +
                "LEFT JOIN order_items oi ON o.id = oi.order_id " +
                "LEFT JOIN variations v ON oi.variation_id = v.id " +
                "WHERE o.id = :id",
        nativeQuery = true
    )
    Order findOrderWithFullItems(@Param("id") Long id);
}
