package vn.co.cake.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class ProductSearchRequest implements Serializable {
    private static final long serialVersionUID = -4115373602750199107L;
    private String name;
    private String code;
    private String subCode;
    private String color;
    private String sizeProduct;
    private String category;
}
