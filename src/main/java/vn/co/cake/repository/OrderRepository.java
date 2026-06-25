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

    @Query(
        value = "SELECT * FROM orders o WHERE o.deleted = false "
                + "AND o.phone IS NOT NULL AND TRIM(o.phone) <> '' "
                + "AND (o.pancake_status_name IS NULL OR TRIM(o.pancake_status_name) = '' "
                + "OR o.shipping_partner IS NULL OR TRIM(o.shipping_partner) = '') "
                + "ORDER BY o.created DESC LIMIT :limit",
        nativeQuery = true
    )
    List<Order> findOrdersNeedingPancakeEnrichment(@Param("limit") int limit);

    @Query(
        value = "SELECT * FROM orders o WHERE o.deleted = false "
                + "AND o.phone IS NOT NULL AND TRIM(o.phone) <> '' "
                + "AND (o.pancake_status_name IS NULL OR LOWER(TRIM(o.pancake_status_name)) NOT IN ('shipped', 'canceled')) "
                + "AND (o.pancake_synced_at IS NULL OR o.pancake_synced_at < NOW() - INTERVAL '1 hour') "
                + "ORDER BY CASE WHEN o.pancake_synced_at IS NULL THEN 0 ELSE 1 END, o.created DESC "
                + "LIMIT :limit",
        nativeQuery = true
    )
    List<Order> findOrdersForPancakeHourlySync(@Param("limit") int limit);

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
