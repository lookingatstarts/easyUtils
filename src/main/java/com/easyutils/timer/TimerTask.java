package com.easyutils.timer;

/**
 * 定时任务
 */
public interface TimerTask {

    void run(Timeout timeout) throws Exception;
}
