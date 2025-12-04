package com.software.logistic.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity
@Table(name = "t_product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "product_code", nullable = false, unique = true, length = 50)
    private String productCode;

    private String specification;

    private String unit;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    private Integer stock;

    @Column(name = "alert_threshold")
    private Integer alertThreshold;



    @Column(nullable = false, columnDefinition = "int default 1")
    private Integer status;

    @Column(name = "create_time", nullable = false, updatable = false)
    private Date createTime;

    @Column(name = "update_time", nullable = false)
    private Date updateTime;

    private String remark;

    // 与Location的多对一关联
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @PrePersist
    protected void onCreate() {
        createTime = new Date();
        updateTime = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
}