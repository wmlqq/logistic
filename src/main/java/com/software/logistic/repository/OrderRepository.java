package com.software.logistic.repository;

import com.software.logistic.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    /**
     * 根据客户ID查询订单列表
     */
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    /**
     * 根据客户ID和状态查询订单列表
     */
    Page<Order> findByCustomerIdAndStatus(Long customerId, String status, Pageable pageable);

    /**
     * 根据客户ID和订单编号查询订单列表
     */
    @Query("select o from Order o where o.customerId = :customerId and o.orderNumber like %:orderNumber%")
    Page<Order> findByCustomerIdAndOrderNumberContaining(@Param("customerId") Long customerId, @Param("orderNumber") String orderNumber, Pageable pageable);

    /**
     * 根据客户ID统计订单数量
     */
    long countByCustomerId(Long customerId);

    /**
     * 根据客户ID和状态统计订单数量
     */
    long countByCustomerIdAndStatus(Long customerId, String status);

    /**
     * 根据状态统计订单数量
     */
    long countByStatus(String status);

    /**
     * 根据状态查询订单列表
     */
    Page<Order> findByStatus(String status, Pageable pageable);

    /**
     * 根据订单编号或客户名称模糊查询订单列表
     */
    @Query("select o from Order o where o.orderNumber like %:orderNumber% or o.customerName like %:customerName%")
    Page<Order> findByOrderNumberContainingOrCustomerNameContaining(@Param("orderNumber") String orderNumber, @Param("customerName") String customerName, Pageable pageable);
    
    /**
     * 根据配送方式查询订单列表
     */
    Page<Order> findByDeliveryMethod(String deliveryMethod, Pageable pageable);
}