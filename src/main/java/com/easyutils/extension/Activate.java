package com.easyutils.extension;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Activate {
    String[] group() default {};

    String[] value() default {};

    String[] before() default {};

    String[] after() default {};

    int order() default 0;
}
