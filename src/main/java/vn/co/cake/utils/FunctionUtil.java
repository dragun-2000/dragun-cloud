package vn.co.cake.utils;

import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import vn.co.cake.dto.CartForm;
import vn.co.cake.dto.OrderItem;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class FunctionUtil {
    public static String joinListMail(List<String> list) {
        if (list.isEmpty()) return "";

        StringBuilder sb = new StringBuilder(list.get(0));

        for (int i = 1; i < list.size(); i++) {
            sb.append("、");
            sb.append(list.get(i));
            if (i == 9) break;
        }

        if (list.size() > 10) {
            sb.append("、。。。");
        }

        return sb.toString();
    }

    public static boolean allPropertiesIsNull(Object target) {
        return Arrays.stream(target.getClass()
                        .getDeclaredFields())
                .peek(f -> f.setAccessible(true))
                .map(f -> getFieldValue(f, target))
                .allMatch(Objects::isNull);
    }

    private static Object getFieldValue(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void updateCartQuantity(Model model, CartForm cartForm) {
        int totalQuantity = 0;
        if (cartForm != null && !CollectionUtils.isEmpty(cartForm.getOrderItems())) {
            List<OrderItem> orderItems = cartForm.getOrderItems();
            totalQuantity = orderItems.stream().mapToInt(OrderItem::getQuantity).sum();
        }
        model.addAttribute("totalCartItems", totalQuantity);
    }
}
