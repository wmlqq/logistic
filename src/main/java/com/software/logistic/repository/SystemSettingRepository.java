package com.software.logistic.repository;

import com.software.logistic.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 系统设置Repository接口
 */
@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {
    
    /**
     * 获取最新的系统设置
     * @return 系统设置对象
     */
    Optional<SystemSetting> findTopByOrderByUpdateTimeDesc();
}