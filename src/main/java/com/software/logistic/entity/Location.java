package com.software.logistic.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "t_location")
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "location_code", nullable = false, unique = true, length = 50)
    private String locationCode;

    private String description;

    private Integer capacity;

    private Integer used;

    @Column(nullable = false, columnDefinition = "int default 1")
    private Integer status;

    @Column(name = "create_time", nullable = false, updatable = false)
    private Date createTime;

    @Column(name = "update_time", nullable = false)
    private Date updateTime;

    private String remark;

    // 与Product的一对多关联
    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Product> products;

    @PrePersist
    protected void onCreate() {
        createTime = new Date();
        updateTime = new Date();
        used = 0; // 初始使用量为0
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
}