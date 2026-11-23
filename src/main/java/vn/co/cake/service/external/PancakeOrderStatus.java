package vn.co.cake.service.external;

import java.util.List;
import java.util.Optional;

import vn.co.cake.entity.Order;
import vn.co.cake.enums.PancakeFulfillmentStatus;
import vn.co.cake.enums.PancakeFulfillmentStatus.CancelMode;

/**
 * Facade over {@link PancakeFulfillmentStatus} — single source of truth for Pancake fulfillment codes.
 */
public final class PancakeOrderStatus {

    public static final int CODE_NEW = PancakeFulfillmentStatus.FULFILLMENT_NEW.getPancakeCode();
    public static final int CODE_CONFIRMED = PancakeFulfillmentStatus.CONFIRMED.getPancakeCode();
    public static final int CODE_SHIPPED = PancakeFulfillmentStatus.SHIPPED.getPancakeCode();
    public static final int CODE_DELIVERED = PancakeFulfillmentStatus.DELIVERED.getPancakeCode();
    public static final int CODE_CANCELED = PancakeFulfillmentStatus.CANCELED.getPancakeCode();
    public static final int CODE_RETURNING = PancakeFulfillmentStatus.RETURNING.getPancakeCode();
    public static final int CODE_PACKING = PancakeFulfillmentStatus.PACKING.getPancakeCode();
    public static final int CODE_WAITING_TRANSFER = PancakeFulfillmentStatus.WAITING_TRANSFER.getPancakeCode();
    public static final int CODE_WAITING_STOCK = PancakeFulfillmentStatus.WAITING_STOCK.getPancakeCode();

    public static final String LABEL_NEW = PancakeFulfillmentStatus.FULFILLMENT_NEW.getDebaseValue();
    public static final String LABEL_CONFIRMED = PancakeFulfillmentStatus.CONFIRMED.getDebaseValue();
    public static final String LABEL_SHIPPED = PancakeFulfillmentStatus.SHIPPED.getDebaseValue();
    public static final String LABEL_DELIVERED = PancakeFulfillmentStatus.DELIVERED.getDebaseValue();
    public static final String LABEL_CANCELED = PancakeFulfillmentStatus.CANCELED.getDebaseValue();
    public static final String LABEL_RETURNING = PancakeFulfillmentStatus.RETURNING.getDebaseValue();
    public static final String LABEL_PACKING = PancakeFulfillmentStatus.PACKING.getDebaseValue();
    public static final String LABEL_WAITING_TRANSFER = PancakeFulfillmentStatus.WAITING_TRANSFER.getDebaseValue();
    public static final String LABEL_WAITING_STOCK = PancakeFulfillmentStatus.WAITING_STOCK.getDebaseValue();

    private PancakeOrderStatus() {
    }

    public static String displayLabel(Integer statusCode) {
        return PancakeFulfillmentStatus.displayLabel(statusCode);
    }

    public static boolean needsHourlySync(Integer statusCode) {
        return PancakeFulfillmentStatus.needsHourlySync(statusCode, null);
    }

    public static boolean needsHourlySync(Integer statusCode, String statusName) {
        return PancakeFulfillmentStatus.needsHourlySync(statusCode, statusName);
    }

    public static boolean isNew(Integer statusCode, String statusName) {
        return PancakeFulfillmentStatus.resolve(statusCode, statusName)
                .map(status -> status == PancakeFulfillmentStatus.FULFILLMENT_NEW)
                .orElse(false);
    }

    public static boolean isCanceled(Integer statusCode, String statusName) {
        return PancakeFulfillmentStatus.resolve(statusCode, statusName)
                .map(status -> status == PancakeFulfillmentStatus.CANCELED)
                .orElse(false);
    }

    public static boolean isTerminal(Integer statusCode, String statusName) {
        Optional<PancakeFulfillmentStatus> resolved = PancakeFulfillmentStatus.resolve(statusCode, statusName);
        if (!resolved.isPresent()) {
            return false;
        }
        PancakeFulfillmentStatus status = resolved.get();
        return status == PancakeFulfillmentStatus.DELIVERED || status == PancakeFulfillmentStatus.CANCELED;
    }

    public static CancelMode resolveCancelMode(String orderStatus, Integer pancakeStatusCode, String pancakeStatusName) {
        return PancakeFulfillmentStatus.resolveCancelMode(orderStatus, pancakeStatusCode, pancakeStatusName);
    }

    public static String resolveDisplayStatus(String lifecycleStatus, Integer pancakeCode, String pancakeStatusName) {
        return PancakeFulfillmentStatus.resolveDisplayStatus(lifecycleStatus, pancakeCode, pancakeStatusName);
    }

    public static String resolveDisplayStatus(Order order) {
        if (order == null) {
            return "—";
        }
        return resolveDisplayStatus(order.getStatus(), order.getPancakeStatusCode(), order.getPancakeStatusName());
    }

    public static List<String> hourlySyncDebaseValues() {
        return PancakeFulfillmentStatus.hourlySyncDebaseValues();
    }

    public static List<Integer> hourlySyncPancakeCodes() {
        return PancakeFulfillmentStatus.hourlySyncPancakeCodes();
    }

    /**
     * Apply Pancake status onto Debase mirror columns (code + standardized Debase label).
     */
    public static void applyFulfillmentToOrder(Order order, Integer pancakeCode, String rawStatusName) {
        if (order == null) {
            return;
        }
        String cleanedName = PancakeFulfillmentStatus.sanitizeStoredName(rawStatusName);
        Optional<PancakeFulfillmentStatus> resolved = PancakeFulfillmentStatus.resolve(pancakeCode, cleanedName);
        if (resolved.isPresent()) {
            PancakeFulfillmentStatus status = resolved.get();
            order.setPancakeStatusCode(status.getPancakeCode());
            order.setPancakeStatusName(status.getDebaseValue());
            return;
        }
        if (pancakeCode != null) {
            order.setPancakeStatusCode(pancakeCode);
        }
        if (cleanedName != null) {
            order.setPancakeStatusName(cleanedName);
        }
    }

    public static String normalizeName(String statusName) {
        return PancakeFulfillmentStatus.normalizeName(statusName);
    }
}
