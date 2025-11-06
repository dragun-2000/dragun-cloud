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
        try {
            // Check username
            if (StringUtils.isAllBlank(username)) {
                throw new BadCredentialsException(BaseConst.ERROR);
            }

            // Search the account by username (can be phone or email)
            Account account = accountRepository.findAccountForMember(username);
            
            // If not found by phone, try by email
            if (account == null) {
                account = accountRepository.findAccountForgetPassword(username);
            }

            if (account == null || account.isDeleted()) {
                throw new UsernameNotFoundException(BaseConst.NOT_FOUND);
            }

            // Validate required fields
            if (account.getId() == null) {
                throw new BadCredentialsException("Account ID is null");
            }

            String mailAddress = account.getMailAddress();
            if (StringUtils.isBlank(mailAddress)) {
                throw new BadCredentialsException("Email address is required");
            }

            String password = account.getPassword();
            if (StringUtils.isBlank(password)) {
                throw new BadCredentialsException("Password is required");
            }

            String authorities = account.getAuthorities();
            if (StringUtils.isBlank(authorities)) {
                throw new BadCredentialsException("Account authorities is required");
            }

            return new UserDetails(account.getId(), mailAddress, password,
                    Collections.singletonList(new SimpleGrantedAuthority(authorities)), mailAddress, account.isFirstLogin());
        } catch (BadCredentialsException | UsernameNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BadCredentialsException("Error loading user: " + e.getMessage(), e);
        }
    }
}
