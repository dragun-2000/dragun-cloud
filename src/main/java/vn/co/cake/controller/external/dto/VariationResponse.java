package vn.co.cake.controller.external.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VariationResponse {

    private String barcode;
    private List<String> composite_products;
    private String display_id;
    private List<Field> fields;
    private String id;
    private List<String> images;
    private Boolean is_hidden;
    private Boolean is_locked;
    private Boolean is_removed;
    private Boolean is_sell_negative_variation;
    private long last_imported_price;
    private Product product;
    private String product_id;
    private Long remain_quantity;
    private Long retail_price;
    private Long total_purchase_price;
    private List<VariationWarehouse> variations_warehouses; 
    private List<String> videos;  
    private int weight;
    private List<String> wholesale_price;  
    private Boolean success; 

    @Data
    public static class Field {
        private String id;
        private String keyValue;
        private String name;
        private String value;
    }
    
    @Data
    public static class Product {
        private List<Category> categories;
        private String display_id;
        private String image; // Assuming this can be null or an object
        private Boolean is_published; // Assuming this can be null or some object
        private String name;
        private String note_product;
    }
    
    @Data
    public static class VariationWarehouse {
        private long actual_remain_quantity;
        private String batch_position;
        private long pending_quantity;
        private long remain_quantity;
        private long returning_quantity;
        private String shelf_position;
        private long total_quantity;
        private long waiting_quantity;
        private String warehouse_id;
    }
    
    @Data
    public static class Category {
        private Integer id;
        private String name;
    }
}

