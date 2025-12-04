package com.software.logistic.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

/**
 * 系统设置实体类
 */
@Data
@Entity
@Table(name = "system_settings")
public class SystemSetting {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 系统名称
     */
    private String systemName;

    /**
     * 系统版本
     */
    private String systemVersion;

    /**
     * 联系邮箱
     */
    private String contactEmail;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 最大登录尝试次数
     */
    private Integer maxLoginAttempts;

    /**
     * 会话超时时间（分钟）
     */
    private Integer sessionTimeout;

    /**
     * 创建时间
     */
    @Column(updatable = false)
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 自动设置创建时间和更新时间
     */
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