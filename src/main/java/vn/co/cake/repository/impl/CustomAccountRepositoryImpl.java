package vn.co.cake.repository.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import vn.co.cake.entity.Account;
import vn.co.cake.entity.QAccount;
import vn.co.cake.enums.Authorities;
import vn.co.cake.repository.BaseRepository;
import vn.co.cake.repository.CustomAccountRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * AccountRepositoryImpl
 */
@Repository
public class CustomAccountRepositoryImpl extends BaseRepository implements CustomAccountRepository {

    private final EntityManager entityManager;

    public CustomAccountRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Page<Account> findAllByDeletedIsFalseAndCondition(String keyword, String email, Pageable pageable) {
        JPAQuery<Account> query = new JPAQuery<>(entityManager);
        QAccount account = QAccount.account;

        BooleanBuilder where = new BooleanBuilder();
        where.and(account.deleted.eq(false));
        where.and(account.mailAddress.ne(email));
        if (!StringUtils.isBlank(keyword)) {
            where.and(account.fullName.containsIgnoreCase(keyword)
            .or(account.phone.containsIgnoreCase(keyword))
            .or(account.mailAddress.containsIgnoreCase(keyword))
            );
        }
        List<Account> accounts = query.from(account)
                .where(where)
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset())
                .orderBy(getOrderSpecifiers(pageable, Account.class))
                .fetch();

        return new PageImpl<>(accounts, pageable, query.fetchCount());

    }

    @Override
    public List<Account> findAllByDeletedIsFalseAndConditionForExport(String keyword, String email) {
        JPAQuery<Account> query = new JPAQuery<>(entityManager);
        QAccount account = QAccount.account;

        BooleanBuilder where = new BooleanBuilder();
        where.and(account.deleted.eq(false));
        where.and(account.mailAddress.ne(email));
        if (!StringUtils.isBlank(keyword)) {
            where.and(account.fullName.containsIgnoreCase(keyword)
            .or(account.phone.containsIgnoreCase(keyword))
            .or(account.mailAddress.containsIgnoreCase(keyword))
            );
        }
        return query.from(account)
                .where(where)
                .orderBy(account.id.desc())
                .fetch();
    }

    @Override
    public List<Long> findIdAllByDeletedIsFalseAndFullNameContaining(String name, String email) {
        JPAQuery<Account> query = new JPAQuery<>(entityManager);
        QAccount account = QAccount.account;

        BooleanBuilder where = new BooleanBuilder();
        where.and(account.deleted.eq(false));
        where.and(account.authorities.in(Authorities.ROLE_STAFF.name()));
        where.and(account.mailAddress.ne(email));
        if (!StringUtils.isBlank(name)) {
            where.and(account.fullName.containsIgnoreCase(name));
        }
        return query.select(account.id)
                .from(account)
                .where(where)
                .fetch();

    }

    @Override
    public Account findAccountForAdmin(String email) {
        JPAQuery<Account> query = new JPAQuery<>(entityManager);
        QAccount account = QAccount.account;

        BooleanBuilder where = new BooleanBuilder();
        where.and(account.deleted.eq(false));
        where.and(account.logout.eq(false));
        where.and(account.authorities.in(Authorities.ROLE_STAFF.name(), Authorities.ROLE_ADMIN.name()));
        where.and(account.mailAddress.eq(email).or(account.phone.eq(email)));
        return query.from(account)
                .where(where)
                .fetchFirst();
    }
    
    @Override
    public Account findAccountForMember(String phone) {
        JPAQuery<Account> query = new JPAQuery<>(entityManager);
        QAccount account = QAccount.account;

        BooleanBuilder where = new BooleanBuilder();
        where.and(account.deleted.eq(false));
        where.and(account.logout.eq(false));
        where.and(account.authorities.in(Authorities.ROLE_USER.name()));
        where.and(account.phone.eq(phone));
        return query.from(account)
                .where(where)
                .fetchFirst();
    }
    
    @Override
    public Account findAccountForgetPassword(String email) {
        JPAQuery<Account> query = new JPAQuery<>(entityManager);
        QAccount account = QAccount.account;

        BooleanBuilder where = new BooleanBuilder();
        where.and(account.deleted.eq(false));
        where.and(account.logout.eq(false));
        where.and(account.authorities.in(Authorities.ROLE_USER.name()));
        where.and(account.mailAddress.eq(email));
        return query.from(account)
                .where(where)
                .fetchFirst();
    }
}
