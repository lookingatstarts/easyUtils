package com.easyutils;

import org.junit.jupiter.api.Test;

class StyleConvertorTest {

  @Test
  public void camelToSplitName() {
    System.out.println(StyleConvertor.camelToSplitName("CamelToSplitName", "."));
  }
}