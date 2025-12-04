package com.software.logistic.service.impl;

import com.software.logistic.dto.LoginRequest;
import com.software.logistic.dto.RegisterRequest;
import com.software.logistic.entity.User;
import com.software.logistic.exception.BusinessException;
import com.software.logistic.repository.UserRepository;
import com.software.logistic.service.AuthService;
import com.software.logistic.utils.JwtUtil;
import com.software.logistic.utils.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Map<String, Object> login(LoginRequest loginRequest) {
        String role = loginRequest.getRole();
        String account = loginRequest.getAccount();
        String password = loginRequest.getPassword();

        // 根据账号查询用户
        User user = null;
        if (account.contains("@")) {
            // 邮箱登录
            Optional<User> userOptional = userRepository.findByEmail(account);
            if (userOptional.isPresent()) {
                user = userOptional.get();
            }
        } else if (account.matches("^1[3-9]\\d{9}$")) {
            // 手机号登录
            Optional<User> userOptional = userRepository.findByPhone(account);
            if (userOptional.isPresent()) {
                user = userOptional.get();
            }
        } else {
            // 用户名登录
            Optional<User> userOptional = userRepository.findByUsername(account);
            if (userOptional.isPresent()) {
                user = userOptional.get();
            }
        }

        // 验证用户是否存在
        if (user == null) {
            throw new BusinessException(1003, "账号不存在");
        }

        // 验证用户角色是否匹配
        if (!user.getRole().equals(role)) {
            throw new BusinessException(1004, "角色不匹配");
        }

        // 验证密码是否正确
        if (!PasswordUtil.matches(password, user.getPassword())) {
            throw new BusinessException(1001, "账号或密码错误");
        }

        // 验证用户是否被禁用
        if (user.getStatus() == 0) {
            throw new BusinessException(1002, "账号已被禁用");
        }

        // 生成JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 更新用户最后登录时间
        user.setLastLoginTime(new java.util.Date());
        userRepository.save(user);

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        result.put("token", token);

        return result;
    }

    @Override
    public User register(RegisterRequest registerRequest) {
        String role = registerRequest.getRole();
        String username = registerRequest.getUsername();
        String email = registerRequest.getEmail();
        String phone = registerRequest.getPhone();
        String password = registerRequest.getPassword();

        // 验证系统管理员不可注册
        if ("admin".equals(role)) {
            throw new BusinessException(2009, "系统管理员不可注册");
        }

        // 验证用户名是否已存在
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(2001, "用户名已存在");
        }

        // 验证邮箱和手机号至少填一项
        if ((email == null || email.isEmpty()) && (phone == null || phone.isEmpty())) {
            throw new BusinessException(2004, "邮箱和手机号至少填一项");
        }

        // 验证邮箱是否已被注册
        if (email != null && !email.isEmpty()) {
            if (userRepository.existsByEmail(email)) {
                throw new BusinessException(2002, "邮箱已被注册");
            }
            // 验证邮箱格式
            if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
                throw new BusinessException(2007, "无效的邮箱格式");
            }
        }

        // 验证手机号是否已被注册
        if (phone != null && !phone.isEmpty()) {
            if (userRepository.existsByPhone(phone)) {
                throw new BusinessException(2003, "手机号已被注册");
            }
            // 验证手机号格式
            if (!phone.matches("^1[3-9]\\d{9}$")) {
                throw new BusinessException(2008, "无效的手机号格式");
            }
        }

        // 验证用户名长度
        if (username.length() < 4) {
            throw new BusinessException(2005, "用户名长度不能少于 4 个字符");
        }

        // 验证密码长度
        if (password.length() < 8) {
            throw new BusinessException(2006, "密码长度不能少于 8 个字符");
        }

        // 加密密码
        String encryptedPassword = PasswordUtil.encryptPassword(password);

        // 创建用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(encryptedPassword);
        user.setRole(role);
        // 将空字符串转换为null，避免唯一约束冲突
        user.setEmail(email != null && !email.isEmpty() ? email : null);
        user.setPhone(phone != null && !phone.isEmpty() ? phone : null);
        user.setStatus(1);

        return userRepository.save(user);
    }

    @Override
    public void logout() {
        // JWT是无状态的，退出登录主要是客户端的操作（删除token）
        // 这里可以添加一些清理工作，比如记录退出日志等
        // 由于当前系统没有实现token黑名单机制，所以暂时只返回成功
    }
}