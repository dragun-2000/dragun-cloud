package vn.co.cake.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommonResponse {
    private String result;
    private String message;
}
