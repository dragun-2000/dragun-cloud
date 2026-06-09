package vn.co.cake.enums;

import vn.co.cake.constants.OrderConstants;

/**
 * Trạng thái đồng bộ đơn hàng lên Pancake POS (cột {@code orders.status}).
 */
public enum OrderStatus {

    NEW(OrderConstants.STATUS_NEW),
    SYNC_FAIL(OrderConstants.STATUS_SYNC_FAIL),
    PENDING_SYNC(OrderConstants.STATUS_PENDING_SYNC);

    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
