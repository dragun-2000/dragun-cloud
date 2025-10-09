package vn.co.cake.controller.external.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderExtendDTO {

    private Long id;
    private String type;
    private List<OrderItemExtendDTO> items;
    private CustomerInfoExtendDTO customerInfo;
    private double total;
    private String status;
    private String createdAt;
    private String updatedAt;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItemExtendDTO {
        private String productId;
        private int quantity;
        private double price;
        private double subtotal;
        private String imageUrl;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CustomerInfoExtendDTO {
        private String name;
        private String phone;
        private String address;
    }
}
