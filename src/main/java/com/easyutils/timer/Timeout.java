package com.easyutils.timer;

/**
 * 1、关联TimerTask Timer
 * 2、取消任务
 * 3、查询任务状态
 */
public interface Timeout {

    Timer timer();

    TimerTask task();

    boolean isExpired();

    boolean isCancelled();

    boolean cancel();
}
