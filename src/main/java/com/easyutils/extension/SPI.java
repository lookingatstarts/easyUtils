package com.easyutils.extension;

import java.lang.annotation.*;

/**
 * 标识类是spi接口的实现类
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SPI {

    /**
     * 实现类的默认名字
     */
    String value() default "";
}
