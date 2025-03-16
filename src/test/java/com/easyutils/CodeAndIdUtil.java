package com.easyutils;


import java.net.URLEncoder;

public class CodeAndIdUtil {

  private static final Long mod = 10000L;

  public static Long hashWithMod(String uid) {
    String md5Value = DigestUtils.md5DigestAsHex(uid.getBytes());
    return Long.valueOf(md5Value.substring(0, 4), 16) % mod;
  }

  private static String data1 = "税友信息技术有限公司 ";
  private static String data2 = "税友信息技术有限公司\u2028";

  public static void main(String[] args) throws Exception {
    System.out.print(data1 + "空格");
    System.out.println();
    System.out.println(URLEncoder.encode(data1, "utf-8").equals(URLEncoder.encode(data2, "utf-8")));
    System.out.print(data2 + "空格");
    System.out.println();
    System.out.println(URLEncoder.encode(data2, "utf-8"));
  }
}
