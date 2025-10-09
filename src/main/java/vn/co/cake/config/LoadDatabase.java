package vn.co.cake.config;

import vn.co.cake.entity.Account;
import vn.co.cake.enums.AccountStatus;
import vn.co.cake.enums.Authorities;
import vn.co.cake.repository.AccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * LoadDatabase
 */
@Configuration
public class LoadDatabase {

    private static final String USER_NAME = "admin";

    private static final String PASSWORD = "abc@1234";

    private static final String MAIL_ADDRESS = "admin@gmail.com";
    private static final String PHONE = "0367209194";
    private static final String ADDRESS = "Ha Noi";

    @Bean
    public CommandLineRunner initDatabase(AccountRepository accountRepository) {
        return (args) -> {
            // Add Account
            createAccount(accountRepository);
        };
    }

    private void createAccount(AccountRepository accountRepository) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        Account account = accountRepository.findAccountForAdmin(MAIL_ADDRESS);
        if (account == null) {
            account = new Account();
            account.setAuthorities(Authorities.ROLE_ADMIN.name());
            account.setFullName(USER_NAME);
            String password = passwordEncoder.encode(PASSWORD);
            account.setPassword(password);
            account.setMailAddress(MAIL_ADDRESS);
            account.setAccountStatus(AccountStatus.VALID.name());
            account.setPhone(PHONE);
            account.setAddress(ADDRESS);
            account.setLogout(false);
            accountRepository.save(account);
        }
    }
}
