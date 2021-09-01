package com.easyutils.limiter;

import java.io.Serializable;
import lombok.Data;

@Data
public class RateLimiterResp implements Serializable {

  private static final long serialVersionUID = -2266222572473715497L;

  /**
   * 是否被限流 ture限流 false不限流
   */
  private boolean success;

  /**
   * 容量
   */
  private long capacity;

  /**
   * 剩余令牌数
   */
  private long rest;

  /**
   * 如果被限流，下次重试时间
   */
  private long tryAfterSeconds;
}
