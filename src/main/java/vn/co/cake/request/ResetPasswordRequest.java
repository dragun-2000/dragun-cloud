package vn.co.cake.request;

import lombok.Data;

@Data
public class ResetPasswordRequest {

    private Long accountId;

    private String hash;

    private String newPassword;

    private String confirmPassword;
}
