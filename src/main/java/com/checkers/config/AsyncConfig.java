package com.checkers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;

/**
 * Configures a separate bot executor and delayed-task scheduler so search and timers do not occupy
 * HTTP/STOMP request threads.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /**
     * Creates the bot calculation pool with two core threads, up to eight workers and a queue of
     * one hundred tasks. Search therefore runs outside the WebSocket request thread.
     */
    @Bean(name = "botExecutor")
    public Executor botExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("bot-ai-");
        executor.initialize();
        return executor;
    }

    /**
     * Creates a four-thread scheduler for delayed actions, including reconnect deadlines and
     * matchmaking timeouts.
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("game-sched-");
        scheduler.initialize();
        return scheduler;
    }
}
