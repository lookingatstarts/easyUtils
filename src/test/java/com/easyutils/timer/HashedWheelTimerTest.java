package com.easyutils.timer;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

class HashedWheelTimerTest {

    @Test
    void newTimeout() throws Exception {

        HashedWheelTimer timer = new HashedWheelTimer(runnable -> new Thread(runnable, "timer-thread"), 1, TimeUnit.SECONDS, 60, 100);
        timer.newTimeout((timeout) -> {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            System.out.println(formatter.format(now));
        }, 1, TimeUnit.SECONDS);
        timer.newTimeout((timeout) -> {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            System.out.println(formatter.format(now));
        }, 5, TimeUnit.SECONDS);
        timer.newTimeout((timeout) -> {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            System.out.println(formatter.format(now));
        }, 10, TimeUnit.SECONDS);
        timer.newTimeout((timeout) -> {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            System.out.println(formatter.format(now));
        }, 15, TimeUnit.SECONDS);
        timer.newTimeout((timeout) -> {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            System.out.println(formatter.format(now));
        }, 20, TimeUnit.SECONDS);


        Thread.sleep(100000);
        timer.stop();
    }
}