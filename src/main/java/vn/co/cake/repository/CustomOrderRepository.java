package vn.co.cake.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.co.cake.entity.Order;
import vn.co.cake.request.SearchRequest;

public interface CustomOrderRepository {
    Page<Order> findAllByCondition(SearchRequest searchRequest, Pageable pageable);
    Page<Order> findAllSyncFailOrders(SearchRequest searchRequest, Pageable pageable);
}
