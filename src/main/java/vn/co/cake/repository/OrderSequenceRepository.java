package vn.co.cake.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.co.cake.entity.OrderSequence;

public interface OrderSequenceRepository extends JpaRepository<OrderSequence, Long> {
    OrderSequence findFirstByDate(String date);
}
