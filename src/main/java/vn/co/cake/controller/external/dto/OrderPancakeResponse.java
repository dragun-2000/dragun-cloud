package vn.co.cake.controller.external.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderPancakeResponse {
    private String order_link;
    private String bill_full_name;
    private String money_to_collect;
    private String link_confirm_order;
    private String tracking_link;
    private String inserted_at;
    private String status_name;
    private ShippingAddress shipping_address;
}


