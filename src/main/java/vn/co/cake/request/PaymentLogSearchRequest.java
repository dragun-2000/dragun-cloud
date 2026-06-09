package vn.co.cake.request;

import java.io.Serializable;
import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;
import vn.co.cake.common.DateConst;

@Data
public class PaymentLogSearchRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderCode;
    private String vietqrOrderId;
    private String paymentMethod;
    private String status;
    private String eventType;

    @DateTimeFormat(pattern = DateConst.YYYY_MM_DD)
    private Date lastUpdateDateFrom;

    @DateTimeFormat(pattern = DateConst.YYYY_MM_DD)
    private Date lastUpdateDateTo;
}
