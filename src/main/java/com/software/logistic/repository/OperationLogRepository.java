package com.software.logistic.repository;

import com.software.logistic.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    /**
     * 根据用户名查询操作日志
     */
    Page<OperationLog> findByUsername(String username, Pageable pageable);

    /**
     * 根据用户名和日期范围查询操作日志
     */
    Page<OperationLog> findByUsernameAndCreateTimeBetween(String username, java.util.Date startDate, java.util.Date endDate, Pageable pageable);
    
    /**
     * 根据用户名模糊查询操作日志
     */
    Page<OperationLog> findByUsernameContaining(String username, Pageable pageable);
    
    /**
     * 根据操作类型查询操作日志
     */
    Page<OperationLog> findByOperationType(String operationType, Pageable pageable);
    
    /**
     * 根据操作类型模糊查询操作日志
     */
    Page<OperationLog> findByOperationTypeContaining(String operationType, Pageable pageable);
    
    /**
     * 根据用户名和操作类型查询操作日志
     */
    Page<OperationLog> findByUsernameContainingAndOperationType(String username, String operationType, Pageable pageable);
    
    /**
     * 根据用户名和操作类型模糊查询操作日志
     */
    Page<OperationLog> findByUsernameContainingAndOperationTypeContaining(String username, String operationType, Pageable pageable);
    
    /**
     * 根据用户名、操作类型和日期范围查询操作日志
     */
    Page<OperationLog> findByUsernameContainingAndOperationTypeAndCreateTimeBetween(
            String username, String operationType, java.util.Date startDate, java.util.Date endDate, Pageable pageable);
    
    /**
     * 根据用户名、操作类型模糊和日期范围查询操作日志
     */
    Page<OperationLog> findByUsernameContainingAndOperationTypeContainingAndCreateTimeBetween(
            String username, String operationType, java.util.Date startDate, java.util.Date endDate, Pageable pageable);
    
    /**
     * 根据操作类型和日期范围查询操作日志
     */
    Page<OperationLog> findByOperationTypeAndCreateTimeBetween(
            String operationType, java.util.Date startDate, java.util.Date endDate, Pageable pageable);
    
    /**
     * 根据操作类型模糊和日期范围查询操作日志
     */
    Page<OperationLog> findByOperationTypeContainingAndCreateTimeBetween(
            String operationType, java.util.Date startDate, java.util.Date endDate, Pageable pageable);
    
    /**
     * 根据日期范围查询操作日志
     */
    Page<OperationLog> findByCreateTimeBetween(java.util.Date startDate, java.util.Date endDate, Pageable pageable);
    
    /**
     * 根据创建时间之后统计操作日志数量
     */
    long countByCreateTimeAfter(java.util.Date createTime);
    
    /**
     * 根据操作类型和创建时间之后统计操作日志数量
     */
    long countByOperationTypeAndCreateTimeAfter(String operationType, java.util.Date createTime);
}