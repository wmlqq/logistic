package com.software.logistic.controller;

import com.software.logistic.common.ResponseResult;
import com.software.logistic.dto.LoginRequest;
import com.software.logistic.dto.RegisterRequest;
import com.software.logistic.entity.User;
import com.software.logistic.repository.UserRepository;
import com.software.logistic.service.AuthService;
import com.software.logistic.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private AuthService authService;
    
    @Autowired
    private OperationLogService operationLogService;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * 用户登录
     * @param loginRequest 登录请求
     * @return 登录结果
     */
    @PostMapping("/login")
    public ResponseResult<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        Map<String, Object> result = authService.login(loginRequest);
        
        // 记录登录日志
        String username = (String) result.get("username"); // 使用真实的用户名
        String role = (String) result.get("role"); // 使用真实的角色
        String ipAddress = request.getRemoteAddr();
        operationLogService.logOperation(
            (Long) result.get("userId"),
            username,
            role,
            "LOGIN",
            "用户登录系统",
            ipAddress
        );
        
        return ResponseResult.success("登录成功", result);
    }

    /**
     * 用户注册
     * @param registerRequest 注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public ResponseResult<?> register(@RequestBody RegisterRequest registerRequest, HttpServletRequest request) {
        User user = authService.register(registerRequest);
        
        // 记录注册日志
        String ipAddress = request.getRemoteAddr();
        operationLogService.logOperation(
            user.getId(),
            user.getUsername(),
            user.getRole(),
            "REGISTER",
            "用户注册系统",
            ipAddress
        );
        
        return ResponseResult.success("注册成功", user);
    }

    /**
     * 用户退出
     * @return 退出结果
     */
    @PostMapping("/logout")
    public ResponseResult<?> logout(HttpServletRequest request) {
        authService.logout();
        
        // 记录退出日志
        String ipAddress = request.getRemoteAddr();
        
        // 从SecurityContext获取用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        // 只有非匿名用户才记录日志
        if (!"anonymousUser".equals(username)) {
            // 查询用户信息
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                operationLogService.logOperation(
                    user.getId(),
                    user.getUsername(),
                    user.getRole(),
                    "LOGOUT",
                    "用户退出系统",
                    ipAddress
                );
            }
        }
        
        return ResponseResult.success("退出成功");
    }
}