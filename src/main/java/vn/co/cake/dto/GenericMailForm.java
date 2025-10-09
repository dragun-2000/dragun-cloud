package vn.co.cake.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GenericMailForm {

    private String phone;
    private String password;

    private String accountName;
    private String url;

    private Long key;

    private String hash;
}
