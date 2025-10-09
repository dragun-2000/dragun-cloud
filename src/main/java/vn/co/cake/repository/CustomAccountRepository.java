package vn.co.cake.repository;

import vn.co.cake.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * CustomAccountRepository
 */
public interface CustomAccountRepository {

    Page<Account> findAllByDeletedIsFalseAndCondition(String username, String email, Pageable pageable);

    List<Long> findIdAllByDeletedIsFalseAndFullNameContaining(String name, String email);

    Account findAccountForAdmin(String phone);
    
    Account findAccountForMember(String phone);
    
    Account findAccountForgetPassword(String email);
}
