package com.easyutils.timer;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

class HashedWheelTimerTest {

    @Test
    void newTimeout() throws Exception {
        HashedWheelTimer timer = new HashedWheelTimer(runnable -> new Thread(runnable, "timer-thread"), 1, TimeUnit.SECONDS, 60, 200);
        int base = 2;
        for (int i = 1; i < 50; i++) {
            int delay = i * base;
            System.out.println(delay);
            timer.newTimeout((timeout) -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                System.out.println(formatter.format(now));
            }, delay, TimeUnit.SECONDS);
        }
        Thread.sleep(100000);
        timer.stop();
    }
}