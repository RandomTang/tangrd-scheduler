package com.tangrd.resourcescheduler.service;

public interface ResourceAccessService {
  /**
   * Method to access resources with rate limiting and concurrency control
   *
   * @param priority Request priority, higher value means higher priority
   * @return Resource access result
   */
  String accessResource(int priority);

} 
