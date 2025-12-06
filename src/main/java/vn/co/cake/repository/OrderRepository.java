package vn.co.cake.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.co.cake.entity.Order;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long>, CustomOrderRepository {
    Order findFirstByCode(String code);
    Order findFirstByAccountIdOrderByCreatedDesc(Long accountId);
    List<Order> findByStatusAndCountErrorLessThan(String status, int maxCountError);
}
