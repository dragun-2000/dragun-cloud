package vn.co.cake.request;

import lombok.Data;
import vn.co.cake.dto.OrderItem;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderDetailRequest implements Serializable {
    private static final long serialVersionUID = -4115973632750499107L;
    private Long orderId;
    private BigDecimal amount;
    private BigDecimal amountDiscount;
    private String paymentMethod;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String province;
    private String district;
    private String ward;
    private String note;
    private String voucher;
    
    private List<OrderItem> newOrderItems;
}
