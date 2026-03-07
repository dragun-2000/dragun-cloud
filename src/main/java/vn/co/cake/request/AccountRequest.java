package vn.co.cake.request;

import lombok.Data;

@Data
public class AccountRequest {

    private String accountId;

    private String fullName;

    private String mailAddress;

    private String password;

    private String confirmPassword;
    
    private String phone;

    private Integer height;
    private Integer weight;

    private String province;
    private String district;
    private String ward;
    private String address;
}
