package com.tangrd.resourcescheduler.service;

import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
public class ResourceAccessServiceImpl implements ResourceAccessService {

  // Using semaphore to limit concurrent access to 1
  private final Semaphore semaphore = new Semaphore(1);
  // Using lock to ensure thread safety for lastAccessTime read/write operations
  private final Lock accessTimeLock = new ReentrantLock();
  // Priority queue to store unprocessed requests
  private final PriorityQueue<PriorityRequest> requestQueue = new PriorityQueue<>(
    Comparator.comparingInt(PriorityRequest::priority).reversed()
  );
  // Records the last access time
  private LocalDateTime lastAccessTime = null;
  // Whether a resource access task is being processed
  private volatile boolean isProcessing = false;
  private final int TIME_LIMITATION = 120; // 2 minutes = 120 seconds


  @Override
  public String accessResource(int priority) {
    log.info("Received access request, priority: {}", priority);

    try {
      // Add request to priority queue
      PriorityRequest request = new PriorityRequest(priority, new CompletableFuture<>());

      synchronized (requestQueue) {
        requestQueue.offer(request);

        // If no task is currently processing, start processing the queue
        if (!isProcessing) {
          isProcessing = true;
          processQueue();
        }
      }

      // Wait for result
      return request.future.get();
    } catch (Exception e) {
      log.error("Error occurred while accessing resource", e);
      return "Access failed: " + e.getMessage();
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

          // Attempt to acquire resource lock
          processRequest(request);
        }
      } catch (Exception e) {
        log.error("Error occurred while processing queue", e);
        synchronized (requestQueue) {
          isProcessing = false;
        }
      }
    });
  }

  private void processRequest(PriorityRequest request) {
    CompletableFuture.runAsync(() -> {
      try {
        // Wait for two-minute cooldown period
        waitForCooldown();

        // Acquire semaphore to ensure only one thread accesses the resource at a time
        log.info("Attempting to acquire access permission, priority: {}", request.priority);
        semaphore.acquire();

        try {
          // Set last access time
          accessTimeLock.lock();
          try {
            lastAccessTime = LocalDateTime.now();
          } finally {
            accessTimeLock.unlock();
          }

          // Execute resource access operation
          String result = simulateResourceAccess(request.priority);
          request.future.complete(result);
        } finally {
          // Release semaphore
          semaphore.release();
        }
      } catch (Exception e) {
        log.error("Error processing request", e);
        request.future.completeExceptionally(e);
      }
    });
  }

  private void waitForCooldown() throws InterruptedException {
    while (true) {
      accessTimeLock.lock();
      try {
        if (lastAccessTime == null) {
          // First access, no need to wait
          return;
        }

        LocalDateTime now = LocalDateTime.now();
        long secondsSinceLastAccess = ChronoUnit.SECONDS.between(lastAccessTime, now);

        if (secondsSinceLastAccess >= TIME_LIMITATION) {
          // Cooling down completed, can continue
          return;
        }

        // Time remaining to wait (in seconds)
        long secondsToWait = TIME_LIMITATION - secondsSinceLastAccess;
        log.info("Resource access cooldown waiting for {} seconds", secondsToWait);
      } finally {
        accessTimeLock.unlock();
      }

      // Wait for a while before checking again
      TimeUnit.SECONDS.sleep(Math.min(10, TIME_LIMITATION));
    }
  }

  private String simulateResourceAccess(int priority) {
    try {
      log.info("Simulating resource access with priority: {}", priority);

      // Simulate resource access duration
      int processingTime = 5 + (int) (Math.random() * 10);
      TimeUnit.SECONDS.sleep(processingTime);

      String result = String.format("Resource access successful! Priority: %d, Processing time: %d seconds, Access time: %s",
        priority, processingTime, LocalDateTime.now());
      log.info("Resource access completed: {}", result);

      return result;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return "Resource access interrupted";
    }
  }

  private record PriorityRequest(int priority, CompletableFuture<String> future) {
  }

}
