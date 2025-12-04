package com.software.logistic.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "t_stock_change")
public class StockChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "change_type", nullable = false, length = 20)
    private String changeType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "before_stock", nullable = false)
    private Integer beforeStock;

    @Column(name = "after_stock", nullable = false)
    private Integer afterStock;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "operator_name", nullable = false, length = 50)
    private String operatorName;

    @Column(name = "change_time", nullable = false, updatable = false)
    private Date changeTime;

    private String remark;

    @PrePersist
    protected void onCreate() {
        changeTime = new Date();
    }
}