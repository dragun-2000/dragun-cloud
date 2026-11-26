package vn.co.cake.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/** Product-list sort orders; a customer's choice travels as the {@code sortBy} query parameter. */
public enum ProductSortOption {
    /** Internal order (NEW IN, category, search, admin): not offered in the customer sort control. */
    NEWEST(null),
    NAME_ASC("home.sort.nameAsc"),
    PRICE_DESC("home.sort.priceDesc"),
    PRICE_ASC("home.sort.priceAsc"),
    DISCOUNT_DESC("home.sort.discountDesc");

    /** The choices of the home ALL PRODUCTS sort control, in display order. */
    public static final List<ProductSortOption> CUSTOMER_OPTIONS =
            Collections.unmodifiableList(Arrays.asList(NAME_ASC, PRICE_DESC, PRICE_ASC, DISCOUNT_DESC));

    /** What the home sort control selects when the customer has not chosen. */
    public static final ProductSortOption CUSTOMER_DEFAULT = DISCOUNT_DESC;

    private final String messageKey;

    ProductSortOption(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }

    /** Unknown or missing values fall back to {@link #NEWEST}, the order the store has always used. */
    public static ProductSortOption fromParam(String value) {
        return fromParam(value, NEWEST);
    }

    public static ProductSortOption fromParam(String value, ProductSortOption fallback) {
        if (StringUtils.isBlank(value)) {
            return fallback;
        }
        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(option -> option.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(fallback);
    }

    /** A customer-submitted value, limited to {@link #CUSTOMER_OPTIONS}; anything else gives {@link #CUSTOMER_DEFAULT}. */
    public static ProductSortOption fromCustomerParam(String value) {
        ProductSortOption option = fromParam(value, CUSTOMER_DEFAULT);
        return CUSTOMER_OPTIONS.contains(option) ? option : CUSTOMER_DEFAULT;
    }
}
