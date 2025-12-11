package vn.co.cake.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.co.cake.entity.Order;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long>, CustomOrderRepository {
    Order findFirstByCode(String code);
    Order findFirstByAccountIdOrderByCreatedDesc(Long accountId);
    List<Order> findByStatusAndCountErrorLessThan(String status, int maxCountError);
    List<Order> findByPhoneAndDeletedFalseOrderByCreatedDesc(String phone);

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
