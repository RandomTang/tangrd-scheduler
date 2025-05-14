package com.example.resourcescheduler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
public class ResourceAccessServiceTest implements CommandLineRunner {
  private static final Logger logger = LoggerFactory.getLogger(ResourceAccessServiceTest.class);

  private final ResourceAccessService resourceAccessService;

  @Autowired
  public ResourceAccessServiceTest(ResourceAccessService resourceAccessService) {
    this.resourceAccessService = resourceAccessService;
  }

  @Override
  public void run(String... args) {
    logger.info("开始测试资源访问服务...");

    // 1. 测试顺序访问
    testSequentialAccess();

    // 2. 测试并行访问
    testParallelAccess();

    // 3. 测试优先级调度
    testPriorityScheduling();

    logger.info("测试完成！");
  }

  private void testSequentialAccess() {
    logger.info("=== 测试顺序访问 ===");

    for (int i = 0; i < 3; i++) {
      String result = resourceAccessService.accessResource(1);
      logger.info("顺序访问结果: {}", result);
    }
  }

  private void testParallelAccess() {
    logger.info("=== 测试并行访问 ===");

    ExecutorService executor = Executors.newFixedThreadPool(5);
    CompletableFuture<?>[] futures = new CompletableFuture[5];

    for (int i = 0; i < 5; i++) {
      final int index = i;
      futures[i] = CompletableFuture.runAsync(() -> {
        String result = resourceAccessService.accessResource(1);
        logger.info("并行访问 #{} 结果: {}", index, result);
      }, executor);
    }

    CompletableFuture.allOf(futures).join();
    executor.shutdown();
  }

  private void testPriorityScheduling() {
    logger.info("=== 测试优先级调度 ===");

    ExecutorService executor = Executors.newFixedThreadPool(10);

    // 提交低优先级请求
    for (int i = 0; i < 3; i++) {
      final int index = i;
      CompletableFuture.runAsync(() -> {
        try {
          TimeUnit.MILLISECONDS.sleep(100 * index);
          String result = resourceAccessService.accessResource(1);
          logger.info("低优先级请求 #{} 结果: {}", index, result);
        } catch (Exception e) {
          logger.error("请求出错", e);
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
          logger.info("中优先级请求 #{} 结果: {}", index, result);
        } catch (Exception e) {
          logger.error("请求出错", e);
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
          logger.info("高优先级请求 #{} 结果: {}", index, result);
        } catch (Exception e) {
          logger.error("请求出错", e);
        }
      }, executor);
    }

    // 等待所有任务完成
    executor.shutdown();
    try {
      executor.awaitTermination(30, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      logger.error("等待任务完成被中断", e);
      Thread.currentThread().interrupt();
    }
  }
} 
