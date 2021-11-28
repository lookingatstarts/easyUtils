package com.easyutils.time;

import java.time.ZoneId;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * 时区
 * 推荐：Etc/GMT+xxx Etc/GMT-xxx 这种形式的时区表示方式
 */
public enum Zones {
    ;
    /**
     * 上海
     */
    public static final ZoneId ASIA_SHANGHAI = ZoneId.of("Asia/Shanghai");
    /**
     * 纽约
     */
    public static final ZoneId AMERICA_NEW_YORK = ZoneId.of("America/New_York");
    /**
     * Etc/GMT-8 等同于 GMT+8
     * 东八区
     * 由于TimeZone不识别UTC+08:00这样的格式，TimeZone.getTimeZone(ZoneId.of(UTC+08:00)) 转换不了，也不报错，直接使用GMT+0时区(格林威治时间)
     */
    public static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Etc/GMT-8");
    public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("Etc/GMT-8");
    /**
     * Etc/GMT+5 等同于 GMT-5
     * 西5区
     */
    public static final ZoneId GMT_MINUS_5 = ZoneId.of("Etc/GMT+5");
    public static final TimeZone GMT_MINUS_5_TIME_ZONE = TimeZone.getTimeZone("Etc/GMT+5");
    /**
     * Etc/GMT+0 等同于 Etc/GMT 等同于 Etc/GMT0 等同于 GMT 等同于 GMT0
     * 格林威治标准时区
     */
    public static final ZoneId GMT_ZONE_ID = ZoneId.of("Etc/GMT+0");
    public static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone("Etc/GMT+0");

    public static Set<String> availableGmtZoneId() {
        return ZoneId.getAvailableZoneIds().stream()
                .filter(id -> id.contains("GMT"))
                .collect(Collectors.toSet());
    }

    public static Set<String> availableZoneId() {
        return ZoneId.getAvailableZoneIds();
    }
}
