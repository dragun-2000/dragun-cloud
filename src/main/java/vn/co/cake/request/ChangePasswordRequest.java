package vn.co.cake.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChangePasswordRequest implements Serializable {
    private String accountId;

    private String oldPassword;

    private String newPassword;

    private String confirmPassword;
}
