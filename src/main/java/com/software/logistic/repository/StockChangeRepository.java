package com.software.logistic.repository;

import com.software.logistic.entity.StockChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockChangeRepository extends JpaRepository<StockChange, Long> {
    // 可以根据需要添加更多查询方法
}
