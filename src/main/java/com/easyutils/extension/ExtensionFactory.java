package com.easyutils.extension;

/**
 * 创建扩展类对象
 */
@SPI
public interface ExtensionFactory {
    
    <T> T getExtension(Class<T> type, String name);
}
