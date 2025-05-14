package com.example.resourcescheduler.controller;

import com.example.resourcescheduler.service.ResourceAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/resource")
public class ResourceController {
  private static final Logger logger = LoggerFactory.getLogger(ResourceController.class);

  private final ResourceAccessService resourceAccessService;

  @Autowired
  public ResourceController(ResourceAccessService resourceAccessService) {
    this.resourceAccessService = resourceAccessService;
  }

  @GetMapping("/access")
  public ResponseEntity<String> accessResource(@RequestParam(defaultValue = "0") int priority) {
    String result = resourceAccessService.accessResource(priority);
    return ResponseEntity.ok(result);
  }

  @GetMapping("/access-sequential")
  public ResponseEntity<List<String>> accessResourceSequentially(
    @RequestParam(defaultValue = "10") int count,
    @RequestParam(defaultValue = "0") int startPriority) {

    List<String> results = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      int priority = startPriority + i;
      logger.info("顺序发起第 {} 次请求，优先级: {}", i + 1, priority);
      String result = resourceAccessService.accessResource(priority);
      results.add(result);
    }

    return ResponseEntity.ok(results);
  }

  @GetMapping("/access-parallel")
  public ResponseEntity<List<String>> accessResourceInParallel(
    @RequestParam(defaultValue = "10") int count,
    @RequestParam(defaultValue = "0") int startPriority) throws Exception {

    ExecutorService executor = Executors.newFixedThreadPool(count);
    List<CompletableFuture<String>> futures = new ArrayList<>();

    // 并行提交请求
    for (int i = 0; i < count; i++) {
      final int requestNumber = i + 1;
      final int priority = startPriority + i;

      CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
        logger.info("并行发起第 {} 次请求，优先级: {}", requestNumber, priority);
        return resourceAccessService.accessResource(priority);
      }, executor);

      futures.add(future);
    }

    // 等待所有请求完成
    CompletableFuture<Void> allOf = CompletableFuture.allOf(
      futures.toArray(new CompletableFuture[0])
    );

    // 获取所有结果
    allOf.get();

    List<String> results = new ArrayList<>();
    for (CompletableFuture<String> future : futures) {
      results.add(future.get());
    }

    executor.shutdown();
    return ResponseEntity.ok(results);
  }

  @GetMapping("/mixed-priority-test")
  public ResponseEntity<String> mixedPriorityTest() {
    // 测试不同优先级的请求并行提交
    ExecutorService executor = Executors.newFixedThreadPool(5);

    // 低优先级请求
    for (int i = 0; i < 3; i++) {
      final int idx = i;
      CompletableFuture.runAsync(() -> {
        try {
          Thread.sleep(100 * idx);  // 稍微错开时间
          resourceAccessService.accessResource(1);
        } catch (Exception e) {
          logger.error("低优先级请求出错", e);
        }
      }, executor);
    }

    // 等待一秒，然后提交中优先级请求
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    for (int i = 0; i < 3; i++) {
      final int idx = i;
      CompletableFuture.runAsync(() -> {
        try {
          Thread.sleep(100 * idx);
          resourceAccessService.accessResource(5);
        } catch (Exception e) {
          logger.error("中优先级请求出错", e);
        }
      }, executor);
    }

    // 再等待一秒，然后提交高优先级请求
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    for (int i = 0; i < 3; i++) {
      final int idx = i;
      CompletableFuture.runAsync(() -> {
        try {
          Thread.sleep(100 * idx);
          resourceAccessService.accessResource(10);
        } catch (Exception e) {
          logger.error("高优先级请求出错", e);
        }
      }, executor);
    }

    executor.shutdown();
    return ResponseEntity.ok("已启动混合优先级测试，请查看日志了解详情");
  }
} 
