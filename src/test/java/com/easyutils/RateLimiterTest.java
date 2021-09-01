package com.easyutils;

import com.easyutils.limiter.RateLimiter;
import java.util.concurrent.TimeUnit;

public class RateLimiterTest {

  public static void main(String[] args) {
    RateLimiter limiter = new RateLimiter("action", 1, 1, 5);
    while (true) {
      try {
        TimeUnit.SECONDS.sleep(1);
      } catch (InterruptedException ignore) {
      }
      System.out.println(limiter.isAllow(1).isSuccess());
    }
  }
}
