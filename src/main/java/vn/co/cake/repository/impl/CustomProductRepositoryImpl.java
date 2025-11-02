package vn.co.cake.repository.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import vn.co.cake.entity.Product;
import vn.co.cake.entity.QProduct;
import vn.co.cake.repository.BaseRepository;
import vn.co.cake.repository.CustomProductRepository;
import vn.co.cake.request.ProductSearchRequest;

import javax.persistence.EntityManager;
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
        if ("SALE".equals(searchRequest.getCategory())) {
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
                .orderBy(qProduct.updated.desc())
                .fetch();

        return new PageImpl<>(products, pageable, query.fetchCount());
    }
}
