package com.software.logistic.controller;

import com.software.logistic.common.ResponseResult;
import com.software.logistic.entity.User;
import com.software.logistic.service.OperationLogService;
import com.software.logistic.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private OperationLogService operationLogService;

    /**
     * 获取用户个人资料
     * @return 用户个人资料
     */
    @GetMapping("/profile")
    public ResponseResult<?> getProfile() {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);

        // 构建返回结果
        Map<String, Object> profile = new HashMap<>();
        profile.put("username", user.getUsername());
        profile.put("role", user.getRole());
        profile.put("email", user.getEmail());
        profile.put("phone", user.getPhone());

        return ResponseResult.success("成功", profile);
    }

    /**
     * 更新用户个人资料
     * @param updateData 更新数据
     * @return 更新结果
     */
    @PutMapping("/profile")
    public ResponseResult<?> updateProfile(@RequestBody Map<String, String> updateData, HttpServletRequest request) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);

        // 更新用户信息
        if (updateData.containsKey("username")) {
            user.setUsername(updateData.get("username"));
        }
        if (updateData.containsKey("email")) {
            user.setEmail(updateData.get("email"));
        }
        if (updateData.containsKey("phone")) {
            user.setPhone(updateData.get("phone"));
        }

        userService.update(user);
        
        // 记录更新个人资料日志
        String ipAddress = request.getRemoteAddr();
        operationLogService.logOperation(
            user.getId(),
            user.getUsername(),
            user.getRole(),
            "UPDATE_PROFILE",
            "用户更新个人资料",
            ipAddress
        );
        
        return ResponseResult.success("个人资料更新成功");
    }

    /**
     * 修改用户密码
     * @param passwordData 密码数据
     * @return 修改结果
     */
    @PutMapping("/change-password")
    public ResponseResult<?> changePassword(@RequestBody Map<String, String> passwordData, HttpServletRequest request) {
        String oldPassword = passwordData.get("oldPassword");
        String newPassword = passwordData.get("newPassword");

        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);

        // 验证旧密码是否正确
        if (!userService.checkPassword(user, oldPassword)) {
            return ResponseResult.error(1001, "旧密码错误");
        }

        // 更新密码
        userService.updatePassword(user, newPassword);
        
        // 记录修改密码日志
        String ipAddress = request.getRemoteAddr();
        operationLogService.logOperation(
            user.getId(),
            user.getUsername(),
            user.getRole(),
            "CHANGE_PASSWORD",
            "用户修改密码",
            ipAddress
        );
        
        return ResponseResult.success("密码修改成功");
    }
}