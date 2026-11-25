package vn.co.cake.storefront.entity;

/** How the storefront's SALE menu entry is displayed; its link and product filter stay {@code /products/group/SALE}. */
public enum SaleMenuStyle {
    SALE("header.sale", ""),
    BLACK_FRIDAY("header.blackFriday", "is-black-friday");

    private final String labelKey;
    private final String cssClass;

    SaleMenuStyle(String labelKey, String cssClass) {
        this.labelKey = labelKey;
        this.cssClass = cssClass;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public String getCssClass() {
        return cssClass;
    }

    public static SaleMenuStyle parse(String value) {
        if (value != null) {
            for (SaleMenuStyle style : values()) {
                if (style.name().equals(value.trim())) {
                    return style;
                }
            }
        }
        throw new IllegalArgumentException("Kiểu hiển thị menu SALE không hợp lệ");
    }
}
