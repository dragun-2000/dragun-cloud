package vn.co.cake.payment.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.co.cake.payment.entity.InventoryReservation;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    List<InventoryReservation> findAllByOrderId(Long orderId);

    Optional<InventoryReservation> findFirstByOrderIdAndVariationId(Long orderId, Long variationId);

    /**
     * Chỉ cộng soft-hold chưa trừ remain_quantity (legacy / chưa hard-deduct).
     * Hard-hold (HELD/CONFIRMED với stockDeducted=true) đã nằm trong remain_quantity nên không đếm lại.
     */
    @Query("select coalesce(sum(r.quantity), 0) from InventoryReservation r "
            + "where r.variation.id = :variationId and r.order.id <> :orderId and r.stockDeducted = false and "
            + "((r.status = 'HELD' and r.expiresAt > :now) or r.status = 'CONFIRMED')")
    Long sumActiveQuantityExcludingOrder(@Param("variationId") Long variationId,
                                         @Param("orderId") Long orderId,
                                         @Param("now") Date now);

    @Query("select distinct r.order.id from InventoryReservation r "
            + "where r.status = 'HELD' and r.expiresAt <= :now")
    List<Long> findExpiredHeldOrderIds(@Param("now") Date now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update InventoryReservation r set r.status = 'RELEASED' "
            + "where r.order.id = :orderId and r.status = 'HELD'")
    int releaseHeldByOrderId(@Param("orderId") Long orderId);
}
