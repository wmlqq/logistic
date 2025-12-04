package com.software.logistic.service;

import com.software.logistic.entity.SystemSetting;

/**
 * 系统设置Service接口
 */
public interface SystemSettingService {
    
    /**
     * 获取当前系统设置
     * @return 系统设置对象
     */
    SystemSetting getCurrentSetting();
    
    /**
     * 更新系统设置
     * @param setting 系统设置对象
     * @return 更新后的系统设置对象
     */
    SystemSetting updateSetting(SystemSetting setting);
}