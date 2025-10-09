package vn.co.cake.service;

import vn.co.cake.dto.CartForm;
import vn.co.cake.dto.OrderItem;
import vn.co.cake.exception.CommonServletException;

import java.util.List;

public interface CartService {
    CartForm findFirstByAccountId(Long accountId);
    void create(CartForm cartForm, Long accountId, String variationId) throws CommonServletException;
    List<OrderItem> findOtherProductItem(Long accountId, Long productId);
    void deletedCartByAccount(Long accountId);
}
