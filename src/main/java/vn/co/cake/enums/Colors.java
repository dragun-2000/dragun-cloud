package vn.co.cake.enums;

import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum Colors {
    RED("Đỏ", "#FF0000"),    // Đỏ
    GREEN("Xanh lá", "#008000"),  // Xanh lá 
    BLUE("Xanh dương", "#0000FF"),   // Xanh dương 
    BLACK("Đen", "#000000"),  // Đen 
    WHITE("Trắng", "#FFFFFF"),  // Trắng 
    YELLOW("Vàng", "#FFFF00"), // Vàng 
    ORANGE("Cam", "#FFA500"), // Cam 
    PURPLE("Tím", "#800080"), // Tím 
    PINK("Hồng", "#FFC0CB"),   // Hồng 
    GRAY("Xám", "#808080");   // Xám 
    
    private final String value;
    private final String code;
    Colors(String value, String code) {
        this.value = value;
        this.code = code;
    }
    
    public static List<String> getValue() {
        return Arrays.stream(Colors.values()).map(Colors::name).collect(Collectors.toList());
    }
    
    public static String getCodeByName(String value) {
        return Arrays.stream(Colors.values()).filter(name -> name.name().equals(value)).map(color -> color.code).findFirst().orElse("");
    }
}