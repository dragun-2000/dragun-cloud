package vn.co.cake.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.co.cake.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
