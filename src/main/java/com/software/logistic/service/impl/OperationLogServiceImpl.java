package com.software.logistic.service.impl;

import com.software.logistic.entity.OperationLog;
import com.software.logistic.repository.OperationLogRepository;
import com.software.logistic.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Override
    public void logOperation(Long userId, String username, String role, String operationType, String operationContent, String ipAddress) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setRole(role);
        log.setOperationType(operationType);
        log.setOperationContent(operationContent);
        log.setIpAddress(ipAddress);
        operationLogRepository.save(log);
    }
}