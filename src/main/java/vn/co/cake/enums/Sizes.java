package vn.co.cake.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum Sizes {
    S,    
    M,  
    L,  
    XL, 
    XXL;
    
    public static List<String> getValue() {
        return Arrays.stream(Sizes.values()).map(Sizes::name).collect(Collectors.toList());
    }
}