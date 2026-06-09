package vn.co.cake.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PasswordResetMailModel {

    private String accountName;
    private String phone;
    private String resetUrl;
    private String storeUrl;
    private String storeDisplayUrl;
}
