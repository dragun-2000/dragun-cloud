package vn.co.cake.security.service;

import vn.co.cake.common.BaseConst;
import vn.co.cake.entity.Account;
import vn.co.cake.repository.AccountRepository;
import vn.co.cake.security.admin.AdminDetails;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * UserDetailsServiceImpl
 */
@Service
public class AdminDetailsServiceImpl implements UserDetailsService {

    private final AccountRepository accountRepository;

    public AdminDetailsServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Check username
        if (StringUtils.isAllBlank(username)) {
            throw new BadCredentialsException(BaseConst.ERROR);
        }

        // Search the account by username
        Account account = accountRepository.findAccountForAdmin(username);

        if (account == null || account.isDeleted()) {
            throw new UsernameNotFoundException(BaseConst.NOT_FOUND);
        }

        return new AdminDetails(account.getId(), account.getMailAddress(), account.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(account.getAuthorities())), account.getMailAddress());
    }
}
