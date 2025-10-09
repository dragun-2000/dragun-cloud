package vn.co.cake.controller.external.dto;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import vn.co.cake.entity.Product;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class ProductPancakeRequest {
    private ProductCake product;

    public ProductPancakeRequest(Product product) {
        this.product = new ProductCake(product);
    }

    @Data
    public static class ProductCake {
        private String name;
        private String note_product;
        private List<ProductAttribute> product_attributes;
        private List<Integer> tags;
        private List<Variation> variations;
        private int weight;
        private String custom_id;
        private boolean is_published = true;
        private List<VariationWarehouse> variationsWarehouses;

        public ProductCake(Product product) {
            this.name = product.getName();
            this.note_product = product.getDescription();

            List<ProductAttribute> attributes = new ArrayList<>();
            String colors = product.getColors();
            List<String> colorModel = new ArrayList<>();
            if (StringUtils.isNotEmpty(colors)) {
                colorModel = Arrays.asList(colors.split(","));
                attributes.add(new ProductAttribute("Màu", colorModel));
            }

            String sizes = product.getSizes();
            List<String> sizeModel = new ArrayList<>();
            if (StringUtils.isNotEmpty(sizes)) {
                sizeModel = Arrays.asList(sizes.split(","));
                attributes.add(new ProductAttribute("Size", sizeModel));
            }
            
            Set<SizeColorPair> sizeColorPairs = new HashSet<>();
            for (String color : colorModel) {
                for (String size : sizeModel) {
                    sizeColorPairs.add(new SizeColorPair(size, color));
                }
            }
            
            this.product_attributes = attributes;
            this.weight = 100;
            this.custom_id = product.getCode();
            List<Variation> variationList = new ArrayList<>();
            for (SizeColorPair sizeColorPair : sizeColorPairs) {
                Variation variation = new Variation(product, sizeColorPair);
                variationList.add(variation);
            }
            this.variations = variationList;
            this.variationsWarehouses = Collections.singletonList(new VariationWarehouse());
            this.tags = Arrays.asList(12, 13);
        }
    }

    @Data
    public static class Variation {
        private List<Field> fields;
        private List<String> images;
        private int last_imported_price;
        private int retail_price;
        private int weight;
        private String barcode;
        private String custom_id;
        private boolean is_hidden = false;

        public Variation(Product product, SizeColorPair sizeColorPair) {
            List<Field> fieldList = new ArrayList<>();
            Field colorField = new Field("Màu", sizeColorPair.getColor());
            Field sizeField = new Field("Size", sizeColorPair.getSize());
            fieldList.add(colorField);
            fieldList.add(sizeField);
            this.fields = fieldList;
            this.images = Collections.singletonList(product.getImage());
            this.last_imported_price = product.getPrice().intValue() / 2;
            this.retail_price = product.getPrice().intValue();
            this.weight = 100;
            this.barcode = product.getCode() + "-" + sizeColorPair.getColor() + "-" + sizeColorPair.getSize();
            this.custom_id = product.getSubCode() + "-" + sizeColorPair.getColor() + "-" + sizeColorPair.getSize();
        }
    }

    @Data
    public static class ProductAttribute {
        private String name;
        private List<String> values;

        public ProductAttribute(String name, List<String> values) {
            this.name = name;
            this.values = values;
        }
    }

    @Data
    public static class VariationWarehouse {
        private int remain_quantity;
        private String warehouse_id;
        private String batch_position;
        private String shelf_position;

        public VariationWarehouse() {
            this.remain_quantity = 10;
            this.warehouse_id = "635ec084-e3ff-4137-b61d-afdafff7d5b5";
            this.batch_position = "lô1";
            this.shelf_position = "1.1";
        }
    }

    @Data
    public static class Field {
        private String name;
        private String value;

        public Field(String field, String value) {
            this.name = field;
            this.value = value;
        }
    }
    
    @Data
    static class SizeColorPair {
        private String size;
        private String color;

        public SizeColorPair(String size, String color) {
            this.size = size;
            this.color = color;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            SizeColorPair that = (SizeColorPair) obj;
            return size.equals(that.size) && color.equals(that.color);
        }
    }
}
