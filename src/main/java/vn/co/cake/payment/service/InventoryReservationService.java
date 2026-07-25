package vn.co.cake.payment.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import vn.co.cake.entity.Order;
import vn.co.cake.entity.OrderItem;
import vn.co.cake.entity.Variation;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.payment.PaymentConstants;
import vn.co.cake.payment.entity.InventoryReservation;
import vn.co.cake.payment.repository.InventoryReservationRepository;
import vn.co.cake.repository.OrderRepository;
import vn.co.cake.repository.VariationRepository;

@Service
public class InventoryReservationService {

    private final InventoryReservationRepository reservationRepository;
    private final VariationRepository variationRepository;
    private final OrderRepository orderRepository;

    public InventoryReservationService(InventoryReservationRepository reservationRepository,
                                       VariationRepository variationRepository,
                                       OrderRepository orderRepository) {
        this.reservationRepository = reservationRepository;
        this.variationRepository = variationRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Giữ + xác nhận kho trong một transaction ĐỘC LẬP (REQUIRES_NEW) cho luồng
     * ĐÃ nhận tiền (webhook / admin tạo lại đơn). Nếu hết hàng, chỉ transaction này
     * rollback, KHÔNG làm hỏng transaction cha đang giữ bản ghi Payment/trạng thái đơn.
     * Reload order theo id để entity + variation thuộc đúng persistence context của tx mới.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void reserveAndConfirmInNewTransaction(Long orderId, Date expiresAt) throws CommonServletException {
        Order managed = orderRepository.findWithItemsAndVariationsById(orderId)
                .orElseThrow(() -> new CommonServletException("Không tìm thấy đơn để giữ hàng"));
        reserve(managed, expiresAt);
        confirm(orderId);
    }

    /**
     * Giữ kho nguyên tử theo variation. Pessimistic lock ngăn hai checkout cùng giữ
     * số lượng cuối cùng tại cùng một thời điểm.
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

        Map<Long, Variation> lockedVariations = variationRepository.findAllByIdInForUpdate(variationIds).stream()
                .collect(Collectors.toMap(Variation::getId, variation -> variation));
        Date now = new Date();

        for (Long variationId : variationIds) {
            Variation variation = lockedVariations.get(variationId);
            if (variation == null || variation.isDeleted()) {
                throw new CommonServletException("Sản phẩm trong đơn không còn tồn tại");
            }
            long remain = variation.getRemainQuantity() != null ? variation.getRemainQuantity() : 0L;
            long reservedByOthers = reservationRepository.sumActiveQuantityExcludingOrder(
                    variationId, order.getId(), now);
            int requested = requestedByVariation.get(variationId);
            if (remain - reservedByOthers < requested) {
                throw new CommonServletException(String.format(
                        "Sản phẩm [%s] không đủ tồn kho để giữ hàng (còn khả dụng %s, yêu cầu %s)",
                        variation.getName(), Math.max(remain - reservedByOthers, 0L), requested));
            }
        }

        for (Long variationId : variationIds) {
            InventoryReservation reservation = reservationRepository
                    .findFirstByOrderIdAndVariationId(order.getId(), variationId)
                    .orElseGet(InventoryReservation::new);
            reservation.setOrder(order);
            reservation.setVariation(lockedVariations.get(variationId));
            reservation.setQuantity(requestedByVariation.get(variationId));
            reservation.setStatus(PaymentConstants.RESERVATION_STATUS_HELD);
            reservation.setExpiresAt(expiresAt);
            reservationRepository.save(reservation);
        }
    }

    @Transactional
    public void confirm(Long orderId) {
        updateStatus(orderId, PaymentConstants.RESERVATION_STATUS_HELD,
                PaymentConstants.RESERVATION_STATUS_CONFIRMED);
    }

    @Transactional
    public void consume(Long orderId) {
        List<InventoryReservation> reservations = reservationRepository.findAllByOrderId(orderId);
        for (InventoryReservation reservation : reservations) {
            if (PaymentConstants.RESERVATION_STATUS_CONFIRMED.equals(reservation.getStatus())) {
                reservation.setStatus(PaymentConstants.RESERVATION_STATUS_CONSUMED);
            }
        }
        reservationRepository.saveAll(reservations);
    }

    @Transactional
    public void release(Long orderId) {
        reservationRepository.releaseHeldByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public boolean hasConfirmedReservation(Long orderId) {
        return reservationRepository.findAllByOrderId(orderId).stream()
                .anyMatch(row -> PaymentConstants.RESERVATION_STATUS_CONFIRMED.equals(row.getStatus()));
    }

    private void updateStatus(Long orderId, String fromStatus, String toStatus) {
        List<InventoryReservation> reservations = reservationRepository.findAllByOrderId(orderId);
        for (InventoryReservation reservation : reservations) {
            if (fromStatus.equals(reservation.getStatus())) {
                reservation.setStatus(toStatus);
            }
        }
        reservationRepository.saveAll(reservations);
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
