package vn.co.cake.enums;

import lombok.Getter;

@Getter
public enum OrderStatus {
    NEW("NEW"),
    SYNC_FAIL("SYNC_FAIL"),
    PENDING_SYNC("PENDING_SYNC");
    
    private final String value;
    
    OrderStatus(String value) {
        this.value = value;
    }
}

