package vn.co.cake.controller.external.dto;

import lombok.Data;

@Data
public class SendMessageDTO {
    private String phone;
    private String message;
}
