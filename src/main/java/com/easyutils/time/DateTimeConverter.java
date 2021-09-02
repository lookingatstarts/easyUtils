package com.easyutils.time;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * 时区转换工具
 * 1、LocalDateTime = LocalDate +  LocalTime  仅仅表示时间
 * eg:2021-08-10 80:22:10 可以认为它是北京时间，也可以认为它是伦敦时间，没有明确的时区
 * 2、时间戳与时间的关系
 * 时间戳没有时区概念，时间是有时区概念
 * 时间戳是一个不断递增的整数，是距离格林威治时间过去的秒数
 * 时间与时间戳是N:1关系
 * 时间可以认为是同一个时间戳不同显示 eg:1630590333 北京时间：2021-09-02 21:45:33 伦敦时间：2021-09-02 14:45:33
 * 3、ZonedDateTime = LocalDateTime + ZoneId 时间+时区 才能转换成时间戳
 * 4、时区
 * 光靠一个时间无法确定一个时刻(时间戳)，需要给当地时间加上一个时区
 * 5、java.util.Date实际上存储了一个long类型毫秒表示的时间戳
 * getYear获取的年份要加上1900
 * getMonth获取的月份要加上1
 * getDate获取天数不要加1
 * 使用SimpleDateFormat格式化时间
 * 5、在数据库中存储时间戳时，尽量使用long型时间戳，它具有省空间，效率高，不依赖数据库的优点
 */
public class DateTimeConverter {

    public static final String YEAR_MONTH_DAY_TIME_FORMATTER = "yyyy-MM-dd HH:mm:ss";
    public static final String YEAR_MONTH_DAY_FORMATTER = "yyyy-MM-dd";
    public static final ZoneId DEFAULT_ZONE_ID = ZoneId.ofOffset("UTC", ZoneOffset.ofHours(8));
    public static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");

    /**
     * 已P....T...形式表示P...T之间表示日期间隔，T后面表示时间间隔，如果是PT...格式仅有时间间隔
     */
    public static Duration duration(LocalDateTime start, LocalDateTime end) {
        return Duration.between(start, end);
    }

    /**
     * 已P....T...形式表示P...T之间表示日期间隔，T后面表示时间间隔，如果是PT...格式仅有时间间隔
     */
    public static Period period(LocalDate start, LocalDate end) {
        return Period.between(start, end);
    }

    public static LocalDateTime parseToLocalDateTime(String time, String formatPattern) {
        return LocalDateTime.parse(time, DateTimeFormatter.ofPattern(formatPattern));
    }


    public static LocalDate parseToLocalDate(String time, String formatPattern) {
        return LocalDate.parse(time, DateTimeFormatter.ofPattern(formatPattern));
    }

    /**
     * 时区：北京时间
     * 当前时间
     */
    public static ZonedDateTime now(ZoneId zoneId) {
        return ZonedDateTime.now(zoneId);
    }

    public static ZonedDateTime withZone(LocalDateTime localDateTime, ZoneId zoneId) {
        return localDateTime.atZone(zoneId);
    }

    /**
     * 时间在不同时区转换
     */
    public static ZonedDateTime convert(LocalDateTime localDateTime, ZoneId from, ZoneId to) {
        return withZone(localDateTime, from)
                .withZoneSameInstant(to);
    }

    public static ZonedDateTime convert(Calendar calendar, ZoneId to) {
        return calendar.toInstant()
                .atZone(calendar.getTimeZone().toZoneId())
                .withZoneSameInstant(to);
    }

    public static Calendar convert(ZonedDateTime zonedDateTime) {
        long milli = zonedDateTime.toInstant().toEpochMilli();
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.setTimeZone(TimeZone.getTimeZone(zonedDateTime.getZone().getId()));
        calendar.setTimeInMillis(milli);
        return calendar;
    }

    /**
     * 时间在不同时区转换
     */
    public static ZonedDateTime convert(Date date, ZoneId to) {
        return ZonedDateTime.ofInstant(date.toInstant(), to);
    }

    /**
     * 时间在不同时区转换
     */
    public static LocalDateTime convertToLocalDateTime(LocalDateTime localDateTime, ZoneId from, ZoneId to) {
        // toLocalDateTime会丢失时区信息
        return convert(localDateTime, from, to).toLocalDateTime();
    }

    /**
     * 时间戳转日期
     */
    public static LocalDateTime convertToLocalDateTime(long nowMills, ZoneId zoneId) {
        return Instant.ofEpochMilli(nowMills).atZone(zoneId).toLocalDateTime();
    }

    /**
     * 日期转时间戳(毫秒)
     */
    public static long convertToMilli(LocalDateTime localDateTime, ZoneId zoneId) {
        return localDateTime.atZone(zoneId).toInstant().toEpochMilli();
    }

    /**
     * 日期转时间戳(毫秒)
     */
    public static long convertToSecond(LocalDateTime localDateTime, ZoneId zoneId) {
        return convertToMilli(localDateTime, zoneId) / 1000;
    }

    /**
     * 时间比较
     */
    public static boolean greaterThan(LocalDateTime one, LocalDateTime two) {
        return one.isAfter(two);
    }

    public static boolean lessThan(LocalDateTime one, LocalDateTime two) {
        return one.isBefore(two);
    }
}
