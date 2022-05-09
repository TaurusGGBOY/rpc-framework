package github.ggb.utils.concurrent.threadpool;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Slf4j
public class ThreadPoolFactoryUtil {
    private static final Map<String, ExecutorService> THREAD_POOL = new ConcurrentHashMap<>();

    public ThreadPoolFactoryUtil() {
    }

    public static ExecutorService createCustomThreadPoolIfAbsent(String threadNamePrefix) {
        CustomThreadPoolConfig customThreadPoolConfig = new CustomThreadPoolConfig();
        return createCustomThreadPoolIfAbsent(customThreadPoolConfig, threadNamePrefix, false);
    }

    private static ExecutorService createCustomThreadPoolIfAbsent(CustomThreadPoolConfig customThreadPoolConfig, String threadNamePrefix, boolean daemon) {
        THREAD_POOL.computeIfAbsent(threadNamePrefix, k -> createThreadPool(customThreadPoolConfig, threadNamePrefix, daemon));
    }

    private static ExecutorService createThreadPool(CustomThreadPoolConfig customThreadPoolConfig, String threadNamePrefix, boolean daemon) {
        ThreadFactory threadFactory = createThreadFactory(threadNamePrefix, daemon);
    }

    private static ThreadFactory createThreadFactory(String threadNamePrefix, boolean daemon) {
        if (threadNamePrefix == null) {
            return Executors.defaultThreadFactory();
        }
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(threadNamePrefix + "-%d");
        if (!daemon) {
            return threadFactory.build();
        }
        return threadFactory.setDaemon(daemon).build();
    }

}