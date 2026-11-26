package vn.co.cake.service;

import vn.co.cake.dto.CartForm;
import vn.co.cake.exception.CommonServletException;

public interface CartService {
    CartForm findFirstByAccountId(Long accountId);
    void create(CartForm cartForm, Long accountId, String variationId) throws CommonServletException;
    void deletedCartByAccount(Long accountId);
}
