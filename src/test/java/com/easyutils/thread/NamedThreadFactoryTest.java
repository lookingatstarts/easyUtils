package com.easyutils.thread;

import org.junit.jupiter.api.Test;

class NamedThreadFactoryTest {

  @Test
  public void thread() throws Exception {
    NamedThreadFactory namedThreadFactory = new NamedThreadFactory("test-io", true);
    NamedThreadFactory namedThreadFactory2 = new NamedThreadFactory("test-io", true);
    namedThreadFactory.newThread(() -> System.out.println(Thread.currentThread().getName()))
        .start();
    namedThreadFactory2.newThread(() -> System.out.println(Thread.currentThread().getName()))
        .start();
    Thread.sleep(4000L);
  }
}