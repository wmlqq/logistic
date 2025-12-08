package com.software.logistic.repository;

import com.software.logistic.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    /**
     * 根据费用类型统计费用金额
     */
    @Query("select sum(e.amount) from Expense e where e.expenseType = :expenseType")
    BigDecimal sumByExpenseType(@Param("expenseType") String expenseType);

    /**
     * 获取所有费用类型
     */
    @Query("select distinct e.expenseType from Expense e")
    List<String> findDistinctExpenseType();
    
    /**
     * 根据创建时间范围查询费用
     */
    List<Expense> findByCreateTimeBetween(Date startDate, Date endDate);
}