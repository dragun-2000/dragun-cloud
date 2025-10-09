package vn.co.cake.common;

import java.util.Calendar;

/**
 * DateConst
 */
public class DateConst {

    // yyyy pattern date
    public static final String YYYY = "yyyy";
    // MM pattern date
    public static final String MM = "MM";
    // dd pattern date
    public static final String DD = "dd";
    // yyyy-MM-dd pattern date
    public static final String YYYY_MM_DD = "yyyy-MM-dd";
    // yyyy/MM/dd pattern date
    public static final String YYYY_MM_DD_SLASH = "yyyy/MM/dd";
    // yyyyMMdd pattern date
    public static final String YYYYMMDD = "yyyyMMdd";
    // yyyy-MM-dd HH:mm:ss pattern date
    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    public static final String YYYY_MM_DD_T_HH_MM_SS = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS";

    public static final String YYYY_MM_DD_HH_MM = "yyyy-MM-dd HH:mm";
    // yyyy/MM/dd HH:mm:ss pattern date
    public static final String YYYY_MM_DD_HH_MM_SS_SLASH = "yyyy/MM/dd HH:mm:ss";
    // ddMMyy pattern date
    public static final String DDMMYY = "ddMMyy";
    // yyMMdd pattern date
    public static final String YYMMDD = "yyMMdd";
    // yyyyMMddHHmmss pattern date
    public static final String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";
    // HH:mm pattern date
    public static final String HH_MM = "HH:mm";
    // Timezone UTC
    public static final String UTC = Calendar.getInstance().getTimeZone().getID();
    // Timezone JST
    public static final String JST = "Asia/Tokyo";
    public static final String VN = "Asia/Ho_Chi_Minh";

    private DateConst() {
        throw new IllegalStateException();
    }
}
