package vn.co.cake.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import vn.co.cake.dto.CartForm;
import vn.co.cake.dto.OrderItem;

/**
 * Characterization tests for {@link FunctionUtil}.
 */
class FunctionUtilTest {

    private static final String TOTAL_CART_ITEMS = "totalCartItems";

    static class AllNullable {
        private String first;
        private Integer second;
    }

    static class WithConstant {
        private static final String CONSTANT = "c";
        private String value;
    }

    private static List<String> mails(int count) {
        List<String> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(String.format("m%d", i));
        }
        return list;
    }

    @Test
    void joinListMailJoinsUpToTenWithJapaneseComma() {
        assertEquals("", FunctionUtil.joinListMail(Collections.emptyList()));
        assertEquals("a", FunctionUtil.joinListMail(Collections.singletonList("a")));
        assertEquals("a、b", FunctionUtil.joinListMail(Arrays.asList("a", "b")));
        assertEquals("m1、m2、m3、m4、m5、m6、m7、m8、m9、m10", FunctionUtil.joinListMail(mails(10)));
        // current behavior: more than 10 entries -> first 10 plus "、。。。"
        assertEquals("m1、m2、m3、m4、m5、m6、m7、m8、m9、m10、。。。", FunctionUtil.joinListMail(mails(12)));
        assertThrows(NullPointerException.class, () -> FunctionUtil.joinListMail(null));
    }

    @Test
    void allPropertiesIsNullInspectsAllDeclaredFieldsIncludingStatic() {
        AllNullable nullable = new AllNullable();
        assertTrue(FunctionUtil.allPropertiesIsNull(nullable));
        nullable.second = 0;
        assertFalse(FunctionUtil.allPropertiesIsNull(nullable));
        // current behavior: static fields are counted, so a non-null constant makes it false
        assertEquals("c", WithConstant.CONSTANT);
        assertFalse(FunctionUtil.allPropertiesIsNull(new WithConstant()));
    }

    @Test
    void updateCartQuantitySumsItemQuantities() {
        ExtendedModelMap model = new ExtendedModelMap();
        FunctionUtil.updateCartQuantity(model, null);
        assertEquals(0, model.get(TOTAL_CART_ITEMS));

        FunctionUtil.updateCartQuantity(model, new CartForm());
        assertEquals(0, model.get(TOTAL_CART_ITEMS));

        OrderItem first = new OrderItem();
        first.setQuantity(2);
        OrderItem second = new OrderItem();
        second.setQuantity(3);
        FunctionUtil.updateCartQuantity(model, new CartForm(Arrays.asList(first, second)));
        assertEquals(5, model.get(TOTAL_CART_ITEMS));
    }
}
