package vn.co.cake.service.external;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import vn.co.cake.controller.external.dto.OrderPancakeResponse;
import vn.co.cake.entity.Order;
import vn.co.cake.repository.OrderRepository;

/**
 * Ghi kết quả sync Pancake theo từng đơn (transaction riêng), dùng bởi Job003 và refresh lịch sử.
 */
@Slf4j
@Service
public class PancakeOrderSyncWriter {

    private final OrderRepository orderRepository;

    public PancakeOrderSyncWriter(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applySuccessfulSync(Long orderId, OrderPancakeResponse pancakeOrder) {
        if (orderId == null || pancakeOrder == null) {
            return;
        }
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return;
        }
        applyFields(order, pancakeOrder);
        order.setPancakeSyncedAt(new Date());
        orderRepository.save(order);
    }

    static void applyFields(Order order, OrderPancakeResponse pancakeOrder) {
        if (StringUtils.isNotBlank(pancakeOrder.getId())) {
            order.setPancakeOrderId(pancakeOrder.getId());
        }
        PancakeOrderStatus.applyFulfillmentToOrder(order, pancakeOrder.getStatusCode(), pancakeOrder.getStatus_name());
        String partner = pancakeOrder.resolveShippingPartner();
        if (StringUtils.isNotBlank(partner)) {
            order.setShippingPartner(partner);
        }
        if (StringUtils.isNotBlank(pancakeOrder.getTracking_link())) {
            order.setTrackingLink(pancakeOrder.getTracking_link());
        }
    }
}
