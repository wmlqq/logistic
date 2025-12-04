package com.software.logistic.service;

import com.software.logistic.dto.LoginRequest;
import com.software.logistic.dto.RegisterRequest;
import com.software.logistic.entity.User;

import java.util.Map;

public interface AuthService {
    /**
     * 用户登录
     * @param loginRequest 登录请求
     * @return 登录结果
     */
    Map<String, Object> login(LoginRequest loginRequest);

    /**
     * 用户注册
     * @param registerRequest 注册请求
     * @return 注册结果
     */
    User register(RegisterRequest registerRequest);

    /**
     * 用户退出
     */
    void logout();
}