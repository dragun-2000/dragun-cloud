package vn.co.cake.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.TimeZone;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Characterization tests for {@link DateUtil}.
 * Many methods format/parse with the JVM default time zone, so the default zone is set to UTC in
 * {@link #setUp()} and restored in {@link #tearDown()}. Methods relying on DateConst.UTC (which is
 * captured from the default zone at class-load time) are intentionally not tested here.
 */
class DateUtilTest {

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final Date FIXED_DATE = Date.from(Instant.parse("2026-01-15T10:20:30.456Z"));
    private static final long ONE_HOUR_MILLIS = 3_600_000L;

    private TimeZone originalTimeZone;

    @BeforeEach
    void setUp() {
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    void tearDown() {
        TimeZone.setDefault(originalTimeZone);
    }

    private static Date utc(String isoInstant) {
        return Date.from(Instant.parse(isoInstant));
    }

    @Test
    void dateToStringAndTimestampToStringFormatInDefaultZone() {
        assertNull(DateUtil.dateToString(null, DATE_TIME_FORMAT));
        assertEquals("2026-01-15 10:20:30", DateUtil.dateToString(FIXED_DATE, DATE_TIME_FORMAT));
        assertNull(DateUtil.timestampToString(null, DATE_TIME_FORMAT));
        assertEquals("2026/01/15", DateUtil.timestampToString(new Timestamp(FIXED_DATE.getTime()), "yyyy/MM/dd"));
        assertNull(DateUtil.dateToStringJP(null, "EEE"));
        assertEquals("木", DateUtil.dateToStringJP(FIXED_DATE, "EEE"));
    }

    @Test
    void stringToStringFormatReformatsToStandardPattern() {
        assertEquals("", DateUtil.stringToStringFormat(null, "yyyy/MM/dd HH:mm"));
        assertEquals("", DateUtil.stringToStringFormat("", "yyyy/MM/dd HH:mm"));
        assertEquals("2026-01-02 03:04:00", DateUtil.stringToStringFormat("2026/01/02 03:04", "yyyy/MM/dd HH:mm"));
        assertThrows(DateTimeParseException.class, () -> DateUtil.stringToStringFormat("2026/01/02", "yyyy/MM/dd"));
    }

    @Test
    void stringToDateIsLenientAndReturnsNullOnFailure() {
        assertNull(DateUtil.stringToDate(null, "yyyy-MM-dd"));
        assertNull(DateUtil.stringToDate("not a date", "yyyy-MM-dd"));
        assertEquals(utc("2026-01-15T10:20:30Z"), DateUtil.stringToDate("2026-01-15 10:20:30", DATE_TIME_FORMAT));
        // current behavior: SimpleDateFormat is lenient, so month 13 / day 45 roll over
        assertEquals(utc("2027-02-14T00:00:00Z"), DateUtil.stringToDate("2026-13-45", "yyyy-MM-dd"));
        // current behavior: trailing text after a valid prefix is ignored
        assertEquals(utc("2026-01-15T00:00:00Z"), DateUtil.stringToDate("2026-01-15xyz", "yyyy-MM-dd"));
    }

    @Test
    void csvDateFormatJpAlwaysUsesTokyoZone() {
        assertEquals("2026-01-15 19:20:30", DateUtil.csvDateFormatJP(FIXED_DATE, DATE_TIME_FORMAT));
        assertThrows(NullPointerException.class, () -> DateUtil.csvDateFormatJP(null, DATE_TIME_FORMAT));
    }

    @Test
    void convertToJpAndVnShiftInstantByZoneOffsetAndDropMillis() {
        assertNull(DateUtil.convertToJP(null));
        assertNull(DateUtil.convertToVN(null));
        // current behavior: wall-clock time in JST/ICT is re-read in the default zone -> shifted instant
        assertEquals(utc("2026-01-15T19:20:30Z"), DateUtil.convertToJP(FIXED_DATE));
        assertEquals(utc("2026-01-15T17:20:30Z"), DateUtil.convertToVN(FIXED_DATE));
    }

    @Test
    void convertToUtcShiftsBackByZoneOffset() {
        assertNull(DateUtil.convertToUTC(null, "Asia/Tokyo"));
        // current behavior: source wall-clock is interpreted in the given zone, millis are dropped
        assertEquals(utc("2026-01-15T01:20:30Z"), DateUtil.convertToUTC(FIXED_DATE, "Asia/Tokyo"));
    }

    @Test
    void formatDateTruncatesToFormatPrecision() {
        assertNull(DateUtil.formatDate(null, "yyyy-MM-dd"));
        assertEquals(utc("2026-01-15T00:00:00Z"), DateUtil.formatDate(FIXED_DATE, "yyyy-MM-dd"));
    }

    @Test
    void dayAndHourArithmetic() {
        assertEquals(utc("2026-01-18T10:20:30.456Z"), DateUtil.addDays(FIXED_DATE, 3));
        assertEquals(utc("2026-01-12T10:20:30.456Z"), DateUtil.minusDays(FIXED_DATE, 3));
        assertEquals(new Date(FIXED_DATE.getTime() + 5 * ONE_HOUR_MILLIS), DateUtil.plusHours(FIXED_DATE, 5));
        assertEquals(Integer.valueOf(10), DateUtil.getHours(FIXED_DATE));
        // current behavior: commons-lang3 DateUtils rejects a null date with NullPointerException
        assertThrows(NullPointerException.class, () -> DateUtil.addDays(null, 1));
        assertEquals(0, DateUtil.calculateAge(null));
    }

    @Test
    void getTimezoneRequestDefaultsToTokyoAndUsesLastMatchingCookie() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        assertEquals("Asia/Tokyo", DateUtil.getTimezoneRequest(request));

        when(request.getCookies()).thenReturn(new Cookie[] {
            new Cookie("other", "x"),
            new Cookie("CalenderAppTimeZone", "Asia/Ho_Chi_Minh"),
            new Cookie("CalenderAppTimeZone", "Not/AZone")
        });
        // current behavior: the last matching cookie wins and its value is not validated
        assertEquals("Not/AZone", DateUtil.getTimezoneRequest(request));
    }
}
