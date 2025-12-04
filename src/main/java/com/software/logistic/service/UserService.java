package com.software.logistic.service;

import com.software.logistic.entity.User;

public interface UserService {
    /**
     * 根据用户名查询用户
     */
    User findByUsername(String username);

    /**
     * 更新用户信息
     */
    void update(User user);

    /**
     * 检查密码是否正确
     */
    boolean checkPassword(User user, String rawPassword);

    /**
     * 更新用户密码
     */
    void updatePassword(User user, String newPassword);
}