package vn.co.cake.entity.external;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Embeddable;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CustomerInfoExtend {

    private String name;
    private String phone;
    private String address;
}
