package vn.co.cake.enums;

import lombok.Getter;
import vn.co.cake.common.StringConst;

/**
 * Categories
 */
@Getter
public enum Categories {
    WEEKLY(StringConst.ZERO, StringConst.WEEKLY),
    ARRIVAL(StringConst.ONE, StringConst.ARRIVAL),
    NEW_IN(StringConst.TWO, StringConst.NEW_IN);

    private final String type;
    private final String text;
    
    public static Categories getCategory(String type) {
        switch (type) {
            case StringConst.ZERO:
                return WEEKLY;
            case StringConst.ONE:
                return ARRIVAL;
            default:
                return NEW_IN;
        }
    }

    Categories(String type, String text) {
        this.type = type;
        this.text = text;
    }
}
