package vn.co.cake.utils;

import vn.co.cake.common.DateConst;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import static vn.co.cake.common.DateConst.YYYY_MM_DD_HH_MM_SS;
import static vn.co.cake.common.DateConst.YYYY_MM_DD_T_HH_MM_SS;

/**
 * DateUtil
 */
@Slf4j
public class DateUtil {

    private static final String INVALID_DATE = "Invalid date: {}, detail: {}";
    private static final String CALENDER_APP_TIME_ZONE = "CalenderAppTimeZone";

    public static String dateToString(Date date, String format) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }

    public static String dateToStringJP(Date date, String format) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format, new Locale("ja"));
        return sdf.format(date);
    }

    public static String stringToStringFormat(String value, String format) {
        if (StringUtils.isEmpty(value)) return StringUtils.EMPTY;

        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(format);
        LocalDateTime dateTime = LocalDateTime.parse(value, inputFormatter);
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS);
        return dateTime.format(outputFormatter);
    }

    public static String csvDateFormatJP(Date date, String format) {
        DateFormat formatterJP = new SimpleDateFormat(format);
        formatterJP.setTimeZone(TimeZone.getTimeZone(DateConst.JST));
        return formatterJP.format(date);
    }

    public static Date stringToDate(String date, String format) {
        if (date == null) {
            return null;
        }
        try {
            DateFormat dateFormat = new SimpleDateFormat(format);
            return dateFormat.parse(date);
        } catch (Exception e) {
            log.error("Parser string (date: {}, format: {}) to date fail: {}", date, format, e.toString());
        }
        return null;
    }

    public static String timestampToString(Timestamp timestamp, String format) {
        if (timestamp == null) {
            return null;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        return dateFormat.format(timestamp);
    }

    public static int calculateAge(Date birthOfDate) {
        if (Objects.nonNull(birthOfDate)) {
            return Period
                    .between(
                            birthOfDate
                                    .toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate(),
                            LocalDate.now())
                    .getYears();
        } else {
            return 0;
        }
    }

    public static Date now() {
        return new Date();
    }

    /**
     * Convert to UTC time
     *
     * @param source     {@link Date}
     * @param timeZoneId {@link String}
     * @return {@link Date}
     */
    public static Date convertToUTC(Date source, String timeZoneId) {
        if (source != null) {
            // convert date to Time zone of site
            DateFormat formatterIST = new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS);
            formatterIST.setTimeZone(TimeZone.getTimeZone(timeZoneId));
            try {
                Date date = formatterIST.parse(dateToString(source, YYYY_MM_DD_HH_MM_SS));

                DateFormat formatterUTC = new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS);
                return stringToDate(formatterUTC.format(date), YYYY_MM_DD_HH_MM_SS);
            } catch (ParseException e) {
                log.error(INVALID_DATE, source.toString(), e);
            }
        }
        return null;
    }

    public static Date convertToJP(Date source) {
        if (source != null) {
            DateFormat formatterJP = new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS);
            formatterJP.setTimeZone(TimeZone.getTimeZone(DateConst.JST));
            return stringToDate(formatterJP.format(source), YYYY_MM_DD_HH_MM_SS);
        }
        return null;
    }

    public static Date convertToVN(Date source) {
        if (source != null) {
            DateFormat formatterJP = new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS);
            formatterJP.setTimeZone(TimeZone.getTimeZone(DateConst.VN));
            return stringToDate(formatterJP.format(source), YYYY_MM_DD_HH_MM_SS);
        }
        return null;
    }

    public static Date formatDate(Date source, String format) {
        if (source != null) {
            DateFormat formatter = new SimpleDateFormat(format);
            return stringToDate(formatter.format(source), format);
        }
        return null;
    }

    /**
     * Convert to site time zone
     *
     * @param utcSource  {@link Date}
     * @param timeZoneId {@link String}
     * @return {@link Date}
     */
    public static Date convertToSiteTimezone(Date utcSource, String timeZoneId) {
        if (utcSource != null) {
            DateFormat formatterUTC = new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS);
            formatterUTC.setTimeZone(TimeZone.getTimeZone(DateConst.UTC));
            try {
                Date date = formatterUTC.parse(dateToString(utcSource, YYYY_MM_DD_HH_MM_SS));

                DateFormat formatterIST = new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS);
                formatterIST.setTimeZone(TimeZone.getTimeZone(timeZoneId));
                return stringToDate(formatterIST.format(date), YYYY_MM_DD_HH_MM_SS);
            } catch (ParseException e) {
                log.error(INVALID_DATE, utcSource.toString(), e);
            }
        }
        return null;
    }

    /**
     * Convert Date to String of site time zone
     *
     * @param utcSource  {@link Date}
     * @param timeZoneId {@link String}
     * @param format     {@link String}
     * @return {@link Date}
     */
    public static String convertDateToStringSiteTimezone(Date utcSource, String timeZoneId, String format) {
        if (utcSource != null) {
            DateFormat formatterUTC = new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS);
            formatterUTC.setTimeZone(TimeZone.getTimeZone(DateConst.UTC));
            try {
                Date date = formatterUTC.parse(dateToString(utcSource, YYYY_MM_DD_HH_MM_SS));

                DateFormat formatterIST = new SimpleDateFormat(format, new Locale("ja"));
                formatterIST.setTimeZone(TimeZone.getTimeZone(timeZoneId));
                return formatterIST.format(date);
            } catch (ParseException e) {
                log.error(INVALID_DATE, utcSource.toString(), e);
            }
        }
        return null;
    }

    /**
     * Convert to UTC time
     *
     * @param source     {@link String}
     * @param timeZoneId {@link String}
     * @param format     {@link String}
     * @return {@link Date}
     */
    public static Date convertStringToDateUTC(String source, String timeZoneId, String format) {
        if (source != null) {
            // convert date to Time zone of site
            DateFormat formatterIST = new SimpleDateFormat(format);
            formatterIST.setTimeZone(TimeZone.getTimeZone(timeZoneId));
            try {
                Date date = formatterIST.parse(source);

                DateFormat formatterUTC = new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS);
                formatterUTC.setTimeZone(TimeZone.getTimeZone(DateConst.UTC));
                return stringToDate(formatterUTC.format(date), YYYY_MM_DD_HH_MM_SS);
            } catch (ParseException e) {
                log.error(INVALID_DATE, source, e);
            }
        }
        return null;
    }

    /**
     * Get timezone from request
     *
     * @param request {@link HttpServletRequest}
     * @return {@link String}
     */
    public static String getTimezoneRequest(HttpServletRequest request) {
        String timeZoneId = ZoneId.of(DateConst.JST).getId();

        Cookie[] cookieArray = request.getCookies();
        if (cookieArray != null) {
            for (Cookie cookie : cookieArray) {
                if (CALENDER_APP_TIME_ZONE.equals(cookie.getName())) {
                    timeZoneId = cookie.getValue();
                }
            }
        }

        return timeZoneId;
    }

    public static Date minusDays(Date source, int days) {
        return DateUtils.addDays(source, -days);
    }

    public static Date addDays(Date source, int days) {
        return DateUtils.addDays(source, days);
    }

    public static Date plusHours(Date date, int hours) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, hours);
        return calendar.getTime();
    }

    public static Integer getHours(Date date) {
        Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
        calendar.setTime(date);   // assigns calendar to given date
        return calendar.get(Calendar.HOUR_OF_DAY);
    }
}
