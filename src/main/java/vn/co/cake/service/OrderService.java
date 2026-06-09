package vn.co.cake.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.co.cake.dto.OrderItem;
import vn.co.cake.entity.Order;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.request.OrderDetailRequest;
import vn.co.cake.request.SearchRequest;

import java.math.BigDecimal;
import java.util.List;

public interface OrderService {

    Page<Order> findAllByCondition(SearchRequest searchForm, Pageable pageable);

    Page<Order> findAllSyncFailOrders(SearchRequest searchRequest, Pageable pageable);

    String generateOrderCode();

    BigDecimal calculateGrandTotal(List<OrderItem> newOrderItems);

    /** Tổng tiền hàng (final_price × số lượng), không gồm phí ship. */
    BigDecimal calculateItemsSubtotal(List<OrderItem> orderItems);

    Order create(Long accountId, List<OrderItem> newOrderItems, OrderDetailRequest request) throws CommonServletException;

    Order createWithOrderCode(Long accountId, List<OrderItem> newOrderItems, OrderDetailRequest request, String orderCode)
            throws CommonServletException;

    Order detail(String code);

    Order detail(Long accountId);

    void delete(String code) throws CommonServletException;
}
