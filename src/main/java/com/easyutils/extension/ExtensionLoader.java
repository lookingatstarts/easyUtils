package com.easyutils.extension;

import com.easyutils.common.Holder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

@Slf4j
public class ExtensionLoader<T> {
    /**
     * 通过,分隔多个名字
     */
    private static final Pattern NAME_SEPARATOR = Pattern.compile("\\s*[,]+\\s*");
    /**
     * Map<spi接口，其ExtensionLoader>
     * <p>
     * 一个spi接口都有一个自己的ExtensionLoader，ExtensionLoader会缓存加载解析的数据
     */
    private static final ConcurrentMap<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();
    /**
     * spi扩展类接口类型
     */
    private final Class<?> type;
    private final String[] loadDirectories;
    /**
     * 缓存异常信息
     * map.key = 配置文件的每一行
     */
    private final Map<String, IllegalStateException> exceptions = new ConcurrentHashMap<>();
    private volatile Throwable createAdaptiveInstanceError;
    /**
     * 默认扩展类名字(只能有一个默认默认扩展类)
     */
    private String cachedDefaultName;
    /**
     * 普通扩展类，所有名字对应的实现类
     */
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();
    /**
     * Map<普通扩展类，第一个名字>
     */
    private final ConcurrentMap<Class<?>, String> cachedNames = new ConcurrentHashMap<>();

    /**
     * 自适应扩展类，最大一个
     */
    private volatile Class<?> cachedAdaptiveClass = null;
    /**
     * wrapper类
     */
    private Map<Class<?>, Boolean> cachedWrapperClasses = null;
    /**
     * Map<扩展类的第一个名字,Activate注解> 激活条件
     */
    private final Map<String, Object> cachedActivates = new ConcurrentHashMap<>();
    /**
     * ExtensionFactory
     *
     * objectFactory的ExtensionLoader的objectFactory为null，相当于bean工厂(ExtensionFactory没有ExtensionFactory)
     */
    private final ExtensionFactory objectFactory;

    /**
     * adaptive扩展类实例只有一个
     */
    private final Holder<Object> cachedAdaptiveInstance = new Holder<>();

    // 最先创建ExtensionFactory的ExtensionLoader,然后获取ExtensionFactory的adaptiveExtension实例
    private ExtensionLoader(Class<?> type) {
        this.type = type;
        // 扩展类工厂
        objectFactory = (type == ExtensionFactory.class ? null : ExtensionLoader.getExtensionLoader(ExtensionFactory.class).getAdaptiveExtension());
    }


