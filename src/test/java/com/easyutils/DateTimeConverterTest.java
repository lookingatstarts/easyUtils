package com.easyutils;

import com.easyutils.time.DateTimeConverter;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class DateTimeConverterTest {

  /**
   * 夏令时问题
   * 纽约6月实行夏令时，会在平常的基础上+1小时
   *
   * 如果时区使用UTC+XXX的形式，会忽略夏令时
   * 如果时区使用洲/城市形式，受到夏令时的影响
   */
  @Test
  public void test(){
    String format = DateTimeConverter.YEAR_MONTH_DAY_TIME_FORMATTER;
    ZoneId defaultZoneId = DateTimeConverter.DEFAULT_ZONE_ID;
    ZoneId americaNewYork = DateTimeConverter.AMERICA_NEW_YORK;
    ZoneId utcMinus5 = DateTimeConverter.GMT_MINUS_5;

    // 北京时间  2019-11-15 13:00:00 (这个时间纽约没有使用夏令时)
    LocalDateTime beijing = DateTimeConverter
        .convertToLocalDateTime(1573794000000L,defaultZoneId);
    System.out.println("北京时间："+DateTimeConverter.format(beijing,format));
    LocalDateTime newYork = DateTimeConverter
        .convertToLocalDateTime(beijing, defaultZoneId,americaNewYork);
    System.out.println("纽约时间："+DateTimeConverter.format(newYork,format));

    // 北京时间  2019-11-15 13:00:00 (这个时间纽约没有使用夏令时)
    LocalDateTime beijing4 = DateTimeConverter
        .convertToLocalDateTime(1573794000000L,defaultZoneId);
    System.out.println("北京时间："+DateTimeConverter.format(beijing4,format));
    LocalDateTime newYork4 = DateTimeConverter
        .convertToLocalDateTime(beijing4, defaultZoneId,utcMinus5);
    System.out.println("纽约时间："+DateTimeConverter.format(newYork4,format));


    // 北京时间  2019-06-15 13:00:00 (这个时间纽约使用夏令时)
    LocalDateTime beijing2 = DateTimeConverter
        .convertToLocalDateTime(1560574800000L, defaultZoneId);
    System.out.println("北京时间："+DateTimeConverter.format(beijing2,format));
    LocalDateTime newYork2 = DateTimeConverter
        .convertToLocalDateTime(beijing2, defaultZoneId,americaNewYork);
    System.out.println("纽约时间："+DateTimeConverter.format(newYork2,format));


    // 北京时间  2019-06-15 13:00:00 (这个时间纽约使用夏令时)
    LocalDateTime beijing3 = DateTimeConverter
        .convertToLocalDateTime(1560574800000L, defaultZoneId);
    System.out.println("北京时间："+DateTimeConverter.format(beijing3,format));
    LocalDateTime newYork3 = DateTimeConverter
        .convertToLocalDateTime(beijing3, defaultZoneId,utcMinus5);
    System.out.println("纽约时间："+DateTimeConverter.format(newYork3,format));
  }
}
