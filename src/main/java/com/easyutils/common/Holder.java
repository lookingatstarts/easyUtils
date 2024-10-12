package com.easyutils.common;

import lombok.Data;

@Data
public class Holder<T> {
    private volatile T value;
}
