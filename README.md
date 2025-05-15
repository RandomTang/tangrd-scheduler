# Resource Scheduler

This is a Spring Boot application that implements a resource access scheduler with the following capabilities:

1. Limit resource access frequency - maximum one access every two minutes
2. Prevent concurrent access - only allow one request to access the resource at the same time
3. Support priority-based scheduling - high priority requests are processed first

## Technology Stack

- Java 21
- Spring Boot 3.2.0
- Gradle

## Key Features

- Resource access frequency limitation: Maximum one access every two minutes
- Concurrent access control: Using semaphore to ensure only one thread accesses the resource at a time
- Priority queue: High priority requests are processed first
- Provides REST API interfaces for testing

## How to Run

1. Make sure you have JDK 21 and Gradle installed
2. Use the following command to start the application:

```bash
gradle bootRun
```

3. The service will start at http://localhost:8080

## API Endpoints

### 1. Single Resource Access

```
GET /api/resource/access?priority={priority}
```

- `priority`: Request priority, higher number means higher priority, default is 0

### 2. Sequential Multiple Access

```
GET /api/resource/access-sequential?count={requestCount}&startPriority={startingPriority}
```

- `count`: Number of requests, default is 10
- `startPriority`: Starting priority, each subsequent request increases priority by 1, default is 0

### 3. Parallel Multiple Access

```
GET /api/resource/access-parallel?count={requestCount}&startPriority={startingPriority}
```

- `count`: Number of requests, default is 10
- `startPriority`: Starting priority, each subsequent request increases priority by 1, default is 0

### 4. Mixed Priority Test

```
GET /api/resource/mixed-priority-test
```

This endpoint submits requests with different priorities in parallel to test the priority scheduling functionality.

## Examples

1. Make single access with priority 5:

```
curl http://localhost:8080/api/resource/access?priority=5
```

2. Sequentially make 3 accesses starting with priority 1:

```
curl http://localhost:8080/api/resource/access-sequential?count=3&startPriority=1
```

3. Parallel make 5 accesses:

```
curl http://localhost:8080/api/resource/access-parallel?count=5
```

4. Test priority queue functionality:

```
curl http://localhost:8080/api/resource/mixed-priority-test
``` 
