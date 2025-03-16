package com.easyutils.type;

import org.junit.jupiter.api.Test;

class TypeConvertorTest {

  @Test
  void convert() {
    System.out.println(TypeConvertor.convert(String.class, "ha"));
    System.out.println(TypeConvertor.convert(Long.class, "1000"));
    System.out.println(TypeConvertor.convert(Short.class, "2"));
    System.out.println(TypeConvertor.convert(EnumDemo.class, "A"));
    System.out.println(TypeConvertor.convert(EnumDemo.class, "B"));
  }

  enum EnumDemo {
    A,
    B
  }
}