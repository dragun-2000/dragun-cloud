package vn.co.cake.payment.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;
import vn.co.cake.entity.Order;
import vn.co.cake.entity.OrderItem;
import vn.co.cake.entity.Product;
import vn.co.cake.entity.Variation;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.payment.PaymentConstants;
import vn.co.cake.payment.entity.InventoryReservation;
import vn.co.cake.payment.repository.InventoryReservationRepository;
import vn.co.cake.repository.OrderRepository;
import vn.co.cake.repository.ProductRepository;
import vn.co.cake.repository.VariationRepository;

/**
 * Kho thanh toán an toàn:
 * <ul>
 *   <li>VietQR submit / COD tạo đơn: {@link #reserve} trừ {@code remain_quantity} ngay (HELD + stockDeducted).</li>
 *   <li>Thanh toán thành công: {@link #confirm} chỉ commit HELD → CONFIRMED (không trừ lại).</li>
 *   <li>Thanh toán thất bại / hết hạn: {@link #release} hoàn lại số lượng đã trừ.</li>
 * </ul>
 * Mục tiêu: nhận tiền thì luôn còn hàng đã giữ từ lúc submit — tránh PAID nhưng hết hàng.
 */
@Service
@Slf4j
public class InventoryReservationService {

    private final InventoryReservationRepository reservationRepository;
    private final VariationRepository variationRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public InventoryReservationService(InventoryReservationRepository reservationRepository,
                                       VariationRepository variationRepository,
                                       ProductRepository productRepository,
                                       OrderRepository orderRepository) {
        this.reservationRepository = reservationRepository;
        this.variationRepository = variationRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Đảm bảo kho đã CONFIRMED cho đơn đã nhận tiền (webhook / admin recreate).
     * Nếu đang HELD (đã trừ lúc submit) → chỉ confirm.
     * Nếu hold đã mất (hết hạn/release) → reserve lại rồi confirm (có thể fail hết hàng).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void ensureConfirmedForPaidOrder(Long orderId, Date expiresAt) throws CommonServletException {
        Order managed = orderRepository.findWithItemsAndVariationsById(orderId)
                .orElseThrow(() -> new CommonServletException("Không tìm thấy đơn để giữ hàng"));
        List<InventoryReservation> existing = reservationRepository.findAllByOrderId(orderId);
        boolean alreadyFinal = existing.stream().anyMatch(row ->
                PaymentConstants.RESERVATION_STATUS_CONFIRMED.equals(row.getStatus())
                        || PaymentConstants.RESERVATION_STATUS_CONSUMED.equals(row.getStatus()));
        if (alreadyFinal) {
            confirm(orderId);
            return;
        }
        boolean hasHeld = existing.stream()
                .anyMatch(row -> PaymentConstants.RESERVATION_STATUS_HELD.equals(row.getStatus()));
        if (!hasHeld) {
            reserve(managed, expiresAt);
        }
        confirm(orderId);
    }

    /** @deprecated dùng {@link #ensureConfirmedForPaidOrder(Long, Date)} */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void reserveAndConfirmInNewTransaction(Long orderId, Date expiresAt) throws CommonServletException {
        ensureConfirmedForPaidOrder(orderId, expiresAt);
    }

    /**
     * Trừ kho cứng ngay khi submit thanh toán / tạo đơn.
     * Cùng transaction với tạo draft: nếu tạo QR/đơn fail → rollback cả số lượng đã trừ.
     */
    @Transactional(rollbackFor = Exception.class)
    public void reserve(Order order, Date expiresAt) throws CommonServletException {
        if (order == null || order.getId() <= 0 || CollectionUtils.isEmpty(order.getOrderItems())) {
            throw new CommonServletException("Không thể giữ hàng cho đơn chưa hợp lệ");
        }
        if (expiresAt == null) {
            throw new CommonServletException("Thời hạn giữ hàng không hợp lệ");
        }

        Map<Long, Integer> requestedByVariation = aggregateQuantities(order.getOrderItems());
        List<Long> variationIds = new ArrayList<>(requestedByVariation.keySet());
        variationIds.sort(Long::compareTo);

        Map<Long, InventoryReservation> existingByVariation = reservationRepository.findAllByOrderId(order.getId())
                .stream()
                .filter(row -> row.getVariation() != null)
                .collect(Collectors.toMap(row -> row.getVariation().getId(), row -> row, (a, b) -> a));

        if (existingByVariation.values().stream().anyMatch(this::isFinalStatus)) {
            return;
        }

        // Đã trừ kho lúc submit → chỉ gia hạn hold, không trừ lần 2.
        if (existingByVariation.values().stream().anyMatch(this::isActiveHardHold)) {
            for (InventoryReservation reservation : existingByVariation.values()) {
                if (isActiveHardHold(reservation)) {
                    reservation.setExpiresAt(expiresAt);
                }
            }
            reservationRepository.saveAll(existingByVariation.values());
            return;
        }

        Map<Long, Variation> lockedVariations = variationRepository.findAllByIdInForUpdate(variationIds).stream()
                .collect(Collectors.toMap(Variation::getId, variation -> variation));
        Date now = new Date();

        for (Long variationId : variationIds) {
            Variation variation = lockedVariations.get(variationId);
            if (variation == null || variation.isDeleted()) {
                throw new CommonServletException("Sản phẩm trong đơn không còn tồn tại");
            }
            long remain = variation.getRemainQuantity() != null ? variation.getRemainQuantity() : 0L;
            long softHeldByOthers = reservationRepository.sumActiveQuantityExcludingOrder(
                    variationId, order.getId(), now);
            int requested = requestedByVariation.get(variationId);
            if (remain - softHeldByOthers < requested) {
                throw new CommonServletException(String.format(
                        "Sản phẩm [%s] không đủ tồn kho (còn khả dụng %s, yêu cầu %s)",
                        variation.getName(), Math.max(remain - softHeldByOthers, 0L), requested));
            }
        }

        Set<String> pancakeProductIds = new LinkedHashSet<>();
        List<InventoryReservation> saved = new ArrayList<>();
        for (Long variationId : variationIds) {
            Variation variation = lockedVariations.get(variationId);
            int qty = requestedByVariation.get(variationId);
            long remain = variation.getRemainQuantity() != null ? variation.getRemainQuantity() : 0L;
            variation.setRemainQuantity(remain - qty);
            if (variation.getActualRemainQuantity() != null) {
                variation.setActualRemainQuantity(Math.max(0L, variation.getActualRemainQuantity() - qty));
            }
            if (variation.getPancakeProductId() != null) {
                pancakeProductIds.add(variation.getPancakeProductId());
            }

            InventoryReservation reservation = existingByVariation.getOrDefault(variationId, new InventoryReservation());
            reservation.setOrder(order);
            reservation.setVariation(variation);
            reservation.setQuantity(qty);
            reservation.setStatus(PaymentConstants.RESERVATION_STATUS_HELD);
            reservation.setExpiresAt(expiresAt);
            reservation.setStockDeducted(true);
            saved.add(reservation);
        }

        variationRepository.saveAll(lockedVariations.values());
        reservationRepository.saveAll(saved);
        refreshProductStockQuantities(pancakeProductIds);
        log.info("reserve orderId={}: deducted stock for {} variation(s)", order.getId(), saved.size());
    }

    /**
     * Commit giữ hàng sau thanh toán thành công: HELD → CONFIRMED.
     * Không trừ lại nếu đã trừ lúc {@link #reserve}.
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long orderId) throws CommonServletException {
        List<InventoryReservation> reservations = reservationRepository.findAllByOrderId(orderId);
        if (reservations.isEmpty()) {
            throw new CommonServletException("Không có giữ hàng để xác nhận");
        }

        List<InventoryReservation> needDeduct = reservations.stream()
                .filter(row -> !row.isStockDeducted())
                .filter(row -> PaymentConstants.RESERVATION_STATUS_HELD.equals(row.getStatus())
                        || PaymentConstants.RESERVATION_STATUS_CONFIRMED.equals(row.getStatus()))
                .collect(Collectors.toList());
        if (!needDeduct.isEmpty()) {
            // Legacy soft-hold: trừ kho tại thời điểm confirm.
            applyStockDelta(orderId, needDeduct, -1, true);
            for (InventoryReservation reservation : needDeduct) {
                reservation.setStockDeducted(true);
            }
        }

        boolean changed = false;
        for (InventoryReservation reservation : reservations) {
            if (PaymentConstants.RESERVATION_STATUS_HELD.equals(reservation.getStatus())) {
                reservation.setStatus(PaymentConstants.RESERVATION_STATUS_CONFIRMED);
                reservation.setStockDeducted(true);
                changed = true;
            }
        }
        if (changed || !needDeduct.isEmpty()) {
            reservationRepository.saveAll(reservations);
        }
        log.info("confirm orderId={}: reservation CONFIRMED (stock already secured)", orderId);
    }

    @Transactional
    public void consume(Long orderId) {
        List<InventoryReservation> reservations = reservationRepository.findAllByOrderId(orderId);
        List<InventoryReservation> legacy = reservations.stream()
                .filter(row -> !row.isStockDeducted())
                .filter(row -> PaymentConstants.RESERVATION_STATUS_CONFIRMED.equals(row.getStatus()))
                .collect(Collectors.toList());
        if (!legacy.isEmpty()) {
            try {
                applyStockDelta(orderId, legacy, -1, false);
                for (InventoryReservation reservation : legacy) {
                    reservation.setStockDeducted(true);
                }
            } catch (CommonServletException ex) {
                log.warn("consume orderId={}: legacy stock deduct skipped — {}", orderId, ex.getMessage());
                for (InventoryReservation reservation : legacy) {
                    reservation.setStockDeducted(true);
                }
            }
        }
        for (InventoryReservation reservation : reservations) {
            if (PaymentConstants.RESERVATION_STATUS_CONFIRMED.equals(reservation.getStatus())) {
                reservation.setStatus(PaymentConstants.RESERVATION_STATUS_CONSUMED);
            }
        }
        reservationRepository.saveAll(reservations);
    }

    /**
     * Hoàn kho khi thanh toán thất bại / hết hạn: cộng lại remain_quantity rồi RELEASED.
     */
    @Transactional(rollbackFor = Exception.class)
    public void release(Long orderId) {
        List<InventoryReservation> held = reservationRepository.findAllByOrderId(orderId).stream()
                .filter(row -> PaymentConstants.RESERVATION_STATUS_HELD.equals(row.getStatus()))
                .collect(Collectors.toList());
        if (held.isEmpty()) {
            return;
        }

        List<InventoryReservation> toRestore = held.stream()
                .filter(InventoryReservation::isStockDeducted)
                .collect(Collectors.toList());
        long softHeldOnly = held.size() - toRestore.size();
        if (softHeldOnly > 0) {
            log.warn("release orderId={}: {} HELD row(s) have stockDeducted=false "
                    + "(legacy soft-hold — không cộng remain_quantity)", orderId, softHeldOnly);
        }
        if (!toRestore.isEmpty()) {
            try {
                applyStockDelta(orderId, toRestore, +1, false);
            } catch (CommonServletException ex) {
                log.warn("release orderId={}: restore stock issue — {}", orderId, ex.getMessage());
            }
        }

        for (InventoryReservation reservation : held) {
            reservation.setStatus(PaymentConstants.RESERVATION_STATUS_RELEASED);
            reservation.setStockDeducted(false);
        }
        reservationRepository.saveAll(held);
        log.info("release orderId={}: restored {} hard-hold(s), marked RELEASED",
                orderId, toRestore.size());
    }

    @Transactional(readOnly = true)
    public boolean hasConfirmedReservation(Long orderId) {
        return reservationRepository.findAllByOrderId(orderId).stream()
                .anyMatch(row -> PaymentConstants.RESERVATION_STATUS_CONFIRMED.equals(row.getStatus()));
    }

    /**
     * @param sign -1 trừ kho, +1 hoàn kho
     */
    private void applyStockDelta(Long orderId, List<InventoryReservation> reservations, int sign, boolean strict)
            throws CommonServletException {
        List<Long> variationIds = reservations.stream()
                .map(row -> row.getVariation().getId())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        Map<Long, Variation> lockedVariations = variationRepository.findAllByIdInForUpdate(variationIds).stream()
                .collect(Collectors.toMap(Variation::getId, variation -> variation));
        Date now = new Date();
        Set<String> pancakeProductIds = new LinkedHashSet<>();

        for (InventoryReservation reservation : reservations) {
            Long variationId = reservation.getVariation().getId();
            Variation variation = lockedVariations.get(variationId);
            if (variation == null || variation.isDeleted()) {
                if (strict) {
                    throw new CommonServletException("Sản phẩm trong đơn không còn tồn tại");
                }
                continue;
            }
            int qty = reservation.getQuantity();
            long remain = variation.getRemainQuantity() != null ? variation.getRemainQuantity() : 0L;
            if (sign < 0) {
                long softHeldByOthers = reservationRepository.sumActiveQuantityExcludingOrder(
                        variationId, orderId, now);
                if (strict && remain - softHeldByOthers < qty) {
                    throw new CommonServletException(String.format(
                            "Sản phẩm [%s] không đủ tồn kho (còn khả dụng %s, yêu cầu %s)",
                            variation.getName(), Math.max(remain - softHeldByOthers, 0L), qty));
                }
                variation.setRemainQuantity(strict ? remain - qty : Math.max(0L, remain - qty));
                if (variation.getActualRemainQuantity() != null) {
                    variation.setActualRemainQuantity(Math.max(0L, variation.getActualRemainQuantity() - qty));
                }
            } else {
                variation.setRemainQuantity(remain + qty);
                if (variation.getActualRemainQuantity() != null) {
                    variation.setActualRemainQuantity(variation.getActualRemainQuantity() + qty);
                }
            }
            if (variation.getPancakeProductId() != null) {
                pancakeProductIds.add(variation.getPancakeProductId());
            }
        }

        variationRepository.saveAll(lockedVariations.values());
        refreshProductStockQuantities(pancakeProductIds);
    }

    private void refreshProductStockQuantities(Set<String> pancakeProductIds) {
        for (String pancakeProductId : pancakeProductIds) {
            Product product = productRepository.findFirstByProductPancakeId(pancakeProductId);
            if (product == null) {
                continue;
            }
            long totalStock = variationRepository.findAllByPancakeProductId(pancakeProductId).stream()
                    .mapToLong(v -> v.getRemainQuantity() != null ? v.getRemainQuantity() : 0L)
                    .sum();
            product.setStockQuantity(totalStock);
            productRepository.save(product);
        }
    }

    private boolean isFinalStatus(InventoryReservation reservation) {
        String status = reservation.getStatus();
        return PaymentConstants.RESERVATION_STATUS_CONFIRMED.equals(status)
                || PaymentConstants.RESERVATION_STATUS_CONSUMED.equals(status);
    }

    private boolean isActiveHardHold(InventoryReservation reservation) {
        return PaymentConstants.RESERVATION_STATUS_HELD.equals(reservation.getStatus())
                && reservation.isStockDeducted();
    }

    private static Map<Long, Integer> aggregateQuantities(List<OrderItem> orderItems)
            throws CommonServletException {
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (OrderItem item : orderItems) {
            if (item == null || item.getVariation() == null || item.getQuantity() <= 0) {
                throw new CommonServletException("Sản phẩm hoặc số lượng giữ hàng không hợp lệ");
            }
            long variationId = item.getVariation().getId();
            result.merge(variationId, item.getQuantity(), Integer::sum);
        }
        if (result.isEmpty() || result.keySet().stream().anyMatch(Objects::isNull)) {
            throw new CommonServletException("Đơn hàng không có sản phẩm hợp lệ");
        }
        return result;
    }
}
