package vn.co.cake.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ShippingDto {
    private String name;
    private int shippingFee;
    private int discountPercent;
    private LocalDate startDate;  
    private LocalDate endDate; 
}
