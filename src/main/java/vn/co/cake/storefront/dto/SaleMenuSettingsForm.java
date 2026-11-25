package vn.co.cake.storefront.dto;

import lombok.Data;

@Data
public class SaleMenuSettingsForm {

    /** {@code SALE} or {@code BLACK_FRIDAY}. */
    private String saleMenuStyle;
}
