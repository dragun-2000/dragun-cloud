package vn.co.cake.repository.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import vn.co.cake.entity.Product;
import vn.co.cake.entity.QProduct;
import vn.co.cake.enums.ProductSortOption;
import vn.co.cake.repository.BaseRepository;
import vn.co.cake.repository.CustomProductRepository;
import vn.co.cake.request.ProductSearchRequest;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;

@Repository
public class CustomProductRepositoryImpl extends BaseRepository implements CustomProductRepository {
    private final EntityManager entityManager;

    public CustomProductRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    @Override
    public Page<Product> getAllByCondition(ProductSearchRequest searchRequest, Pageable pageable, boolean isAdmin) {
        JPAQuery<Product> query = new JPAQuery<>(entityManager);
        QProduct qProduct = QProduct.product;

        BooleanBuilder where = new BooleanBuilder();
        if (!isAdmin) {
            where.and(qProduct.deleted.eq(false));
        }

        if (StringUtils.isNotEmpty(searchRequest.getName())) {
            where.and(qProduct.name.containsIgnoreCase(searchRequest.getName().trim()));
        }
        if (StringUtils.isNotEmpty(searchRequest.getCode())) {
            where.and(qProduct.code.containsIgnoreCase(searchRequest.getCode().trim()));
        }
        if (StringUtils.isNotEmpty(searchRequest.getColor())) {
            where.and(qProduct.colors.containsIgnoreCase(searchRequest.getColor().trim()));
        }
        if ("SALE".equals(searchRequest.getCategory()) || "BLACK FRIDAY".equals(searchRequest.getCategory())) {
            where.and(qProduct.discount.gt(0));
        } else if (StringUtils.isNotEmpty(searchRequest.getCategory()) && !"ALL PRODUCTS".equals(searchRequest.getCategory())) {
            where.and(qProduct.categories.containsIgnoreCase(searchRequest.getCategory().trim()));
        }  
        if (StringUtils.isNotEmpty(searchRequest.getSubCode())) {
            where.and(qProduct.subCode.containsIgnoreCase(searchRequest.getSubCode().trim()));
        }
        if (StringUtils.isNotEmpty(searchRequest.getSizeProduct())) {
            where.and(qProduct.sizes.containsIgnoreCase(searchRequest.getSizeProduct().trim()));
        }

        List<Product> products = query.from(qProduct)
                .where(where)
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset())
                .orderBy(orderSpecifiers(ProductSortOption.fromParam(searchRequest.getSortBy())))
                .fetch();

        return new PageImpl<>(products, pageable, query.fetchCount());
    }

    static OrderSpecifier<?>[] orderSpecifiers(ProductSortOption option) {
        QProduct qProduct = QProduct.product;
        // Customers compare what they pay, so price sorts use the price after the product discount (%).
        NumberExpression<BigDecimal> sellingPrice = Expressions.numberTemplate(BigDecimal.class,
                "{0} * (100 - coalesce({1}, 0))", qProduct.price, qProduct.discount);
        OrderSpecifier<?> newest = qProduct.updated.desc();
        OrderSpecifier<?> stable = qProduct.id.desc();

        switch (option) {
            case PRICE_ASC:
                return new OrderSpecifier<?>[] {sellingPrice.asc(), newest, stable};
            case PRICE_DESC:
                return new OrderSpecifier<?>[] {sellingPrice.desc(), newest, stable};
            case DISCOUNT_DESC:
                return new OrderSpecifier<?>[] {qProduct.discount.coalesce(BigDecimal.ZERO).desc(), newest, stable};
            case NAME_ASC:
                return new OrderSpecifier<?>[] {qProduct.name.asc(), newest, stable};
            case NEWEST:
            default:
                return new OrderSpecifier<?>[] {newest, stable};
        }
    }
}
