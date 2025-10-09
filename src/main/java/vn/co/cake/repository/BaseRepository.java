package vn.co.cake.repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilder;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * BaseRepository
 */
@Repository
public class BaseRepository {

    protected OrderSpecifier[] getOrderSpecifiers(Pageable pageable, Class className) {
        String tableName = className.getSimpleName();
        final String orderVariable = String.valueOf(Character.toLowerCase(tableName.charAt(0))).concat(tableName.substring(1));

        return pageable.getSort().stream()
                .map(order -> new OrderSpecifier(
                        Order.valueOf(order.getDirection().toString()),
                        new PathBuilder(className, orderVariable).get(order.getProperty()))
                )
                .toArray(OrderSpecifier[]::new);
    }
}
