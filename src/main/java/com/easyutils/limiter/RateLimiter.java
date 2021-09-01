package com.easyutils.limiter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import redis.clients.jedis.Jedis;

/**
 * 利用redis-cell模块cl.throttle命令
 */
public class RateLimiter {

  private final String capacity;
  private final String times;
  private final String seconds;
  private final String action;
  private final Jedis jedis = new Jedis();

  public RateLimiter(String action, int capacity, int times, int seconds) {
    this.capacity = capacity + "";
    this.times = times + "";
    this.seconds = seconds + "";
    this.action = action;
  }

  public RateLimiterResp isAllow(int count) {
    String script = "return redis.call('cl.throttle',KEYS[1], ARGV[1], ARGV[2], ARGV[3], ARGV[4])";
    @SuppressWarnings("unchecked")
    List<Long> result = (List<Long>) jedis.eval(script, Collections.singletonList(action),
        Arrays.asList(capacity, times, seconds, count + ""));
    RateLimiterResp resp = new RateLimiterResp();
    resp.setSuccess(result.get(0) == 0L);
    resp.setCapacity(result.get(1));
    resp.setRest(result.get(2));
    resp.setTryAfterSeconds(result.get(4) == -1L ? result.get(4) : 0L);
    return resp;
  }
}
