package vn.co.cake.controller.external.dto;

import lombok.Data;
import vn.co.cake.entity.Order;

@Data
public class ShippingAddress {
    private String address;
    private String commune_id;
    private String country_code;
    private String district_id;
    private String full_address;
    private String full_name;
    private String phone_number;
    private String post_code;
    private String province_id;
    
    public ShippingAddress() {}
    
    public ShippingAddress(Order order) {
        if (order != null) {
            this.full_name = order.getFullName();
            this.phone_number = order.getPhone();
            this.address = order.getShippingAddress();
            this.full_address = order.getShippingAddress();
        }
    }
}

