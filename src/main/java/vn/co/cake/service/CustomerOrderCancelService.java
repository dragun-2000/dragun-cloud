package vn.co.cake.service;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import vn.co.cake.controller.external.dto.OrderPancakeResponse;
import vn.co.cake.dto.CustomerOrderCancelResponse;
import vn.co.cake.entity.Order;
import vn.co.cake.enums.OrderStatus;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.repository.OrderRepository;
import vn.co.cake.service.external.PancakeOrderStatus;
import vn.co.cake.service.external.PancakePosService;

/**
 * Khách hủy đơn trên lịch sử: chỉ khi Pancake còn trạng thái mới.
 * Không đụng kho — Pancake cộng tồn và đẩy webhook.
 */
@Slf4j
@Service
public class CustomerOrderCancelService {

    private static final String CANCEL_REASON = "Khách hủy đơn trên lịch sử đơn hàng";

    private final OrderRepository orderRepository;
    private final PancakePosService pancakePosService;

    public CustomerOrderCancelService(OrderRepository orderRepository, PancakePosService pancakePosService) {
        this.orderRepository = orderRepository;
        this.pancakePosService = pancakePosService;
    }

    public CustomerOrderCancelResponse cancel(String orderCode, Long accountId) throws CommonServletException {
        if (StringUtils.isBlank(orderCode) || accountId == null) {
            throw new CommonServletException("Không tìm thấy đơn hàng");
        }
        Order order = orderRepository.findFirstByCodeAndAccountIdAndDeletedFalse(orderCode.trim(), accountId)
                .orElseThrow(() -> new CommonServletException("Không tìm thấy đơn hàng"));

        if (!OrderStatus.NEW.getValue().equals(order.getStatus())) {
            if (OrderStatus.CANCELLED.getValue().equals(order.getStatus())) {
                return CustomerOrderCancelResponse.alreadyCancelled(order.getCode(), order.getPancakeStatusName());
            }
            throw new CommonServletException("Đơn hàng không thể hủy ở trạng thái hiện tại");
        }
        if (StringUtils.isBlank(order.getPancakeOrderId())) {
            throw new CommonServletException("Đơn chưa đồng bộ Pancake. Vui lòng thử lại sau.");
        }

        OrderPancakeResponse live = pancakePosService.getOrderPancakeDetail(order.getPancakeOrderId());
        if (live == null) {
            throw new CommonServletException("Không kiểm tra được trạng thái trên Pancake. Vui lòng thử lại.");
        }

        Integer liveCode = live.getStatusCode();
        String liveName = live.getStatus_name();
        log.info("Customer cancel check code={} pancakeOrderId={} statusCode={} statusName={}",
                order.getCode(), order.getPancakeOrderId(), liveCode, liveName);

        if (PancakeOrderStatus.isCanceled(liveCode, liveName)) {
            markLocalCancelled(order, live);
            return CustomerOrderCancelResponse.alreadyCancelled(order.getCode(), order.getPancakeStatusName());
        }
        if (!PancakeOrderStatus.isNew(liveCode, liveName)) {
            applyLiveStatus(order, live);
            return CustomerOrderCancelResponse.contactAdmin(order.getCode(),
                    PancakeOrderStatus.resolveDisplayStatus(order.getStatus(), liveCode, liveName));
        }

        OrderPancakeResponse canceled = pancakePosService.cancelOrderOnPancake(order.getPancakeOrderId());
        if (canceled == null) {
            throw new CommonServletException("Không hủy được đơn trên Pancake. Vui lòng thử lại.");
        }
        markLocalCancelled(order, canceled);
        return CustomerOrderCancelResponse.cancelled(order.getCode(), order.getPancakeStatusName());
    }

    private void markLocalCancelled(Order order, OrderPancakeResponse pancake) {
        order.setStatus(OrderStatus.CANCELLED.getValue());
        order.setMessageError(CANCEL_REASON);
        if (pancake != null) {
            PancakeOrderStatus.applyFulfillmentToOrder(order, pancake.getStatusCode(), pancake.getStatus_name());
            if (StringUtils.isNotBlank(pancake.getTracking_link())) {
                order.setTrackingLink(pancake.getTracking_link());
            }
        }
        PancakeOrderStatus.applyFulfillmentToOrder(order, PancakeOrderStatus.CODE_CANCELED,
                PancakeOrderStatus.LABEL_CANCELED);
        order.setPancakeSyncedAt(new Date());
        orderRepository.save(order);
    }

    private void applyLiveStatus(Order order, OrderPancakeResponse pancake) {
        if (pancake == null) {
            return;
        }
        PancakeOrderStatus.applyFulfillmentToOrder(order, pancake.getStatusCode(), pancake.getStatus_name());
        if (StringUtils.isNotBlank(pancake.getTracking_link())) {
            order.setTrackingLink(pancake.getTracking_link());
        }
        order.setPancakeSyncedAt(new Date());
        orderRepository.save(order);
    }
}
