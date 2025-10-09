package vn.co.cake.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UpdateStockResponse {
    @JsonProperty("actual_remain_quantity")
    private int actualRemainQuantity;

    @JsonProperty("change_quantity")
    private int changeQuantity;

    @JsonProperty("inserted_at")
    private String insertedAt;

    @JsonProperty("is_actual_remain_quantity")
    private boolean isActualRemainQuantity;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("remain_quantity")
    private Long remainQuantity;

    @JsonProperty("type")
    private String type;

    @JsonProperty("variation_id")
    private String variationId;

    @JsonProperty("warehouse_id")
    private String warehouseId;
}
