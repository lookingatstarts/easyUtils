package com.easyutils.type;

public class TypeConvertor {

  public static <T> T convert(Class<T> type, String value) {
    if (value == null || value.isEmpty()) {
      return null;
    }
    Object obj = value;
    // String类型
    if (type.isInstance(obj)) {
      return type.cast(obj);
    }
    // boolean
    if (Boolean.class.equals(type) || Boolean.TYPE.equals(type)) {
      obj = Boolean.valueOf(value);
    } else if (Number.class.isAssignableFrom(type) || type.isPrimitive()) { // Number类型
      if (Long.class.equals(type) || Long.TYPE.equals(type)) {
        obj = Long.valueOf(value);
      }
      if (Integer.class.equals(type) || Integer.TYPE.equals(type)) {
        obj = Integer.valueOf(value);
      }
      if (Byte.class.equals(type) || Byte.TYPE.equals(type)) {
        obj = Byte.valueOf(value);
      }
      if (Short.class.equals(type) || Short.TYPE.equals(type)) {
        obj = Short.valueOf(value);
      }
      if (Float.class.equals(type) || Float.TYPE.equals(type)) {
        obj = Float.valueOf(value);
      }
      if (Double.class.equals(type) || Double.TYPE.equals(type)) {
        obj = Double.valueOf(value);
      }
    } else if (type.isEnum()) { // 枚举类型
      obj = Enum.valueOf(type.asSubclass(Enum.class), value);
    }
    return type.cast(obj);
  }
}
