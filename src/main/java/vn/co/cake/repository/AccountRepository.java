package vn.co.cake.repository;

import vn.co.cake.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AccountRepository
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long>, CustomAccountRepository {

    Account findByIdAndDeletedFalse(Long accountId);

    Account findByIdAndDeletedFalseAndLogoutIsFalse(Long id);

    Account findByIdAndHash(Long accountId, String hash);

    Account findFirstByIdAndDeletedIsFalse(Long id);

    List<Account> findAllByAuthoritiesAndMailAddressIn(String role, List<String> usernames);
}
