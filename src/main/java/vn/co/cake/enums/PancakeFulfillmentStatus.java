package vn.co.cake.enums;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import vn.co.cake.constants.OrderConstants;

/**
 * Fulfillment status mirrored from Pancake POS into Debase
 * ({@code orders.pancake_status_code} / {@code pancake_status_name}).
 * Not the same as lifecycle {@link OrderStatus} ({@code orders.status}).
 */
public enum PancakeFulfillmentStatus {

    FULFILLMENT_NEW(0, "Mới", true),
    CONFIRMED(1, "Đã xác nhận", true),
    SHIPPED(2, "Đã gửi hàng", true),
    DELIVERED(3, "Đã nhận", false),
    CANCELED(6, "Đã hủy", false),
    RETURNING(7, "Đang hoàn", false),
    PACKING(8, "Đang đóng hàng", true),
    WAITING_TRANSFER(9, "Chờ chuyển hàng", true),
    WAITING_STOCK(11, "Chờ hàng", false);

    private static final Pattern FAKE_STATUS_NAME = Pattern.compile("^Trạng thái #\\d+$");

    private static final Map<Integer, PancakeFulfillmentStatus> BY_CODE;
    private static final Map<String, PancakeFulfillmentStatus> BY_NORMALIZED_NAME;
    private static final List<String> HOURLY_DEBASE_VALUES;
    private static final List<Integer> HOURLY_PANCAKE_CODES;

    static {
        Map<Integer, PancakeFulfillmentStatus> byCode = new HashMap<>();
        Map<String, PancakeFulfillmentStatus> byName = new HashMap<>();
        List<String> hourlyNames = new ArrayList<>();
        List<Integer> hourlyCodes = new ArrayList<>();
        for (PancakeFulfillmentStatus status : values()) {
            byCode.put(status.pancakeCode, status);
            byName.put(normalizeName(status.debaseValue), status);
            if (status.hourlySync) {
                hourlyNames.add(status.debaseValue);
                hourlyCodes.add(status.pancakeCode);
            }
        }
        // Pancake / legacy aliases (normalized, no accent)
        putAlias(byName, "new", FULFILLMENT_NEW);
        putAlias(byName, "moi", FULFILLMENT_NEW);
        putAlias(byName, "confirmed", CONFIRMED);
        putAlias(byName, "da xac nhan", CONFIRMED);
        putAlias(byName, "shipped", SHIPPED);
        putAlias(byName, "da gui", SHIPPED);
        putAlias(byName, "da gui hang", SHIPPED);
        putAlias(byName, "delivered", DELIVERED);
        putAlias(byName, "da nhan", DELIVERED);
        putAlias(byName, "da nhan hang", DELIVERED);
        putAlias(byName, "completed", DELIVERED);
        putAlias(byName, "hoan thanh", DELIVERED);
        putAlias(byName, "canceled", CANCELED);
        putAlias(byName, "cancelled", CANCELED);
        putAlias(byName, "da huy", CANCELED);
        putAlias(byName, "returning", RETURNING);
        putAlias(byName, "dang hoan", RETURNING);
        putAlias(byName, "packing", PACKING);
        putAlias(byName, "dang dong hang", PACKING);
        putAlias(byName, "waiting transfer", WAITING_TRANSFER);
        putAlias(byName, "cho chuyen hang", WAITING_TRANSFER);
        putAlias(byName, "waiting stock", WAITING_STOCK);
        putAlias(byName, "cho hang", WAITING_STOCK);

        BY_CODE = Collections.unmodifiableMap(byCode);
        BY_NORMALIZED_NAME = Collections.unmodifiableMap(byName);
        HOURLY_DEBASE_VALUES = Collections.unmodifiableList(hourlyNames);
        HOURLY_PANCAKE_CODES = Collections.unmodifiableList(hourlyCodes);
    }

    private final int pancakeCode;
    private final String debaseValue;
    private final boolean hourlySync;

    PancakeFulfillmentStatus(int pancakeCode, String debaseValue, boolean hourlySync) {
        this.pancakeCode = pancakeCode;
        this.debaseValue = debaseValue;
        this.hourlySync = hourlySync;
    }

    public int getPancakeCode() {
        return pancakeCode;
    }

    public String getDebaseValue() {
        return debaseValue;
    }

    public boolean needsHourlySync() {
        return hourlySync;
    }

    public static Optional<PancakeFulfillmentStatus> fromPancakeCode(Integer code) {
        if (code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_CODE.get(code));
    }

