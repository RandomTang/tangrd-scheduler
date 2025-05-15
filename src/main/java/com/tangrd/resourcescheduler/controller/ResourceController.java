package com.tangrd.resourcescheduler.controller;

import com.tangrd.resourcescheduler.service.ResourceAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resource")
public class ResourceController {

  private final ResourceAccessService resourceAccessService;

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
      log.info("Processing request {} with priority: {}", i + 1, priority);
      String result = resourceAccessService.accessResource(priority);
      log.info("Resource access completed: {}", result);
      results.add(result);
    }

    log.info("Sequential access results: {}", results);
    return ResponseEntity.ok(results);
  }

  @GetMapping("/access-parallel")
  public ResponseEntity<List<String>> accessResourceInParallel(
    @RequestParam(defaultValue = "10") int count,
    @RequestParam(defaultValue = "0") int startPriority) throws Exception {

    ExecutorService executor = Executors.newFixedThreadPool(count);
    List<CompletableFuture<String>> futures = new ArrayList<>();

    // Submit requests in parallel
    for (int i = 0; i < count; i++) {
      final int requestNumber = i + 1;
      final int priority = startPriority + i;

      CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
        log.info("Parallel processing started for {} with priority {}", requestNumber, priority);
        return resourceAccessService.accessResource(priority);
      }, executor);

      futures.add(future);
    }

    // Wait for all requests to complete
    CompletableFuture<Void> allOf = CompletableFuture.allOf(
      futures.toArray(new CompletableFuture[0])
    );

    // Get all results
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
    // Test submitting requests with different priorities in parallel
    ExecutorService executor = Executors.newFixedThreadPool(5);

    // Low priority requests
    for (int i = 0; i < 3; i++) {
      final int idx = i;
      CompletableFuture.runAsync(() -> {
        try {
          Thread.sleep(100 * idx);  // Slightly staggered timing
          resourceAccessService.accessResource(1);
        } catch (Exception e) {
          log.error("Error in low priority request", e);
        }
      }, executor);
    }

    // Wait for one second, then submit medium priority requests
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Medium priority requests
    for (int i = 0; i < 3; i++) {
      final int idx = i;
      CompletableFuture.runAsync(() -> {
        try {
          Thread.sleep(100 * idx);
          resourceAccessService.accessResource(5);
        } catch (Exception e) {
          log.error("Error in medium priority request", e);
        }
      }, executor);
    }

    // Wait another second, then submit high priority requests
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
          log.error("Error in high priority request", e);
        }
      }, executor);
    }

    executor.shutdown();
    return ResponseEntity.ok("This is a mixed priority test, please check the logs for details");
  }
} 
