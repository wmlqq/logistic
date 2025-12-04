package com.software.logistic.service.impl;

import com.software.logistic.entity.SystemSetting;
import com.software.logistic.repository.SystemSettingRepository;
import com.software.logistic.service.SystemSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 系统设置Service实现类
 */
@Service
public class SystemSettingServiceImpl implements SystemSettingService {

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    /**
     * 获取当前系统设置
     * @return 系统设置对象
     */
    @Override
    public SystemSetting getCurrentSetting() {
        Optional<SystemSetting> optionalSetting = systemSettingRepository.findTopByOrderByUpdateTimeDesc();
        SystemSetting setting;
        if (optionalSetting.isPresent()) {
            setting = optionalSetting.get();
        } else {
            // 如果没有系统设置，创建一个默认的
            setting = new SystemSetting();
            setting.setSystemName("XX物流管理平台");
            setting.setSystemVersion("v1.0.0");
            setting.setMaxLoginAttempts(5);
            setting.setSessionTimeout(30);
            systemSettingRepository.save(setting);
        }
        return setting;
    }

    /**
     * 更新系统设置
     * @param setting 系统设置对象
     * @return 更新后的系统设置对象
     */
    @Override
    public SystemSetting updateSetting(SystemSetting setting) {
        SystemSetting currentSetting = getCurrentSetting();
        
        // 更新系统设置，只更新不为null的字段
        if (setting.getSystemName() != null) {
            currentSetting.setSystemName(setting.getSystemName());
        }
        if (setting.getSystemVersion() != null) {
            currentSetting.setSystemVersion(setting.getSystemVersion());
        }
        if (setting.getContactEmail() != null) {
            currentSetting.setContactEmail(setting.getContactEmail());
        }
        if (setting.getContactPhone() != null) {
            currentSetting.setContactPhone(setting.getContactPhone());
        }
        if (setting.getMaxLoginAttempts() != null) {
            currentSetting.setMaxLoginAttempts(setting.getMaxLoginAttempts());
        }
        if (setting.getSessionTimeout() != null) {
            currentSetting.setSessionTimeout(setting.getSessionTimeout());
        }
        
        return systemSettingRepository.save(currentSetting);
    }
}