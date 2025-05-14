package com.example.resourcescheduler.service;

public interface ResourceAccessService {
  /**
   * 访问资源的方法，该方法有访问频率和并发限制
   *
   * @param priority 请求优先级，值越大优先级越高
   * @return 资源访问结果
   */
  String accessResource(int priority);
} 
