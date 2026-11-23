package vn.co.cake.service.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import vn.co.cake.enums.PancakeFulfillmentStatus;
import vn.co.cake.enums.PancakeFulfillmentStatus.CancelMode;

/** Facade smoke tests — detailed cases live in PancakeFulfillmentStatusTest. */
class PancakeOrderStatusTest {

    @Test
    void facadeDelegatesToEnum() {
        assertEquals(PancakeFulfillmentStatus.FULFILLMENT_NEW.getPancakeCode(), PancakeOrderStatus.CODE_NEW);
        assertEquals(PancakeFulfillmentStatus.CANCELED.getDebaseValue(), PancakeOrderStatus.LABEL_CANCELED);
        assertTrue(PancakeOrderStatus.isNew(0, null));
        assertTrue(PancakeOrderStatus.isCanceled(6, null));
        assertFalse(PancakeOrderStatus.needsHourlySync(3, "Đã nhận"));
        assertEquals(CancelMode.CONTACT, PancakeOrderStatus.resolveCancelMode("NEW", 1, null));
        assertFalse(PancakeOrderStatus.hourlySyncDebaseValues().isEmpty());
        assertFalse(PancakeOrderStatus.hourlySyncPancakeCodes().isEmpty());
    }
}
