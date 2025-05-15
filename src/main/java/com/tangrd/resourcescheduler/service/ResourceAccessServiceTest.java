package com.tangrd.resourcescheduler.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 仅在测试配置文件激活时运行的测试类
 * 使用方法：在启动参数中添加 -Dspring.profiles.active=test
 */
@Component
@Profile("test")
@RequiredArgsConstructor
public class ResourceAccessServiceTest implements CommandLineRunner {
  private static final Logger logger = LoggerFactory.getLogger(ResourceAccessServiceTest.class);

  private final ResourceAccessService resourceAccessService;

  @Override
  public void run(String... args) {
    logger.info("Starting resource access tests...");

    // 1. 测试顺序访问
    testSequentialAccess();

    // 2. 测试并行访问
    testParallelAccess();

    // 3. 测试优先级调度
    testPriorityScheduling();

    logger.info("Tests completed!");
  }

  private void testSequentialAccess() {
    logger.info("=== Sequential access test ===");

    for (int i = 0; i < 3; i++) {
      String result = resourceAccessService.accessResource(1);
      logger.info("Sequential access result: {}", result);
    }
  }

  private void testParallelAccess() {
    logger.info("=== Parallel access test ===");

    ExecutorService executor = Executors.newFixedThreadPool(5);
    CompletableFuture<?>[] futures = new CompletableFuture[5];

    for (int i = 0; i < 5; i++) {
      final int index = i;
      futures[i] = CompletableFuture.runAsync(() -> {
        String result = resourceAccessService.accessResource(1);
        logger.info("Parallel access #{} result: {}", index, result);
      }, executor);
    }

    CompletableFuture.allOf(futures).join();
    executor.shutdown();
  }

  private void testPriorityScheduling() {
    logger.info("=== Priority scheduling test ===");

    ExecutorService executor = Executors.newFixedThreadPool(10);

    // 提交低优先级请求
    for (int i = 0; i < 3; i++) {
      final int index = i;
      CompletableFuture.runAsync(() -> {
        try {
          TimeUnit.MILLISECONDS.sleep(100 * index);
          String result = resourceAccessService.accessResource(1);
          logger.info("Low priority task #{} result: {}", index, result);
        } catch (Exception e) {
          logger.error("Task execution error", e);
        }
      }, executor);
    }

    // 等待一下，然后提交中优先级请求
    try {
      TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // 提交中优先级请求
    for (int i = 0; i < 3; i++) {
      final int index = i;
      CompletableFuture.runAsync(() -> {
        try {
          TimeUnit.MILLISECONDS.sleep(100 * index);
          String result = resourceAccessService.accessResource(5);
          logger.info("Medium priority task #{} result: {}", index, result);
        } catch (Exception e) {
          logger.error("Task execution error", e);
        }
      }, executor);
    }

    // 再等待一下，然后提交高优先级请求
    try {
      TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // 提交高优先级请求
    for (int i = 0; i < 3; i++) {
      final int index = i;
      CompletableFuture.runAsync(() -> {
        try {
          TimeUnit.MILLISECONDS.sleep(100 * index);
          String result = resourceAccessService.accessResource(10);
          logger.info("High priority task #{} result: {}", index, result);
        } catch (Exception e) {
          logger.error("Task execution error", e);
        }
      }, executor);
    }

    // 等待所有任务完成
    executor.shutdown();
    try {
      executor.awaitTermination(30, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      logger.error("Task completion wait interrupted", e);
      Thread.currentThread().interrupt();
    }
  }
} 
