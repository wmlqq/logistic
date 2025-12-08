package com.software.logistic.repository;

import com.software.logistic.entity.DeliveryTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface DeliveryTaskRepository extends JpaRepository<DeliveryTask, Long> {
    /**
     * 根据状态查询配送任务列表
     */
    Page<DeliveryTask> findByStatus(String status, Pageable pageable);

    /**
     * 根据配送员ID和状态查询配送任务列表
     */
    List<DeliveryTask> findByDeliveryManIdAndStatus(Long deliveryManId, String status);

    /**
     * 根据配送员ID和状态统计配送任务数量
     */
    long countByDeliveryManIdAndStatus(Long deliveryManId, String status);

    /**
     * 根据配送员ID和状态统计指定时间后的配送任务数量
     */
    long countByDeliveryManIdAndStatusAndCreateTimeAfter(Long deliveryManId, String status, Date createTime);

    /**
     * 根据配送员ID查询配送任务列表
     */
    List<DeliveryTask> findByDeliveryManId(Long deliveryManId, Sort sort);
    
    /**
     * 根据配送员ID查询配送任务列表（分页）
     */
    Page<DeliveryTask> findByDeliveryManId(Long deliveryManId, Pageable pageable);
    
    /**
     * 根据配送员ID和状态查询配送任务列表（分页）
     */
    Page<DeliveryTask> findByDeliveryManIdAndStatus(Long deliveryManId, String status, Pageable pageable);
    
    /**
     * 根据创建时间范围查询配送任务列表
     */
    List<DeliveryTask> findByCreateTimeBetween(Date startDate, Date endDate);
}