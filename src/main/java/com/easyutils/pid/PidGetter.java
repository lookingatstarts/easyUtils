package com.easyutils.pid;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

/**
 * 获取进程pid
 */
public class PidGetter {
    public static Integer currentPid(){
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String name = runtime.getName();
        return Integer.parseInt(name.substring(0, name.indexOf('@')));
    }
}
