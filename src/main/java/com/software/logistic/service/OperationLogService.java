package com.software.logistic.service;

import com.software.logistic.entity.OperationLog;

public interface OperationLogService {
    /**
     * 记录操作日志
     * @param userId 用户ID
     * @param username 用户名
     * @param role 角色
     * @param operationType 操作类型
     * @param operationContent 操作内容
     * @param ipAddress IP地址
     */
    void logOperation(Long userId, String username, String role, String operationType, String operationContent, String ipAddress);
}