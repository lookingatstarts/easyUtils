package com.easyutils;

import com.easyutils.limiter.RateLimiter;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class RateLimiterTest {

  @Test
  public void test(){
    RateLimiter limiter = new RateLimiter("action", 1, 1, 5);
    for(int i=0;i<20;i++) {
      try {
        TimeUnit.SECONDS.sleep(1);
      } catch (InterruptedException ignore) {
      }
      System.out.println(limiter.isAllow(1).isSuccess());
    }
  }
}
