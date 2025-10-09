package vn.co.cake.response;

import lombok.Data;

@Data
public class ResetPasswordModel {
    private String email;
    private String emailRepeat;
}
