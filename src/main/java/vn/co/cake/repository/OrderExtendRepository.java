package vn.co.cake.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.co.cake.entity.external.OrderExtend;

import java.util.List;

@Repository
public interface OrderExtendRepository extends JpaRepository<OrderExtend, Long> {
    List<OrderExtend> findByCustomerInfoName(String name);
}