    public static Optional<PancakeFulfillmentStatus> fromStoredName(String name) {
        String cleaned = sanitizeStoredName(name);
        if (StringUtils.isBlank(cleaned)) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_NORMALIZED_NAME.get(normalizeName(cleaned)));
    }

    /**
     * Prefer Pancake numeric code; fall back to stored/API name (Debase label or raw alias).
     */
    public static Optional<PancakeFulfillmentStatus> resolve(Integer pancakeCode, String statusName) {
        Optional<PancakeFulfillmentStatus> byCode = fromPancakeCode(pancakeCode);
        if (byCode.isPresent()) {
            return byCode;
        }
        return fromStoredName(statusName);
    }

    public static List<String> hourlySyncDebaseValues() {
        return HOURLY_DEBASE_VALUES;
    }

    public static List<Integer> hourlySyncPancakeCodes() {
        return HOURLY_PANCAKE_CODES;
    }

    public static boolean needsHourlySync(Integer pancakeCode, String pancakeStatusName) {
        if (pancakeCode == null && StringUtils.isBlank(sanitizeStoredName(pancakeStatusName))) {
            return true;
        }
        Optional<PancakeFulfillmentStatus> resolved = resolve(pancakeCode, pancakeStatusName);
        if (resolved.isPresent()) {
            return resolved.get().needsHourlySync();
        }
        // Unknown name but hourly code (legacy rows) — keep polling.
        return pancakeCode != null && HOURLY_PANCAKE_CODES.contains(pancakeCode);
    }

    public static String displayLabel(Integer pancakeCode) {
        return fromPancakeCode(pancakeCode).map(PancakeFulfillmentStatus::getDebaseValue).orElse(null);
    }

    /**
     * UI label: fulfillment enum → stored name → Debase lifecycle.
     */
    public static String resolveDisplayStatus(String lifecycleStatus, Integer pancakeCode, String pancakeStatusName) {
        Optional<PancakeFulfillmentStatus> fulfillment = resolve(pancakeCode, pancakeStatusName);
        if (fulfillment.isPresent()) {
            return fulfillment.get().getDebaseValue();
        }
        String cleaned = sanitizeStoredName(pancakeStatusName);
        if (StringUtils.isNotBlank(cleaned)) {
            return cleaned;
        }
        return mapLifecycleStatus(lifecycleStatus);
    }

    public enum CancelMode {
        SELF,
        CONTACT,
        NONE
    }

    /**
     * Cancel UX: requires Debase lifecycle NEW plus fulfillment still "Mới" (or not mirrored yet).
     */
    public static CancelMode resolveCancelMode(String lifecycleStatus, Integer pancakeCode, String pancakeStatusName) {
        if (!OrderConstants.STATUS_NEW.equals(lifecycleStatus)) {
            return CancelMode.NONE;
        }
        Optional<PancakeFulfillmentStatus> fulfillment = resolve(pancakeCode, pancakeStatusName);
        if (fulfillment.isPresent()) {
            if (fulfillment.get() == CANCELED) {
                return CancelMode.NONE;
            }
            if (fulfillment.get() == FULFILLMENT_NEW) {
                return CancelMode.SELF;
            }
            return CancelMode.CONTACT;
        }
        if (pancakeCode == null && StringUtils.isBlank(sanitizeStoredName(pancakeStatusName))) {
            return CancelMode.SELF;
        }
        return CancelMode.CONTACT;
    }

    /** Strip fake names produced when API only returns a numeric status. */
    public static String sanitizeStoredName(String statusName) {
        if (StringUtils.isBlank(statusName)) {
            return null;
        }
        String trimmed = statusName.trim();
        if (FAKE_STATUS_NAME.matcher(trimmed).matches()) {
            return null;
        }
        return trimmed;
    }

    public static String normalizeName(String statusName) {
        if (StringUtils.isBlank(statusName)) {
            return "";
        }
        String noAccent = Normalizer.normalize(statusName.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return noAccent.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private static void putAlias(Map<String, PancakeFulfillmentStatus> map, String alias, PancakeFulfillmentStatus status) {
        map.put(normalizeName(alias), status);
    }

    private static String mapLifecycleStatus(String status) {
        if (StringUtils.isBlank(status)) {
            return "—";
        }
        if (OrderConstants.STATUS_NEW.equals(status)) {
            return "Đã đồng bộ Pancake";
        }
        if (OrderConstants.STATUS_SYNC_FAIL.equals(status)) {
            return "Đồng bộ Pancake thất bại";
        }
        if (OrderConstants.STATUS_PENDING_SYNC.equals(status)) {
            return "Chờ đồng bộ Pancake";
        }
        if (OrderConstants.STATUS_AWAITING_PAYMENT.equals(status)) {
            return "Chờ thanh toán";
        }
        if (OrderConstants.STATUS_PAYMENT_RECEIVED_UNFULFILLABLE.equals(status)) {
            return "Đã nhận tiền - cần xử lý tồn kho";
        }
        if (OrderConstants.STATUS_CANCELLED.equals(status)) {
            return CANCELED.getDebaseValue();
        }
        return status;
    }
}
