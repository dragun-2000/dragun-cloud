package vn.co.cake.security.user;

import vn.co.cake.enums.Authorities;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * LoginInfo
 */
@Component
@Data
@EqualsAndHashCode(callSuper = true)
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class UserLoginInfo extends UserDetails implements Serializable {
    private static final long serialVersionUID = -4115373652750199107L;

    private Authorities authority;

    public UserLoginInfo(UserDetails simpleUser) {
        super(simpleUser.getId(), simpleUser.getUsername(), simpleUser.getPassword(), simpleUser.getAuthorities(), simpleUser.getEmail(), simpleUser.isFirstLogin());

        this.authority = simpleUser.getAuthorities().stream().map(grantedAuthority -> Authorities.valueOf(grantedAuthority.getAuthority())).findFirst().orElse(null);
    }
}
