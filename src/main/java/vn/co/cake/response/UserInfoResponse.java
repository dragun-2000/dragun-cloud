package vn.co.cake.response;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserInfoResponse implements Serializable {
    private String displayName;
    private String userPrincipalName;
    private String email;
    private String role;
}
