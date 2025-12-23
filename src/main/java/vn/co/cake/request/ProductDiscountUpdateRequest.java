package vn.co.cake.request;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductDiscountUpdateRequest implements Serializable {
    private static final long serialVersionUID = -4115373602750199108L;
    private List<Long> productIds;
    private BigDecimal discount;
}

