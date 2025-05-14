package com.example.resourcescheduler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ResourceAccessServiceImpl implements ResourceAccessService {
  private static final Logger logger = LoggerFactory.getLogger(ResourceAccessServiceImpl.class);

  // 使用信号量限制并发访问数量为1
  private final Semaphore semaphore = new Semaphore(1);
  // 使用锁来保证对lastAccessTime的读写操作是线程安全的
  private final Lock accessTimeLock = new ReentrantLock();
  // 优先级队列，存储未处理的请求
  private final PriorityQueue<PriorityRequest> requestQueue = new PriorityQueue<>(
    Comparator.comparingInt(PriorityRequest::priority).reversed()
  );
  // 用于记录最后一次访问时间
  private LocalDateTime lastAccessTime = null;
  // 资源访问任务是否正在处理中
  private volatile boolean isProcessing = false;

  @Override
  public String accessResource(int priority) {
    logger.info("收到访问请求，优先级: {}", priority);

    try {
      // 将请求添加到优先级队列
      PriorityRequest request = new PriorityRequest(priority);

      synchronized (requestQueue) {
        requestQueue.offer(request);

        // 如果当前没有正在处理的任务，开始处理队列
        if (!isProcessing) {
          isProcessing = true;
          processQueue();
        }
      }

      // 等待结果
      return request.future.get();
    } catch (Exception e) {
      logger.error("访问资源时发生错误", e);
      return "访问失败：" + e.getMessage();
    }
  }

  private void processQueue() {
    CompletableFuture.runAsync(() -> {
      try {
        while (true) {
          PriorityRequest request;
          synchronized (requestQueue) {
            request = requestQueue.poll();
            if (request == null) {
              isProcessing = false;
              break;
            }
          }

          // 尝试获取资源锁
          processRequest(request);
        }
      } catch (Exception e) {
        logger.error("处理队列时发生错误", e);
        synchronized (requestQueue) {
          isProcessing = false;
        }
      }
    });
  }

  private void processRequest(PriorityRequest request) {
    CompletableFuture.runAsync(() -> {
      try {
        // 等待两分钟的冷却时间
        waitForCooldown();

        // 获取信号量，确保同一时间只有一个线程访问资源
        logger.info("尝试获取访问权限，优先级: {}", request.priority);
        semaphore.acquire();

        try {
          // 设置最后访问时间
          accessTimeLock.lock();
          try {
            lastAccessTime = LocalDateTime.now();
          } finally {
            accessTimeLock.unlock();
          }

          // 执行资源访问操作
          String result = simulateResourceAccess(request.priority);
          request.future.complete(result);
        } finally {
          // 释放信号量
          semaphore.release();
        }
      } catch (Exception e) {
        logger.error("处理请求失败", e);
        request.future.completeExceptionally(e);
      }
    });
  }

  private void waitForCooldown() throws InterruptedException {
    while (true) {
      accessTimeLock.lock();
      try {
        if (lastAccessTime == null) {
          // 首次访问，无需等待
          return;
        }

        LocalDateTime now = LocalDateTime.now();
        long secondsSinceLastAccess = ChronoUnit.SECONDS.between(lastAccessTime, now);

        if (secondsSinceLastAccess >= 120) { // 两分钟 = 120秒
          // 已经过了冷却时间，可以继续
          return;
        }

        // 还需要等待的时间（秒）
        long secondsToWait = 120 - secondsSinceLastAccess;
        logger.info("需要等待 {} 秒后才能再次访问资源", secondsToWait);
      } finally {
        accessTimeLock.unlock();
      }

      // 等待一段时间后再检查
      TimeUnit.SECONDS.sleep(Math.min(10, 120));
    }
  }

  private String simulateResourceAccess(int priority) {
    try {
      logger.info("开始访问资源，优先级: {}", priority);

      // 模拟资源访问耗时
      int processingTime = 5 + (int) (Math.random() * 10);
      TimeUnit.SECONDS.sleep(processingTime);

      String result = String.format("资源访问成功！优先级: %d, 处理时间: %d秒, 访问时间: %s",
        priority, processingTime, LocalDateTime.now());
      logger.info(result);

      return result;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return "资源访问被中断";
    }
  }

  // 内部类，表示一个带优先级的请求
  private static class PriorityRequest {
    private final int priority;
    private final CompletableFuture<String> future;

    public PriorityRequest(int priority) {
      this.priority = priority;
      this.future = new CompletableFuture<>();
    }

    public int priority() {
      return priority;
    }
  }
} 
