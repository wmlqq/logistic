package com.software.logistic.repository;

import com.software.logistic.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    /**
     * 查询库存小于预警阈值的商品
     */
    @Query("select p from Product p where p.stock < p.alertThreshold")
    List<Product> findByStockLessThanAlertThreshold();

    /**
     * 查询严重预警商品（库存为0）
     */
    @Query("select p from Product p where p.stock = 0")
    List<Product> findByStockZero();

    /**
     * 统计库存小于预警阈值的商品数量
     */
    @Query("select count(p) from Product p where p.stock < p.alertThreshold")
    long countByStockLessThanAlertThreshold();

    /**
     * 统计严重预警商品数量（库存为0）
     */
    @Query("select count(p) from Product p where p.stock = 0")
    long countByStockZero();

    /**
     * 根据商品名称或商品代码模糊查询商品列表
     */
    @Query("select p from Product p where p.productName like %:productName% or p.productCode like %:productCode%")
    Page<Product> findByProductNameContainingOrProductCodeContaining(@Param("productName") String productName, @Param("productCode") String productCode, Pageable pageable);
    
    /**
     * 根据商品编码查询商品
     */
    Product findByProductCode(String productCode);
    
    /**
     * 根据状态查询商品
     */
    List<Product> findByStatus(Integer status);
    
    /**
     * 根据状态统计商品数量
     */
    long countByStatus(Integer status);
    
    /**
     * 根据仓库ID查询商品
     */
    List<Product> findByLocationId(Long locationId);
    
    /**
     * 根据仓库ID查询库存小于预警阈值的商品
     */
    @Query("select p from Product p where p.location.id = :locationId and p.stock < p.alertThreshold")
    List<Product> findByLocationIdAndStockLessThanAlertThreshold(@Param("locationId") Long locationId);
}