# 资源调度器

该项目是一个Spring Boot应用程序，实现了一个资源访问调度器，它能够：

1. 限制资源访问频率 - 每两分钟最多访问一次
2. 防止并发访问 - 同一时间只允许一个请求访问资源
3. 支持优先级调度 - 高优先级请求会优先处理

## 技术栈

- Java 21
- Spring Boot 3.2.0
- Gradle

## 主要特性

- 资源访问频率限制：每两分钟最多访问一次
- 并发访问控制：使用信号量确保同一时间只有一个线程访问资源
- 优先级队列：高优先级请求会优先得到处理
- 提供了REST API接口进行测试

## 如何运行

1. 确保您安装了JDK 21和Gradle
2. 使用以下命令启动应用程序：

```bash
gradle bootRun
```

3. 服务将在 http://localhost:8080 上启动

## API 接口

### 1. 单次资源访问

```
GET /api/resource/access?priority={优先级}
```

- `priority`: 请求优先级，数字越大优先级越高，默认为0

### 2. 顺序多次访问

```
GET /api/resource/access-sequential?count={请求次数}&startPriority={起始优先级}
```

- `count`: 请求次数，默认为10
- `startPriority`: 起始优先级，每个后续请求优先级+1，默认为0

### 3. 并行多次访问

```
GET /api/resource/access-parallel?count={请求次数}&startPriority={起始优先级}
```

- `count`: 请求次数，默认为10
- `startPriority`: 起始优先级，每个后续请求优先级+1，默认为0

### 4. 混合优先级测试

```
GET /api/resource/mixed-priority-test
```

该接口会并行发起不同优先级的请求，测试优先级调度功能。

## 示例

1. 发起一个优先级为5的单次访问：

```
curl http://localhost:8080/api/resource/access?priority=5
```

2. 顺序发起3次访问，优先级从1开始：

```
curl http://localhost:8080/api/resource/access-sequential?count=3&startPriority=1
```

3. 并行发起5次访问：

```
curl http://localhost:8080/api/resource/access-parallel?count=5
```

4. 测试优先级队列功能：

```
curl http://localhost:8080/api/resource/mixed-priority-test
``` 