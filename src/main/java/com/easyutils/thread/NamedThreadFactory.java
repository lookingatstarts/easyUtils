package com.easyutils.thread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {

  private static final AtomicInteger POOL_INCR = new AtomicInteger(0);
  private final AtomicInteger thread_incr = new AtomicInteger(0);
  private final boolean daemon;
  private final ThreadGroup group;
  private final String namePrefix;

  public NamedThreadFactory(String namePrefix, boolean daemon) {
    namePrefix = namePrefix + "-pool-" + POOL_INCR.incrementAndGet() + "-thread-";
    SecurityManager securityManager = System.getSecurityManager();
    ThreadGroup group = Thread.currentThread().getThreadGroup();
    if (securityManager != null) {
      group = securityManager.getThreadGroup();
    }
    this.namePrefix = namePrefix;
    this.group = group;
    this.daemon = daemon;
  }

  @Override
  public Thread newThread(Runnable r) {
    Thread thread = new Thread(group, r, namePrefix + thread_incr.getAndIncrement());
    thread.setDaemon(daemon);
    return thread;
  }
}
