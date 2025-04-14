
package com.ecommerce.kafkahighconcurrencyproject.util;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

@Component
@Log4j2
public class DateUtil {

    public enum Format {
        YYYY_MM_DD("yyyy-MM-dd"), // 2018-05-23
        YYYY_MM_DD_HH_MM_SS_SSS("yyyy-MM-dd HH:mm:ss.SSS"), // 2018-05-25 12:22:00.674
        YYYY_MM_DD_HH_MM_SS("yyyy-MM-dd HH:mm:ss"), // 2018-05-23 12:23:23
        DD_MMM_YYYY_HH_MM("dd-MMM-yyyy HH:mm"), // 23-MAY-2018 13:12
        YYYYMMDDHHMMSS("yyyyMMddHHmmss"), // 20180523134012
        DD_MM_YYYY_HH_MM_SS("dd-MM-yyyy HH:mm:ss"), // 23-05-2018 13:00:00
        HH_MM_SS("HH:mm:ss"), // 09:19:59
        DD_MMM_YYYY("dd MMM yyyy");

        private final String text;

        Format(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /**
     * Get the Calendar instance object
     *
     * @return
     */
    public Calendar calendar() {
        return Calendar.getInstance();
    }

    /**
     * Get Current LocalDateTime
     *
     * @return
     */
    public LocalDateTime getCurrentTimestamp() {
        return LocalDateTime.now();
    }

    /**
     * Get SimpleDateFormat Instance
     *
     * @param format
     * @return
     */
    public SimpleDateFormat sdf(Format format) {
        return new SimpleDateFormat(format.toString());
    }

    /**
     * Get Current Date in a particular format
     *
     * @param format
     * @return
     */
    public String getDate(Format format) {
        try {
            return sdf(format).format(calendar().getTime());
        } catch (Exception e) {
            return null;
        }
    }

    public String getFormattedDate(Date date, Format format) {
        SimpleDateFormat sdf = sdf(format);
        try {
            return date == null ? sdf.format(calendar().getTime()) : sdf.format(date);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get Current Date in format yyyy-MM-dd
     *
     * @return
     */
    public String getDate() {
        return getDate(Format.YYYY_MM_DD);
    }

    /**
     * Parse String Date with given format
     *
     * @param date   String Date Time
     * @param format Formatter to be used to convert
     * @return
     * @throws ParseException
     */
    public Date getDate(String date, Format format) throws ParseException {
        return sdf(format).parse(date);
    }

    /**
     * Parse the string date with time zone and format
     *
     * @param date     Parse given date string
     * @param format   Format of String
     * @param timeZone Pass time zone like IST,GMT+5 or GMT-5.
     * @return
     */
    public Date getDate(String date, Format format, String timeZone) {
        SimpleDateFormat sdf = sdf(format);
        sdf.setTimeZone(TimeZone.getTimeZone(timeZone));
        try {
            return sdf.parse(date);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Get current date in format dd MMM yyyy for the given time zone
     *
     * @param timeZone
     * @return
     */
    public String getDate(String timeZone) {
        return getDate(Format.DD_MMM_YYYY, timeZone);
    }

    /**
     * Get current date in specified format for the given time zone
     *
     * @param format
     * @param timeZone
     * @return
     */
    public String getDate(Format format, String timeZone) {
        TimeZone tz = TimeZone.getTimeZone(timeZone);
        SimpleDateFormat sdf1 = sdf(format);
        sdf1.setTimeZone(tz);
        return sdf1.format(calendar().getTime());
    }

    /**
     * Get current date time in format yyyy-MM-dd HH:mm:ss.SSS
     *
     * @return
     */
    public String getDateTime() {
        return getDate(Format.YYYY_MM_DD_HH_MM_SS_SSS);
    }

    /**
     * Get current sql timestamp get time format with paramter time zone.
     *
     * @return return SQL Timestamp
     */
    public Timestamp getCurrentSqlTimestamp() {
        return Timestamp.valueOf(sdf(Format.YYYY_MM_DD_HH_MM_SS).format(calendar().getTime()));
    }

    /**
     * Add hours, minutes in the given time and result as string format of
     * "HH:mm:ss".
     *
     * @param hours         Hour in integer
     * @param minutes       Minute in integer
     * @param preferredTime String datetime in format (HH:mm:ss);
     * @return result will return with time by adding hours and minutes.
     */
    public String addHoursInGivenTimeDateFormat(int hours, int minutes, String preferredTime) {
        Calendar cal = calendar();
        try {
            SimpleDateFormat sdf = sdf(Format.HH_MM_SS);
            cal.setTime(sdf.parse(preferredTime));
            cal.add(Calendar.HOUR, hours);
            cal.add(Calendar.MINUTE, minutes);
            return sdf.format(cal.getTime());
        } catch (Exception e) {
            log.error("Exception addHoursInGivenTimeDateFormat {}", e);
            return null;
        }
    }

    /**
     * Reformat date string from one format to another
     *
     * @param date
     * @param fromFormat
     * @param toFormat
     * @return Date String in Desired format
     */
    public String reformatDate(String date, Format fromFormat, Format toFormat) {
        Calendar cal = calendar();
        try {
            cal.setTime(sdf(fromFormat).parse(date));
            return sdf(toFormat).format(cal.getTime());
        } catch (Exception e) {
            log.error("Exception reformatDate {}", e);
            return null;
        }
    }

}