    @SuppressWarnings("unchecked")
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Extension type == null");
        }
        // 必须是接口
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type (" + type + ") is not an interface!");
        }
        // 必须有SPI注解
        if (!withExtensionAnnotation(type)) {
            throw new IllegalArgumentException("Extension type (" + type +
                    ") is not an extension, because it is NOT annotated with @" + SPI.class.getSimpleName() + "!");
        }
        ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        if (loader == null) {
            EXTENSION_LOADERS.putIfAbsent(type,new ExtensionLoader<>())
        }
        return loader;

    }

    @SuppressWarnings("unchecked")
    public T getAdaptiveExtension() {
        Object instance = cachedAdaptiveInstance.getValue();
        if(instance!=null){
            return (T) instance;
        }
        if(createAdaptiveInstanceError!=null){
            // 自适应扩展对象创建失败
            throw new IllegalStateException("Failed to create adaptive instance: " + createAdaptiveInstanceError.toString(), createAdaptiveInstanceError);
        }
        synchronized (cachedAdaptiveInstance){
            instance = cachedAdaptiveInstance.getValue();
            if(instance!=null){
                return (T) instance;
            }
            instance =
        }

    }

    @SuppressWarnings("unchecked")
    public T getExtension(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Extension name can not be empty");
        }
        // 获取默认实现
        if ("true".equals(name)) {
            return getDefaultExtension();
        }

    }

    /**
     * 默认实现
     */
    public T getDefaultExtension() {
        getExtensionClasses();
        if (StringUtils.isBlank(cachedDefaultName) || "true".equals(cachedDefaultName)) {
            return null;
        }
        return getExtension(cachedDefaultName);
    }


    private static <T> boolean withExtensionAnnotation(Class<T> type) {
        return type.isAnnotationPresent(SPI.class);
    }

    private Map<String, Class<?>> getExtensionClasses() {
        Map<String, Class<?>> classes = cachedClasses.getValue();
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.getValue();
                if (classes == null) {
                    classes = loadExtensionClasses();
                    cachedClasses.setValue(classes);
                }
            }
        }
        return classes;
    }

    private Map<String, Class<?>> loadExtensionClasses() {
        cacheDefaultExtensionName();
        Map<String, Class<?>> extensionClasses = new HashMap<>();
        for (String loadDirectory : loadDirectories) {
            doLoadClass(extensionClasses, loadDirectory);
        }
        return extensionClasses;
    }

    private void cacheDefaultExtensionName() {
        SPI annotation = type.getAnnotation(SPI.class);
        if (annotation == null) {
            return;
        }
        String value = annotation.value().trim();
        if (value.isEmpty()) {
            return;
        }
        String[] names = NAME_SEPARATOR.split(value);
        // 默认扩展类只能有一个
        if (names.length > 1) {
            throw new IllegalStateException("More than 1 default extension name on extension " + type.getName() + ": " + Arrays.toString(names));
        }
        if (names.length == 1) {
            cachedDefaultName = names[0];
        }
    }

    private static ClassLoader findClassLoader() {
        ClassLoader cl = null;
        try {
            // 使用上下文类加载器
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ignore) {
        }
        if (cl != null) {
            return cl;
        }
        cl = ExtensionLoader.class.getClassLoader();
        if (cl == null) {
            try {
                cl = ClassLoader.getSystemClassLoader();
            } catch (Throwable ignore) {
            }
        }
        return cl;
    }

    /**
     * 加载实现类
     */
    private void doLoadClass(Map<String, Class<?>> extensionClasses, String dir) {
        String fileName = dir + type.getName();
        try {
            Enumeration<URL> urls;
            ClassLoader classLoader = findClassLoader();
            if (classLoader != null) {
                urls = classLoader.getResources(fileName);
            } else {
                urls = ClassLoader.getSystemResources(fileName);
            }
            if (urls == null) {
                return;
            }
            // 解析每个配置文件
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                doLoadResource(extensionClasses, classLoader, url);
            }
        } catch (Exception e) {
            log.error("Exception occurred when loading extension class (interface: " +
                    type + ", description file: " + fileName + ").", e);
        }
    }

    private void doLoadResource(Map<String, Class<?>> extensionClasses, ClassLoader classLoader, URL url) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()
                , StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 取出注释
                int ci = line.indexOf('#');
                if (ci >= 0) {
                    line = line.substring(0, ci);
                }
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                // 重试解析每一行配置，存储每行配置失败的原因
                try {
                    String name = null;
                    int i = line.indexOf('=');
                    if (i > 0) {
                        name = line.substring(0, i).trim();
                        line = line.substring(i + 1).trim();
                    }
                    if (!line.isEmpty()) {
                        doResolveClass(extensionClasses, url, Class.forName(line, true, classLoader), name);
                    }
                } catch (Throwable t) {
                    IllegalStateException e = new IllegalStateException("Failed to load extension class (interface: " + type + ", class line: " + line + ") in " + resourceURL + ", cause: " + t.getMessage(), t);
                    exceptions.put(line, e);
                }
            }
        } catch (Throwable t) {
            log.error("Exception occurred when loading extension class (interface: " +
                    type + ", class file: " + url + ") in " + url, t);
        }
    }

    private void doResolveClass(Map<String, Class<?>> extensionClasses, URL url, Class<?> clazz, String name) throws Exception {
        // 是否为接口实现类
        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Error occurred when loading extension class (interface: " +
                    type + ", class line: " + clazz.getName() + "), class " + clazz.getName() + " is not subtype of interface.");
        }
        if (clazz.isAnnotationPresent(Adaptive.class)) {
            cacheAdaptiveClass(clazz);
        } else if (isWrapperClass(clazz)) {
            cacheWrapperClass(clazz);
        } else {
            clazz.getConstructor();
            if (StringUtils.isEmpty(name)) {
                name = findAnnotationName(clazz);
            }
            if (name.isEmpty()) {
                throw new IllegalStateException("No such extension name for the class " + clazz.getName() + " in the config " + url);
            }
            String[] names = NAME_SEPARATOR.split(name);
            if (names != null && names.length > 0) {
                cacheActivateClass(clazz, names[0]);
                cacheName(clazz, names[0]);
                for (String n : names) {
                    saveInExtensionClass(extensionClasses, clazz, n);
                }
            }
        }
    }

    /**
     * 兼容java SPI, 没有名字生成一个名字
     */
    private String findAnnotationName(Class<?> clazz) {
        Extension extension = clazz.getAnnotation(Extension.class);
        if (extension != null) {
            return extension.value();
        }
        String name = clazz.getSimpleName();
        if (name.endsWith(type.getSimpleName())) {
            name = name.substring(0, name.length() - type.getSimpleName().length());
        }
        return name.toLowerCase();
    }

    private boolean isWrapperClass(Class<?> clazz) {
        try {
            clazz.getConstructor(type);
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

    private void saveInExtensionClass(Map<String, Class<?>> extensionClasses, Class<?> clazz, String name) {
        Class<?> c = extensionClasses.get(name);
        if (c == null) {
            extensionClasses.put(name, clazz);
            return;
        }
        if (c != clazz) {
            throw new IllegalStateException("Duplicate extension " + type.getName() + " name " + name + " on " + c.getName() + " and " + clazz.getName());
        }
    }

    private void cacheName(Class<?> clazz, String name) {
        if (!cachedNames.containsKey(clazz)) {
            cachedNames.put(clazz, name);
        }
    }

    private void cacheActivateClass(Class<?> clazz, String name) {
        Activate annotation = clazz.getAnnotation(Activate.class);
        if (annotation != null) {
            cachedActivates.put(name, annotation);
        }
    }

    private void cacheWrapperClass(Class<?> clazz) {
        if (cachedWrapperClasses == null) {
            cachedWrapperClasses = new ConcurrentHashMap<>();
        }
        cachedWrapperClasses.put(clazz, Boolean.TRUE);
    }

    private void cacheAdaptiveClass(Class<?> clazz) {
        if (cachedAdaptiveClass == null) {
            cachedAdaptiveClass = clazz;
            return;
        }
        if (!cachedAdaptiveClass.equals(clazz)) {
            throw new IllegalStateException("More than 1 adaptive class found: "
                    + cachedAdaptiveClass.getName() + ", " + clazz.getName());
        }
    }

    private T createAdaptiveExtension(){

    }

    private Class<?> getAdaptiveExtensionClass() {
        getExtensionClasses();
        if(cachedAdaptiveClass!=null){
            return cachedAdaptiveClass;
        }
        return cachedAdaptiveClass =
    }

    private Class<?> createAdaptiveExtensionClass(){

    }
}
