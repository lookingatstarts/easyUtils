package com.easyutils.timer;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class HashedWheelTimer implements Timer {
    private static final int INSTANCE_COUNT_LIMIT = 64;
    private static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger(0);
    private static final AtomicBoolean WARNED_TOO_MANY_INSTANCES = new AtomicBoolean();
    private static final AtomicIntegerFieldUpdater<HashedWheelTimer> WORKER_STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(HashedWheelTimer.class, "workerState");
    /**
     * workerThread执行worker
     */
    private final Worker worker = new Worker();
    private final Thread workerThread;
    private static final int WORKER_STATE_INIT = 0;
    private static final int WORKER_STATE_STARTED = 1;
    private static final int WORKER_STATE_SHUTDOWN = 2;

    /**
     * 0 - init, 1 - started, 2 - shut down
     * 定时器状态
     */
    @SuppressWarnings({"unused", "FieldMayBeFinal"})
    private volatile int workerState;
    private final HashedWheelBucket[] wheel;
    /**
     * 时间指针每次加 1所代表的实际时间，单位为纳秒
     */
    private final long tickDuration;
    private final int mask;

    /**
     * 暂存取消的任务
     */
    private final Queue<HashedWheelTimeout> cancelledTimeouts = new LinkedBlockingQueue<>();
    /**
     * 暂存添加的任务
     */
    private final Queue<HashedWheelTimeout> timeouts = new LinkedBlockingQueue<>();
    /**
     * 当前时间轮剩余的定时任务总数
     */
    private final AtomicLong pendingTimeouts = new AtomicLong(0);
    private final long maxPendingTimeouts;
    /**
     * 启动时间
     */
    private volatile long startTime;
    /**
     * 多等一
     */
    private final CountDownLatch startTimeInitialized = new CountDownLatch(1);

    /**
     * 构造器
     */
    public HashedWheelTimer(ThreadFactory threadFactory,
                            long tickDuration, TimeUnit unit, int ticksPerWheel,
                            long maxPendingTimeouts) {
        if (threadFactory == null) {
            throw new NullPointerException("threadFactory");
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (tickDuration <= 0) {
            throw new IllegalArgumentException("tickDuration must be greater than 0: " + tickDuration);
        }
        if (ticksPerWheel <= 0) {
            throw new IllegalArgumentException("ticksPerWheel must be greater than 0: " + ticksPerWheel);
        }
        wheel = createWheel(ticksPerWheel);
        mask = wheel.length - 1;
        this.tickDuration = unit.toNanos(tickDuration);
        // 防止tickDuration * wheel.length > Long.MAX_VALUE 溢出
        if (this.tickDuration >= Long.MAX_VALUE / wheel.length) {
            throw new IllegalArgumentException(String.format("tickDuration: %d (expected: 0 < tickDuration in nanos < %d", tickDuration, Long.MAX_VALUE / wheel.length));
        }
        workerThread = threadFactory.newThread(worker);
        this.maxPendingTimeouts = maxPendingTimeouts;
        // 告警：HashedWheelTimer创建太多
        if (INSTANCE_COUNTER.incrementAndGet() > INSTANCE_COUNT_LIMIT &&
                WARNED_TOO_MANY_INSTANCES.compareAndSet(false, true)) {
            String resourceType = this.getClass().getSimpleName();
            log.error("You are creating too many " + resourceType + " instances. " +
                    resourceType + " is a shared resource that must be reused across the JVM," +
                    "so that only a few instances are created.");
        }
    }

    /**
     * 对象被回收时，设置关闭状态
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            super.finalize();
        } finally {
            if (WORKER_STATE_UPDATER.getAndSet(this, WORKER_STATE_SHUTDOWN) == WORKER_STATE_SHUTDOWN) {
                INSTANCE_COUNTER.decrementAndGet();
            }
        }
    }

    /**
     * 关闭定时器
     */
    @Override
    public Set<Timeout> stop() {
        if (Thread.currentThread() == workerThread) {
            throw new IllegalStateException(
                    HashedWheelTimer.class.getSimpleName() +
                            ".stop() cannot be called from " +
                            TimerTask.class.getSimpleName());
        }
        // workState可能为0或2
        // 先尝试关闭，关闭失败，判断是否已关闭
        if (!WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_STARTED, WORKER_STATE_SHUTDOWN)) {
            // 有可能定时器未启动
            if (WORKER_STATE_UPDATER.getAndSet(this, WORKER_STATE_SHUTDOWN) == WORKER_STATE_SHUTDOWN) {
                INSTANCE_COUNTER.decrementAndGet();
            }
            return Collections.emptySet();
        }
        try {
            boolean interrupted = false;
            while (workerThread.isAlive()) {
                workerThread.interrupt();
                try {
                    // 等待workerThread完成，阻塞当前线程
                    workerThread.join(100);
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            // 恢复中断标识
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        } finally {
            INSTANCE_COUNTER.decrementAndGet();
        }
        return Collections.unmodifiableSet(worker.unprocessedTimeouts);
    }

    /**
     * 启动定时器
     */
    private void start() {
        switch (WORKER_STATE_UPDATER.get(this)) {
            case WORKER_STATE_INIT:
                if (WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_INIT, WORKER_STATE_STARTED)) {
                    workerThread.start();
                    System.out.println("定时器启动");
                }
                break;
            case WORKER_STATE_STARTED:
                break;
            case WORKER_STATE_SHUTDOWN:
                throw new IllegalStateException("cannot be started once stopped");
            default:
                throw new Error("Invalid WorkerState");
        }
        while (startTime == 0) {
            try {
                startTimeInitialized.await();
            } catch (Exception ignore) {

            }
        }
    }

    @Override
    public Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
        if (task == null) {
            throw new NullPointerException("task");
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        // 等待处理任务数+1
        long pendingTimeoutsCount = pendingTimeouts.incrementAndGet();
        if (maxPendingTimeouts > 0 && pendingTimeoutsCount > maxPendingTimeouts) {
            pendingTimeouts.decrementAndGet();
            throw new RejectedExecutionException("Number of pending timeouts ("
                    + pendingTimeoutsCount + ") is greater than or equal to maximum allowed pending "
                    + "timeouts (" + maxPendingTimeouts + ")");
        }
        // 启动定时器
        start();
        long deadline = System.nanoTime() + unit.toNanos(delay) - startTime;
        if (delay > 0 && deadline < 0) {
            deadline = Long.MAX_VALUE;
        }
        HashedWheelTimeout timeout = new HashedWheelTimeout(this, task, deadline);
        timeouts.add(timeout);
        return timeout;
    }

    @Override
    public boolean isStopped() {
        return WORKER_STATE_UPDATER.get(this) == WORKER_STATE_SHUTDOWN;
    }

    private static HashedWheelBucket[] createWheel(int ticksPerWheel) {
        if (ticksPerWheel <= 0) {
            throw new IllegalArgumentException(
                    "ticksPerWheel must be greater than 0: " + ticksPerWheel);
        }
        if (ticksPerWheel > 1073741824) {
            throw new IllegalArgumentException(
                    "ticksPerWheel may not be greater than 2^30: " + ticksPerWheel);
        }
        ticksPerWheel = normalizeTicksPerWheel(ticksPerWheel);
        HashedWheelBucket[] wheel = new HashedWheelBucket[ticksPerWheel];
        for (int i = 0; i < wheel.length; i++) {
            wheel[i] = new HashedWheelBucket();
        }
        return wheel;
    }

    /**
     * 2^n 方便取模
     */
    private static int normalizeTicksPerWheel(int ticksPerWheel) {
        int normalizedTicksPerWheel = ticksPerWheel - 1;
        normalizedTicksPerWheel |= normalizedTicksPerWheel >>> 1;
        normalizedTicksPerWheel |= normalizedTicksPerWheel >>> 2;
        normalizedTicksPerWheel |= normalizedTicksPerWheel >>> 4;
        normalizedTicksPerWheel |= normalizedTicksPerWheel >>> 8;
        normalizedTicksPerWheel |= normalizedTicksPerWheel >>> 16;
        return normalizedTicksPerWheel + 1;
    }

    /**
     * 定时器任务
     */
    private final class Worker implements Runnable {

        /**
         * 定时器关闭时会将未处理的问题放入到这个集合中
         */
        private final Set<Timeout> unprocessedTimeouts = new HashSet<>();
        /**
         * 时间轮的指针，是一个步长为1的单调递增计数器。
         * mask & tick 定位bucket
         */
        private long tick;

        @Override
        public void run() {
            // 初始化定时器启动时间
            startTime = System.nanoTime();
            if (startTime == 0) {
                startTime = 1;
            }
            // 唤醒调用start启动timer阻塞的线程
            startTimeInitialized.countDown();
            do {
                final long deadline = waitForNextTick();
                int idx = (int) (tick & mask);
                processCancelledTasks();
                transferTimeoutsToBucks();
                wheel[idx].expireTimeouts(deadline);
                tick++;
            } while (WORKER_STATE_UPDATER.get(HashedWheelTimer.this) == WORKER_STATE_STARTED);
            // 定时器结束
            for (HashedWheelBucket bucket : wheel) {
                bucket.clearTimeouts(unprocessedTimeouts);
            }
            for (; ; ) {
                HashedWheelTimeout timeout = timeouts.poll();
                if (timeout == null) {
                    break;
                }
                if (!timeout.isCancelled()) {
                    unprocessedTimeouts.add(timeout);
                }
            }
            processCancelledTasks();
        }

        private long waitForNextTick() {
            long deadline = tickDuration * (tick + 1);
            for (; ; ) {
                final long currentTime = System.nanoTime() - startTime;
                // 阻塞时间从纳秒转毫秒，这里加999999的目的是，及时deadline-currentTime未达到1ms，也会
                // 按1ms计算
                long sleepTimeInMs = (deadline - currentTime + 999_999) / 1000_000;
                // 执行任务的时间到了
                if (sleepTimeInMs <= 0) {
                    if (currentTime == Long.MIN_VALUE) {
                        return -Long.MAX_VALUE;
                    }
                    return currentTime;
                }
                try {
                    if (isWindows()) {
                        sleepTimeInMs = sleepTimeInMs / 10 * 10;
                    }
                    Thread.sleep(sleepTimeInMs);
                } catch (Exception e) {
                    // 如果定时器关闭了
                    if (WORKER_STATE_UPDATER.get(HashedWheelTimer.this) == WORKER_STATE_SHUTDOWN) {
                        return Long.MIN_VALUE;
                    }
                }
            }
        }

        private void processCancelledTasks() {
            // 处理取消任务
            for (; ; ) {
                HashedWheelTimeout timeout = cancelledTimeouts.poll();
                if (timeout == null) {
                    return;
                }
                try {
                    // 从时间轮中移除
                    timeout.remove();
                } catch (Throwable t) {
                    log.warn("An exception was thrown while process a cancellation task", t);
                }
            }
        }

        /**
         * 将暂存的任务转移到时间轮中
         * tick可以认为是过去的时间，通过deadline计算任务应该放在哪个时间轮上
         */
        private void transferTimeoutsToBucks() {
            for (int i = 0; i < 100000; i++) {
                HashedWheelTimeout timeout = timeouts.poll();
                if (timeout == null) {
                    break;
                }
                if (timeout.isCancelled()) {
                    continue;
                }
                long calculated = timeout.deadline / tickDuration;
                timeout.remainingRounds = (calculated - tick) / wheel.length;
                final long ticks = Math.max(calculated, tick);
                int idx = (int) (ticks & mask);
                wheel[idx].addTimeout(timeout);
            }
        }

        /**
         * 判断是否为windows系统
         */
        private boolean isWindows() {
            return System.getProperty("os.name", "").toLowerCase(Locale.US).contains("win");
        }
    }

    /**
     * 1、任务双端列表节点
     * 2、封装任务状态,类似Future
     * 3、关联TimeTask Timer
     */
    @Slf4j
    private static final class HashedWheelTimeout implements Timeout {

        private static final int ST_INIT = 0;
        private static final int ST_CANCELLED = 1;
        private static final int ST_EXPIRED = 2;
        private static final AtomicIntegerFieldUpdater<HashedWheelTimeout> STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(HashedWheelTimeout.class, "state");
        private final HashedWheelTimer timer;
        private final TimerTask task;
        private final long deadline;
        long remainingRounds;
        /**
         * 时间轮，添加进时间轮时设置
         */
        HashedWheelBucket bucket;
        private volatile int state = ST_INIT;
        HashedWheelTimeout prev;
        HashedWheelTimeout next;

        public HashedWheelTimeout(HashedWheelTimer timer, TimerTask task, long deadline) {
            this.timer = timer;
            this.task = task;
            this.deadline = deadline;
        }

        @Override
        public Timer timer() {
            return timer;
        }

        @Override
        public TimerTask task() {
            return task;
        }

        @Override
        public boolean isExpired() {
            return state == ST_EXPIRED;
        }

        @Override
        public boolean isCancelled() {
            return state == ST_CANCELLED;
        }

        @Override
        public boolean cancel() {
            if (!STATE_UPDATER.compareAndSet(this, ST_INIT, ST_CANCELLED)) {
                return false;
            }
            this.timer.cancelledTimeouts.add(this);
            return true;
        }

        public void expire() {
            if (!STATE_UPDATER.compareAndSet(this, ST_INIT, ST_EXPIRED)) {
                return;
            }
            try {
                task.run(this);
            } catch (Exception e) {
                log.warn("An exception was thrown by " + TimerTask.class.getSimpleName() + '.', e);
            }
        }

        /**
         * 从时间轮中删除
         */
        void remove() {
            HashedWheelBucket bucket = this.bucket;
            if (bucket == null) {
                timer.pendingTimeouts.decrementAndGet();
                return;
            }
            bucket.remove(this);
        }

        public int state() {
            return state;
        }

        @Override
        public String toString() {
            final long currentTime = System.nanoTime();
            // 任务剩余执行时间
            long remaining = deadline - currentTime + timer.startTime;
            String simpleClassName = this.getClass().getSimpleName();
            StringBuilder buf = new StringBuilder(192)
                    .append(simpleClassName)
                    .append('(')
                    .append("deadline: ");
            if (remaining > 0) {
                buf.append(remaining).append(" ns later");
            } else if (remaining < 0) {
                buf.append(-remaining)
                        .append(" ns ago");
            } else {
                buf.append("now");
            }
            if (isCancelled()) {
                buf.append(", cancelled");
            }
            return buf.append(", task: ")
                    .append(task())
                    .append(')')
                    .toString();
        }
    }

    /**
     * 时间轮
     * 1、维护任务双端列表
     */
    private static final class HashedWheelBucket {
        /**
         * 维护双向列表
         */
        private HashedWheelTimeout head, tail;

        /**
         * 移除指定任务，并返回它的下一个任务
         */
        public HashedWheelTimeout remove(HashedWheelTimeout timeout) {
            HashedWheelTimeout next = timeout.next;
            // 断前后链接
            if (timeout.prev != null) {
                timeout.prev.next = next;
            }
            if (timeout.next != null) {
                timeout.next.prev = timeout.prev;
            }
            if (timeout == head) {
                if (timeout == tail) {
                    tail = null;
                    head = null;
                } else {
                    head = next;
                }
            } else if (timeout == tail) {
                tail = timeout.prev;
            }
            // help gc
            timeout.prev = null;
            timeout.next = null;
            timeout.bucket = null;
            // 等待处理任务-1
            timeout.timer.pendingTimeouts.decrementAndGet();
            return next;
        }

        void addTimeout(HashedWheelTimeout timeout) {
            timeout.bucket = this;
            if (head == null) {
                head = tail = timeout;
                return;
            }
            // 追加到尾部
            tail.next = timeout;
            timeout.prev = tail;
            tail = timeout;
        }

        void expireTimeouts(long deadline) {
            HashedWheelTimeout timeout = head;
            while (timeout != null) {
                HashedWheelTimeout next = timeout.next;
                if (timeout.remainingRounds <= 0) {
                    // 从任务队列移除任务
                    next = this.remove(timeout);
                    if (timeout.deadline <= deadline) {
                        timeout.expire();
                    } else {
                        // 正常不会走到这，除非添加错了时间轮
                        throw new IllegalStateException(String.format(
                                "timeout.deadline (%d) > deadline (%d)", timeout.deadline, deadline));
                    }
                } else if (timeout.isCancelled()) {
                    next = this.remove(timeout);
                } else {
                    timeout.remainingRounds--;
                }
                timeout = next;
            }
        }

        /**
         * 定时器关闭时将未执行的任务防止set中
         */
        void clearTimeouts(Set<Timeout> set) {
            for (; ; ) {
                HashedWheelTimeout timeout = poll();
                if (timeout == null) {
                    return;
                }
                if (timeout.isCancelled() || timeout.isExpired()) {
                    continue;
                }
                set.add(timeout);
            }
        }

        /**
         * 移除队列头部元素
         */
        private HashedWheelTimeout poll() {
            HashedWheelTimeout head = this.head;
            if (head == null) {
                return null;
            }
            HashedWheelTimeout next = head.next;
            if (next == null) {
                this.head = null;
                this.tail = null;
            } else {
                this.head = next;
                next.prev = null;
            }
            head.next = null;
            head.prev = null;
            head.bucket = null;
            return head;
        }
    }
}
