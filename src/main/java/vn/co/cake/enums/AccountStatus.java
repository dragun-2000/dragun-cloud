package vn.co.cake.enums;

import vn.co.cake.common.StringConst;
import lombok.Getter;

/**
 * AccountStatus
 */
@Getter
public enum AccountStatus {

    PROVISIONAL(StringConst.ZERO, StringConst.PROVISIONAL),
    VALID(StringConst.ONE, StringConst.VALID),
    LOCKING(StringConst.TWO, StringConst.LOCKING);

    private final String type;
    private final String text;

    AccountStatus(String type, String text) {
        this.type = type;
        this.text = text;
    }
}
