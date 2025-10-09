package vn.co.cake.security.service;

import vn.co.cake.common.BaseConst;
import vn.co.cake.entity.Account;
import vn.co.cake.repository.AccountRepository;
import vn.co.cake.security.user.UserDetails;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * UserDetailsServiceImpl
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AccountRepository accountRepository;

    public UserDetailsServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Check username
        if (StringUtils.isAllBlank(username)) {
            throw new BadCredentialsException(BaseConst.ERROR);
        }

        // Search the account by username
        Account account = accountRepository.findAccountForMember(username);

        if (account == null || account.isDeleted()) {
            throw new UsernameNotFoundException(BaseConst.NOT_FOUND);
        }

        return new UserDetails(account.getId(), account.getMailAddress(), account.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(account.getAuthorities())), account.getMailAddress(), account.isFirstLogin());
    }
}
