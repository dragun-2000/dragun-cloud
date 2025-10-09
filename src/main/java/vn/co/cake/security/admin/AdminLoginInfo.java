package vn.co.cake.security.admin;

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
public class AdminLoginInfo extends AdminDetails implements Serializable {

    private Authorities authority;

    public AdminLoginInfo(AdminDetails simpleUser) {
        super(simpleUser.getId(), simpleUser.getUsername(), simpleUser.getPassword(), simpleUser.getAuthorities(), simpleUser.getEmail());

        this.authority = simpleUser.getAuthorities().stream().map(grantedAuthority -> Authorities.valueOf(grantedAuthority.getAuthority())).findFirst().orElse(null);
    }
}
