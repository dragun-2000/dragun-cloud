package vn.co.cake.security.admin;

import vn.co.cake.common.BaseConst;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * AuthenticationProvider
 */
@Slf4j
public class AdminAuthenticationProvider extends DaoAuthenticationProvider {

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) {
        Object credentials = authentication.getCredentials();
        
        if (credentials == null) {
            log.error("Admin account {} authentication failed: credentials are null", userDetails.getUsername());
            throw new BadCredentialsException(BaseConst.ERROR);
        }

        String password = credentials.toString();

        if (password == null || password.isEmpty()) {
            log.error("Admin account {} authentication failed: password is empty", userDetails.getUsername());
            throw new BadCredentialsException(BaseConst.ERROR);
        }

        // Use the configured password encoder from parent class
        PasswordEncoder passwordEncoder = getPasswordEncoder();
        if (passwordEncoder == null) {
            log.error("Admin account {} authentication failed: password encoder is not configured", userDetails.getUsername());
            throw new BadCredentialsException(BaseConst.ERROR);
        }

        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            log.error("Admin account {} password incorrect", userDetails.getUsername());
            throw new BadCredentialsException(BaseConst.ERROR);
        }

        // Call parent to perform additional checks (account status, etc.)
        super.additionalAuthenticationChecks(userDetails, authentication);
    }
}
