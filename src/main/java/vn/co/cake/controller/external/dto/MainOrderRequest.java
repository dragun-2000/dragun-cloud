package vn.co.cake.controller.external.dto;

import lombok.Data;
import vn.co.cake.entity.*;
import vn.co.cake.utils.OrderPricingUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class MainOrderRequest {
    private String bill_full_name;
    private String bill_phone_number;
    private boolean is_free_shipping;
    private boolean received_at_shop;
    private String page_id;
    private int account;
    private String account_name;
    private String assigning_seller_id;
    private List<Item> items;
    private String note;
    private String note_print;
    private int returned_reason;
    private String warehouse_id;
    private ShippingAddress shipping_address;
    private int shipping_fee;
    private int shop_id;
    private int discount;
    private int prepaid;
    private String custom_id;
    private WarehouseInfo warehouse_info;
    private List<ActivatedPromotionAdvance> activatedPromotionAdvances;

    public MainOrderRequest() {}

    public MainOrderRequest(Order order, PancakeProperties properties, Warehouse warehouse, List<OrderItem> orderItems) {
        this.bill_full_name = order.getFullName();
        this.bill_phone_number = order.getPhone();
        this.is_free_shipping = true;
        this.received_at_shop = false;
        this.page_id = properties.getPageId();
        this.assigning_seller_id = properties.getSellerId();
        this.account = 1;
        this.account_name = order.getFullName();
        this.note = String.format("WEB + %s tiền phí ship setting trên web Debase", order.getShippingFee());
        this.note_print = order.getNote();
        this.returned_reason = 0;
        this.warehouse_id = properties.getWarehouseId();
        this.shipping_fee = order.getShippingFee().intValue();
        this.shop_id = Integer.parseInt(properties.getShopId());
        this.discount = this.discount(orderItems);
        if (order.getPrepaid() != null && order.getPrepaid().signum() > 0) {
            this.prepaid = order.getPrepaid().intValue();
        }
        this.custom_id = order.getCode();
        this.items = orderItems.stream().map(Item::new).collect(Collectors.toList());
        this.shipping_address = new ShippingAddress(order);
        this.warehouse_info = new WarehouseInfo(warehouse);
    }
    
    private int discount(List<OrderItem> orderItems) {
        return OrderPricingUtil.sumOrderDiscountAmount(orderItems);
    }
}

@Data
class Item {
    private int discount_each_product;
    private boolean is_bonus_product;
    private boolean is_discount_percent;
    private boolean is_wholesale;
    private boolean one_time_product;
    private int quantity;
    private String variation_id;
    private String product_id;
    private VariationInfo variation_info;

    public Item() {}
    public Item(OrderItem orderItem) {
        this.discount_each_product = 0;
        this.is_bonus_product = false;
        this.is_discount_percent = false;
        this.is_wholesale = false;
        this.one_time_product = false;
        this.quantity = orderItem.getQuantity();
        this.variation_id = orderItem.getVariation().getVariationId();
        this.product_id = orderItem.getVariation().getPancakeProductId();
        this.variation_info = new VariationInfo(orderItem);
    }
}

@Data
class VariationInfo {
    private String detail;
    private String fields;
    private String display_id;
    private String name;
    private String product_display_id;
    private int retail_price;
    private int weight;

    public VariationInfo() {}

    public VariationInfo(OrderItem orderItem) {
        Variation variation = orderItem.getVariation();
        this.detail = null;
        this.fields = null;
        this.display_id = variation.getDisplayId();
        this.name = variation.getName();
        this.product_display_id = variation.getDisplayId();
        if (orderItem.getPrice() != null && orderItem.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            this.retail_price = orderItem.getPrice().intValue();
        } else if (variation.getRetailPrice() != null) {
            this.retail_price = variation.getRetailPrice().intValue();
        } else {
            this.retail_price = 0;
        }
        this.weight = 200;
    }
}

// ShippingAddress class moved to separate file: ShippingAddress.java

@Data
class WarehouseInfo {
    private String districtId;
    private String fullAddress;
    private String name;
    private String phoneNumber;
    private String provinceId;
    public WarehouseInfo() {}
    public WarehouseInfo(Warehouse warehouse) {
        this.districtId = warehouse.getDistrictId();
        this.fullAddress = warehouse.getFullAddress();
        this.name = warehouse.getName();
        this.phoneNumber = warehouse.getPhoneNumber();
        this.provinceId = warehouse.getProvinceId();
    }
}

@Data
class ActivatedPromotionAdvance {
    private String promotionAdvanceId;
    private boolean isActivated;
}