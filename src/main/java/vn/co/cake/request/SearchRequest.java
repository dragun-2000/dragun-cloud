package vn.co.cake.request;

import vn.co.cake.common.DateConst;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
public class SearchRequest implements Serializable {
    private static final long serialVersionUID = -4115973602750499107L;
    public String id;
    public String code;
    private String email;
    private String status;
    
    @DateTimeFormat(pattern = DateConst.YYYY_MM_DD)
    private Date lastUpdateDateFrom;
    @DateTimeFormat(pattern = DateConst.YYYY_MM_DD)
    private Date lastUpdateDateTo;
}
