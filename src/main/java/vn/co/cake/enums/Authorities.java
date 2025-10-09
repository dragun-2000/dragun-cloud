package vn.co.cake.enums;

import vn.co.cake.common.StringConst;
import lombok.Getter;

/**
 * Authorities
 */
@Getter
public enum Authorities {
    ROLE_ADMIN(StringConst.ZERO, StringConst.ROLE_ADMIN),
    ROLE_MANAGE(StringConst.ONE, StringConst.ROLE_MANAGE),
    ROLE_USER(StringConst.TWO, StringConst.ROLE_USER),
    ROLE_STAFF(StringConst.THREE, StringConst.ROLE_STAFF);

    private final String type;
    private final String text;
    
    public static Authorities getRole(String type) {
        switch (type) {
            case StringConst.ZERO:
                return ROLE_ADMIN;
            case StringConst.ONE:
                return ROLE_MANAGE;
            case StringConst.TWO:
                return ROLE_USER;
            default:
                return ROLE_STAFF;
        }
    }

    Authorities(String type, String text) {
        this.type = type;
        this.text = text;
    }
}
