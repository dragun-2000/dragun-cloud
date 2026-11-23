package vn.co.cake.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import vn.co.cake.entity.Order;
import vn.co.cake.enums.PancakeFulfillmentStatus.CancelMode;
import vn.co.cake.service.external.PancakeOrderStatus;

class PancakeFulfillmentStatusTest {

    @Test
    void fromPancakeCodeMapsAllKnownCodes() {
        assertEquals("Mới", PancakeFulfillmentStatus.fromPancakeCode(0).get().getDebaseValue());
        assertEquals("Đã xác nhận", PancakeFulfillmentStatus.fromPancakeCode(1).get().getDebaseValue());
        assertEquals("Đã gửi hàng", PancakeFulfillmentStatus.fromPancakeCode(2).get().getDebaseValue());
        assertEquals("Đã nhận", PancakeFulfillmentStatus.fromPancakeCode(3).get().getDebaseValue());
        assertEquals("Đã hủy", PancakeFulfillmentStatus.fromPancakeCode(6).get().getDebaseValue());
        assertEquals("Đang hoàn", PancakeFulfillmentStatus.fromPancakeCode(7).get().getDebaseValue());
        assertEquals("Đang đóng hàng", PancakeFulfillmentStatus.fromPancakeCode(8).get().getDebaseValue());
        assertEquals("Chờ chuyển hàng", PancakeFulfillmentStatus.fromPancakeCode(9).get().getDebaseValue());
        assertEquals("Chờ hàng", PancakeFulfillmentStatus.fromPancakeCode(11).get().getDebaseValue());
        assertFalse(PancakeFulfillmentStatus.fromPancakeCode(99).isPresent());
    }

    @Test
    void fromStoredNameAcceptsDebaseAndRawAliases() {
        assertEquals(PancakeFulfillmentStatus.FULFILLMENT_NEW, PancakeFulfillmentStatus.fromStoredName("Mới").get());
        assertEquals(PancakeFulfillmentStatus.FULFILLMENT_NEW, PancakeFulfillmentStatus.fromStoredName("new").get());
        assertEquals(PancakeFulfillmentStatus.CONFIRMED, PancakeFulfillmentStatus.fromStoredName("confirmed").get());
        assertEquals(PancakeFulfillmentStatus.CANCELED, PancakeFulfillmentStatus.fromStoredName("Đã hủy").get());
        assertEquals(PancakeFulfillmentStatus.CANCELED, PancakeFulfillmentStatus.fromStoredName("canceled").get());
        assertFalse(PancakeFulfillmentStatus.fromStoredName("Trạng thái #1").isPresent());
    }

    @Test
    void hourlyListsAreNonEmptyAndAligned() {
        assertFalse(PancakeFulfillmentStatus.hourlySyncDebaseValues().isEmpty());
        assertFalse(PancakeFulfillmentStatus.hourlySyncPancakeCodes().isEmpty());
        assertEquals(PancakeFulfillmentStatus.hourlySyncDebaseValues().size(),
                PancakeFulfillmentStatus.hourlySyncPancakeCodes().size());
        assertTrue(PancakeFulfillmentStatus.hourlySyncPancakeCodes().contains(0));
        assertTrue(PancakeFulfillmentStatus.hourlySyncPancakeCodes().contains(1));
        assertTrue(PancakeFulfillmentStatus.hourlySyncPancakeCodes().contains(2));
        assertTrue(PancakeFulfillmentStatus.hourlySyncPancakeCodes().contains(8));
        assertTrue(PancakeFulfillmentStatus.hourlySyncPancakeCodes().contains(9));
        assertFalse(PancakeFulfillmentStatus.hourlySyncPancakeCodes().contains(3));
        assertFalse(PancakeFulfillmentStatus.hourlySyncPancakeCodes().contains(6));
        assertFalse(PancakeFulfillmentStatus.hourlySyncPancakeCodes().contains(7));
        assertFalse(PancakeFulfillmentStatus.hourlySyncPancakeCodes().contains(11));
    }

    @Test
    void needsHourlySyncPredicate() {
        assertTrue(PancakeFulfillmentStatus.needsHourlySync(null, null));
        assertTrue(PancakeFulfillmentStatus.needsHourlySync(null, ""));
        assertTrue(PancakeFulfillmentStatus.needsHourlySync(null, "Mới"));
        assertTrue(PancakeFulfillmentStatus.needsHourlySync(1, "weird-old-name"));
        assertFalse(PancakeFulfillmentStatus.needsHourlySync(3, "Đã nhận"));
        assertFalse(PancakeFulfillmentStatus.needsHourlySync(6, null));
        assertFalse(PancakeFulfillmentStatus.needsHourlySync(7, "Đang hoàn"));
        assertFalse(PancakeFulfillmentStatus.needsHourlySync(11, null));
    }

    @Test
    void applyFulfillmentPrefersCodeAndDropsFakeName() {
        Order order = new Order();
        PancakeOrderStatus.applyFulfillmentToOrder(order, 1, "Trạng thái #1");
        assertEquals(Integer.valueOf(1), order.getPancakeStatusCode());
        assertEquals("Đã xác nhận", order.getPancakeStatusName());
    }

    @Test
    void resolveDisplayStatusPrefersFulfillmentOverLifecycleNew() {
        assertEquals("Mới",
                PancakeFulfillmentStatus.resolveDisplayStatus("NEW", 0, null));
        assertEquals("Đã xác nhận",
                PancakeFulfillmentStatus.resolveDisplayStatus("NEW", 1, "confirmed"));
        assertEquals("Đã đồng bộ Pancake",
                PancakeFulfillmentStatus.resolveDisplayStatus("NEW", null, null));
        assertEquals("Chờ thanh toán",
                PancakeFulfillmentStatus.resolveDisplayStatus("AWAITING_PAYMENT", null, null));
    }

    @Test
    void resolveCancelModeRequiresLifecycleAndFulfillmentNew() {
        assertEquals(CancelMode.SELF, PancakeFulfillmentStatus.resolveCancelMode("NEW", 0, "Mới"));
        assertEquals(CancelMode.SELF, PancakeFulfillmentStatus.resolveCancelMode("NEW", null, null));
        assertEquals(CancelMode.CONTACT, PancakeFulfillmentStatus.resolveCancelMode("NEW", 1, null));
        assertEquals(CancelMode.CONTACT, PancakeFulfillmentStatus.resolveCancelMode("NEW", 8, null));
        assertEquals(CancelMode.NONE, PancakeFulfillmentStatus.resolveCancelMode("NEW", 6, null));
        assertEquals(CancelMode.NONE, PancakeFulfillmentStatus.resolveCancelMode("CANCELLED", 0, "Mới"));
    }
}
