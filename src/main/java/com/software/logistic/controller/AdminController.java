package com.software.logistic.controller;

import com.software.logistic.common.ResponseResult;
import com.software.logistic.entity.OperationLog;
import com.software.logistic.entity.User;
import com.software.logistic.repository.OperationLogRepository;
import com.software.logistic.entity.SystemSetting;
import com.software.logistic.repository.UserRepository;
import com.software.logistic.service.SystemSettingService;
import com.software.logistic.utils.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Autowired
    private SystemSettingService systemSettingService;

    /**
     * 系统管理员仪表盘统计
     * @return 统计数据
     */
    @GetMapping("/stats")
    public ResponseResult<?> getStats() {
        // 统计总用户数
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(1);
        
        // 统计角色数量
        long totalRoles = userRepository.findAll().stream()
                .map(User::getRole)
                .distinct()
                .count();

        // 统计今日登录次数
        // 从操作日志中统计今日登录次数
        Date today = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startOfDay = calendar.getTime();
        
        long todayLogins = operationLogRepository.countByOperationTypeAndCreateTimeAfter("LOGIN", startOfDay);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("totalRoles", totalRoles);
        stats.put("todayLogins", todayLogins);

        return ResponseResult.success("成功", stats);
    }

    /**
     * 系统管理员系统状态
     * @return 系统状态
     */
    @GetMapping("/system/status")
    public ResponseResult<?> getSystemStatus() {
        // 获取系统设置
        SystemSetting setting = systemSettingService.getCurrentSetting();
        
        // 系统状态数据
        Map<String, Object> status = new HashMap<>();
        status.put("systemVersion", setting.getSystemVersion());
        status.put("systemName", setting.getSystemName());
        status.put("contactEmail", setting.getContactEmail());
        status.put("contactPhone", setting.getContactPhone());
        status.put("maxLoginAttempts", setting.getMaxLoginAttempts());
        status.put("sessionTimeout", setting.getSessionTimeout());
        
        // 计算运行时间
        Date createTime = setting.getCreateTime();
        Date currentTime = new Date();
        long diff = currentTime.getTime() - createTime.getTime();
        long days = diff / (1000 * 60 * 60 * 24);
        long hours = (diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (diff % (1000 * 60 * 60)) / (1000 * 60);
        String uptime = days + "天 " + hours + "小时 " + minutes + "分钟";
        status.put("uptime", uptime);
        
        // 获取服务器IP
        try {
            String serverIp = java.net.InetAddress.getLocalHost().getHostAddress();
            status.put("serverIp", serverIp);
        } catch (Exception e) {
            status.put("serverIp", "127.0.0.1");
        }
        
        // 获取CPU使用率
        try {
            com.sun.management.OperatingSystemMXBean osBean = (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            double cpuUsage = osBean.getSystemCpuLoad() * 100;
            status.put("cpuUsage", String.format("%.1f%%", cpuUsage));
        } catch (Exception e) {
            status.put("cpuUsage", "0%");
        }
        
        // 获取内存使用率
        try {
            java.lang.management.MemoryMXBean memoryBean = java.lang.management.ManagementFactory.getMemoryMXBean();
            long totalMemory = Runtime.getRuntime().totalMemory();
            long freeMemory = Runtime.getRuntime().freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double memoryUsage = (double) usedMemory / totalMemory * 100;
            status.put("memoryUsage", String.format("%.1f%%", memoryUsage));
        } catch (Exception e) {
            status.put("memoryUsage", "0%");
        }
        
        // 获取磁盘使用率
        try {
            File root = new File("/");
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            double diskUsage = (double) usedSpace / totalSpace * 100;
            status.put("diskUsage", String.format("%.1f%%", diskUsage));
        } catch (Exception e) {
            status.put("diskUsage", "0%");
        }

        return ResponseResult.success("成功", status);
    }

    /**
     * 系统管理员最近登录记录
     * @return 最近登录记录
     */
    @GetMapping("/logins/recent")
    public ResponseResult<?> getRecentLogins() {
        // 从数据库中查询最近登录记录
        List<OperationLog> logs = operationLogRepository.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createTime"))).getContent();
        
        // 转换为响应格式
        List<Map<String, Object>> logins = logs.stream().map(log -> {
            Map<String, Object> login = new HashMap<>();
            login.put("username", log.getUsername());
            login.put("role", log.getRole());
            login.put("loginTime", log.getCreateTime());
            login.put("loginIp", log.getIpAddress());
            login.put("status", "SUCCESS"); // 假设所有操作日志都是成功的登录记录
            return login;
        }).collect(Collectors.toList());

        return ResponseResult.success("成功", logins);
    }

    /**
     * 系统管理员用户列表
     * @param page 页码
     * @param size 每页条数
     * @param role 角色
     * @param status 用户状态
     * @param keyword 搜索关键词
     * @return 用户列表
     */
    @GetMapping("/users")
    public ResponseResult<?> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        // 调试信息
        System.out.println("收到用户列表请求");
        System.out.println("请求参数: page=" + page + ", size=" + size + ", role=" + role + ", status=" + status + ", keyword=" + keyword);
        
        // 构建查询条件
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<User> userPage;

        // 处理空字符串keyword，创建一个新的final变量
        final String finalKeyword;
        if (keyword != null && !keyword.isEmpty()) {
            finalKeyword = keyword;
        } else {
            finalKeyword = null;
        }
        
        // 处理角色参数，创建一个新的final变量
        final String finalRole;
        if (role != null && !role.isEmpty()) {
            finalRole = role;
        } else {
            finalRole = null;
        }
        
        // 使用JPA Specification构建动态查询条件
        Specification<User> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 添加角色条件
            if (finalRole != null) {
                predicates.add(criteriaBuilder.equal(root.get("role"), finalRole));
            }
            
            // 添加状态条件
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            
            // 添加关键词条件（用户名、邮箱、手机号模糊查询）
            if (finalKeyword != null) {
                String likeKeyword = "%" + finalKeyword + "%";
                predicates.add(criteriaBuilder.or(
                    criteriaBuilder.like(root.get("username"), likeKeyword),
                    criteriaBuilder.like(root.get("email"), likeKeyword),
                    criteriaBuilder.like(root.get("phone"), likeKeyword)
                ));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        // 执行查询
        userPage = userRepository.findAll(spec, pageable);
        
        // 调试信息：查看Page对象的内容
        System.out.println("Page对象内容: ");
        System.out.println("  总元素数: " + userPage.getTotalElements());
        System.out.println("  总页数: " + userPage.getTotalPages());
        System.out.println("  当前页: " + userPage.getNumber() + 1);
        System.out.println("  每页大小: " + userPage.getSize());
        System.out.println("  内容列表: " + userPage.getContent());

        // 转换为响应格式
        List<Map<String, Object>> users = userPage.getContent().stream().map(user -> {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("userId", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("role", user.getRole());
            userMap.put("email", user.getEmail());
            userMap.put("phone", user.getPhone());
            userMap.put("status", user.getStatus());
            userMap.put("createTime", user.getCreateTime());
            userMap.put("updateTime", user.getUpdateTime());
            userMap.put("lastLoginTime", user.getLastLoginTime());
            userMap.put("lastLoginIp", user.getLastLoginIp());
            return userMap;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", userPage.getTotalElements());
        result.put("pages", userPage.getTotalPages());
        result.put("current", page);
        result.put("records", users);
        
        // 调试信息：查看响应结果
        System.out.println("返回响应: ");
        System.out.println("  总元素数: " + userPage.getTotalElements());
        System.out.println("  总页数: " + userPage.getTotalPages());
        System.out.println("  当前页: " + page);
        System.out.println("  记录数: " + users.size());

        return ResponseResult.success("成功", result);
    }

    /**
     * 系统管理员修改用户状态
     * @param userId 用户ID
     * @param statusData 状态数据
     * @return 修改结果
     */
    @PutMapping("/users/{userId}/status")
    public ResponseResult<?> updateUserStatus(@PathVariable Long userId, @RequestBody Map<String, Object> statusData) {
        // 查询用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 解析状态数据
        Integer status = (Integer) statusData.get("status");

        // 不能禁用系统管理员
        if ("admin".equals(user.getRole()) && status == 0) {
            return ResponseResult.error("不能禁用系统管理员");
        }

        // 更新用户状态
        user.setStatus(status);
        userRepository.save(user);

        return ResponseResult.success("用户状态修改成功");
    }
    
    /**
     * 系统管理员删除用户
     * @param userId 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/users/{userId}")
    public ResponseResult<?> deleteUser(@PathVariable Long userId) {
        // 查询用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 不能删除系统管理员
        if ("admin".equals(user.getRole())) {
            return ResponseResult.error("不能删除系统管理员");
        }
        
        // 删除用户
        userRepository.delete(user);
        
        return ResponseResult.success("用户删除成功");
    }
    
    /**
     * 系统管理员创建用户
     * @param user 用户数据
     * @return 创建结果
     */
    @PostMapping("/users")
    public ResponseResult<?> createUser(@RequestBody User user) {
        // 不能创建系统管理员用户
        if ("admin".equals(user.getRole())) {
            return ResponseResult.error("不能创建系统管理员用户");
        }
        
        // 检查用户名是否已存在
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseResult.error("用户名已存在");
        }
        
        // 检查邮箱是否已存在
        if (user.getEmail() != null && !user.getEmail().isEmpty() && userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseResult.error("邮箱已存在");
        }
        
        // 检查手机号是否已存在
        if (user.getPhone() != null && !user.getPhone().isEmpty() && userRepository.findByPhone(user.getPhone()).isPresent()) {
            return ResponseResult.error("手机号已存在");
        }
        
        // 加密密码
        String encryptedPassword = PasswordUtil.encryptPassword(user.getPassword());
        user.setPassword(encryptedPassword);
        
        // 将空字符串转换为null，避免唯一约束冲突
        user.setEmail(user.getEmail() != null && !user.getEmail().isEmpty() ? user.getEmail() : null);
        user.setPhone(user.getPhone() != null && !user.getPhone().isEmpty() ? user.getPhone() : null);
        
        // 保存用户
        User savedUser = userRepository.save(user);
        
        return ResponseResult.success("用户创建成功", savedUser);
    }

    /**
     * 系统管理员备份数据
     * @return 备份结果
     */
    @PostMapping("/backup")
    public ResponseResult<?> backupData() {
        try {
            // 创建备份目录
            String backupDir = "D:/logistic_backups";
            File dir = new File(backupDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // 生成备份文件名
            String backupId = UUID.randomUUID().toString().replace("-", "");
            String backupFileName = backupId + ".sql";
            String backupFilePath = backupDir + File.separator + backupFileName;
            
            // 数据库连接信息
            String dbHost = "localhost";
            String dbPort = "3306";
            String dbName = "mylogistic";
            String dbUser = "root";
            String dbPassword = "3141306947w666W@";
            
            // 执行mysqldump命令进行备份
            ProcessBuilder processBuilder = new ProcessBuilder(
                "mysqldump",
                "-h", dbHost,
                "-P", dbPort,
                "-u", dbUser,
                "-p" + dbPassword,
                dbName,
                "--result-file=" + backupFilePath
            );
            
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                // 备份成功
                Map<String, Object> result = new HashMap<>();
                result.put("backupId", backupId);
                result.put("backupFilePath", backupFilePath);
                
                return ResponseResult.success("备份成功", result);
            } else {
                // 备份失败
                return ResponseResult.error("备份失败，exitCode: " + exitCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseResult.error("备份失败: " + e.getMessage());
        }
    }

    /**
     * 系统管理员恢复数据
     * @param file 文件
     * @return 恢复结果
     */
    @PostMapping("/restore")
    public ResponseResult<?> restoreData(@RequestParam("backupFile") MultipartFile file) {
        try {
            // 数据库连接信息
            String dbHost = "localhost";
            String dbPort = "3306";
            String dbName = "mylogistic";
            String dbUser = "root";
            String dbPassword = "3141306947w666W@";
            
            // 保存上传的备份文件
            String tempDir = "D:/logistic_backups/temp";
            File dir = new File(tempDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            String tempFilePath = tempDir + File.separator + "temp_backup.sql";
            File tempFile = new File(tempFilePath);
            file.transferTo(tempFile);
            
            // 执行mysql命令进行恢复
            ProcessBuilder processBuilder = new ProcessBuilder(
                "mysql",
                "-h", dbHost,
                "-P", dbPort,
                "-u", dbUser,
                "-p" + dbPassword,
                dbName
            );
            
            Process process = processBuilder.start();
            OutputStream outputStream = process.getOutputStream();
            Files.copy(tempFile.toPath(), outputStream);
            outputStream.close();
            
            int exitCode = process.waitFor();
            
            // 删除临时文件
            tempFile.delete();
            
            if (exitCode == 0) {
                // 恢复成功
                return ResponseResult.success("恢复成功");
            } else {
                // 恢复失败
                return ResponseResult.error("恢复失败，exitCode: " + exitCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseResult.error("恢复失败: " + e.getMessage());
        }
    }

    /**
     * 系统管理员获取备份历史
     * @return 备份历史
     */
    @GetMapping("/backup/history")
    public ResponseResult<?> getBackupHistory() {
        try {
            // 备份目录
            String backupDir = "D:/logistic_backups";
            File dir = new File(backupDir);
            
            List<Map<String, Object>> history = new ArrayList<>();
            
            if (dir.exists() && dir.isDirectory()) {
                // 获取备份文件列表
                File[] backupFiles = dir.listFiles((dir1, name) -> name.endsWith(".sql"));
                
                if (backupFiles != null) {
                    // 按修改时间排序，最新的在前面
                    Arrays.sort(backupFiles, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                    
                    // 遍历备份文件，生成备份历史
                    for (File backupFile : backupFiles) {
                        Map<String, Object> backup = new HashMap<>();
                        String backupId = backupFile.getName().replace(".sql", "");
                        backup.put("backupId", backupId);
                        backup.put("backupTime", new Date(backupFile.lastModified()));
                        backup.put("size", formatFileSize(backupFile.length()));
                        backup.put("status", "SUCCESS");
                        history.add(backup);
                    }
                }
            }
            
            return ResponseResult.success("成功", history);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseResult.error("获取备份历史失败: " + e.getMessage());
        }
    }
    
    /**
     * 格式化文件大小
     * @param size 文件大小（字节）
     * @return 格式化后的文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + "B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2fKB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2fMB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2fGB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * 系统管理员下载备份文件
     * @param backupId 备份ID
     * @param response 响应
     */
    @GetMapping("/backup/download/{backupId}")
    public void downloadBackup(@PathVariable String backupId, HttpServletResponse response) {
        try {
            // 备份目录
            String backupDir = "D:/logistic_backups";
            String backupFileName = backupId + ".sql";
            String backupFilePath = backupDir + File.separator + backupFileName;
            File backupFile = new File(backupFilePath);
            
            if (!backupFile.exists()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            // 设置响应头
            response.setContentType("application/octet-stream");
            response.setContentLengthLong(backupFile.length());
            response.setHeader("Content-Disposition", "attachment; filename=" + backupFileName);
            
            // 读取文件并写入响应流
            Files.copy(backupFile.toPath(), response.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 系统管理员获取操作日志
     * @param page 页码
     * @param size 每页条数
     * @param username 用户名
     * @param operationType 操作类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 操作日志
     */
    @GetMapping("/logs")
    public ResponseResult<?> getOperationLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        // 构建查询条件
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<OperationLog> logPage;
        
        // 解析日期参数
        java.util.Date startDate = null;
        java.util.Date endDate = null;
        
        try {
            if (startTime != null && !startTime.isEmpty()) {
                startDate = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(startTime);
            }
            if (endTime != null && !endTime.isEmpty()) {
                endDate = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(endTime);
            }
        } catch (Exception e) {
            // 日期格式错误，忽略日期条件
        }

        // 查询操作日志
        if (username != null && !username.isEmpty() && operationType != null && !operationType.isEmpty() && startDate != null && endDate != null) {
            logPage = operationLogRepository.findByUsernameContainingAndOperationTypeContainingAndCreateTimeBetween(username, operationType, startDate, endDate, pageable);
        } else if (username != null && !username.isEmpty() && operationType != null && !operationType.isEmpty()) {
            logPage = operationLogRepository.findByUsernameContainingAndOperationTypeContaining(username, operationType, pageable);
        } else if (username != null && !username.isEmpty() && startDate != null && endDate != null) {
            logPage = operationLogRepository.findByUsernameAndCreateTimeBetween(username, startDate, endDate, pageable);
        } else if (operationType != null && !operationType.isEmpty() && startDate != null && endDate != null) {
            logPage = operationLogRepository.findByOperationTypeContainingAndCreateTimeBetween(operationType, startDate, endDate, pageable);
        } else if (username != null && !username.isEmpty()) {
            logPage = operationLogRepository.findByUsernameContaining(username, pageable);
        } else if (operationType != null && !operationType.isEmpty()) {
            logPage = operationLogRepository.findByOperationTypeContaining(operationType, pageable);
        } else if (startDate != null && endDate != null) {
            logPage = operationLogRepository.findByCreateTimeBetween(startDate, endDate, pageable);
        } else {
            logPage = operationLogRepository.findAll(pageable);
        }

        // 转换为响应格式
        List<Map<String, Object>> logs = logPage.getContent().stream().map(log -> {
            Map<String, Object> logMap = new HashMap<>();
            logMap.put("id", log.getId());
            logMap.put("username", log.getUsername());
            logMap.put("role", log.getRole());
            logMap.put("operationType", log.getOperationType());
            logMap.put("operationContent", log.getOperationContent());
            logMap.put("ipAddress", log.getIpAddress());
            logMap.put("createTime", log.getCreateTime());
            return logMap;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", logPage.getTotalElements());
        result.put("pages", logPage.getTotalPages());
        result.put("current", page);
        result.put("records", logs);

        return ResponseResult.success("成功", result);
    }

    /**
     * 系统管理员获取系统设置
     * @return 系统设置
     */
    @GetMapping("/system/settings")
    public ResponseResult<?> getSystemSettings() {
        // 获取当前系统设置
        SystemSetting setting = systemSettingService.getCurrentSetting();
        
        // 转换为响应格式
        Map<String, Object> settings = new HashMap<>();
        settings.put("systemName", setting.getSystemName());
        settings.put("systemVersion", setting.getSystemVersion());
        settings.put("contactEmail", setting.getContactEmail());
        settings.put("contactPhone", setting.getContactPhone());
        settings.put("maxLoginAttempts", setting.getMaxLoginAttempts());
        settings.put("sessionTimeout", setting.getSessionTimeout());

        return ResponseResult.success("成功", settings);
    }
    
    /**
     * 系统管理员更新系统设置
     * @param settingsData 设置数据
     * @return 更新结果
     */
    @PutMapping("/system/settings")
    public ResponseResult<?> updateSystemSettings(@RequestBody Map<String, Object> settingsData) {
        // 获取当前系统设置
        SystemSetting setting = systemSettingService.getCurrentSetting();
        
        // 解析设置数据并更新
        if (settingsData.containsKey("systemName")) {
            String systemName = (String) settingsData.get("systemName");
            setting.setSystemName(systemName.isEmpty() ? null : systemName);
        }
        if (settingsData.containsKey("systemVersion")) {
            String systemVersion = (String) settingsData.get("systemVersion");
            setting.setSystemVersion(systemVersion.isEmpty() ? null : systemVersion);
        }
        if (settingsData.containsKey("contactEmail")) {
            String contactEmail = (String) settingsData.get("contactEmail");
            setting.setContactEmail(contactEmail.isEmpty() ? null : contactEmail);
        }
        if (settingsData.containsKey("contactPhone")) {
            String contactPhone = (String) settingsData.get("contactPhone");
            setting.setContactPhone(contactPhone.isEmpty() ? null : contactPhone);
        }
        if (settingsData.containsKey("maxLoginAttempts")) {
            // 处理数字类型参数，可能是Integer或String
            Object maxLoginAttemptsObj = settingsData.get("maxLoginAttempts");
            if (maxLoginAttemptsObj != null) {
                if (maxLoginAttemptsObj instanceof Integer) {
                    setting.setMaxLoginAttempts((Integer) maxLoginAttemptsObj);
                } else if (maxLoginAttemptsObj instanceof String) {
                    String maxLoginAttemptsStr = (String) maxLoginAttemptsObj;
                    if (!maxLoginAttemptsStr.isEmpty()) {
                        setting.setMaxLoginAttempts(Integer.parseInt(maxLoginAttemptsStr));
                    }
                }
            }
        }
        if (settingsData.containsKey("sessionTimeout")) {
            // 处理数字类型参数，可能是Integer或String
            Object sessionTimeoutObj = settingsData.get("sessionTimeout");
            if (sessionTimeoutObj != null) {
                if (sessionTimeoutObj instanceof Integer) {
                    setting.setSessionTimeout((Integer) sessionTimeoutObj);
                } else if (sessionTimeoutObj instanceof String) {
                    String sessionTimeoutStr = (String) sessionTimeoutObj;
                    if (!sessionTimeoutStr.isEmpty()) {
                        setting.setSessionTimeout(Integer.parseInt(sessionTimeoutStr));
                    }
                }
            }
        }
        
        // 保存更新后的系统设置
        systemSettingService.updateSetting(setting);
        
        return ResponseResult.success("系统设置更新成功");
    }

    /**
     * 系统管理员获取角色列表
     * @return 角色列表
     */
    @GetMapping("/roles")
    public ResponseResult<?> getRoles() {
        // 获取所有角色
        List<String> roles = userRepository.findAll().stream()
                .map(User::getRole)
                .distinct()
                .collect(Collectors.toList());

        // 转换为响应格式
        List<Map<String, Object>> result = roles.stream().map(role -> {
            Map<String, Object> roleMap = new HashMap<>();
            roleMap.put("roleId", 1); // 模拟ID
            roleMap.put("roleName", role);
            roleMap.put("description", getRoleDescription(role));
            return roleMap;
        }).collect(Collectors.toList());

        return ResponseResult.success("成功", result);
    }
    
    /**
     * 系统管理员导出操作日志
     * @param username 用户名
     * @param operationType 操作类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param response HTTP响应
     */
    @GetMapping("/logs/export")
    public void exportOperationLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            HttpServletResponse response) {
        try {
            // 解析日期参数
            java.util.Date startDate = null;
            java.util.Date endDate = null;
            
            if (startTime != null && !startTime.isEmpty()) {
                startDate = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(startTime);
            }
            if (endTime != null && !endTime.isEmpty()) {
                endDate = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(endTime);
            }
            
            // 查询操作日志
            List<OperationLog> logs;
            if (username != null && !username.isEmpty() && operationType != null && !operationType.isEmpty() && startDate != null && endDate != null) {
                logs = operationLogRepository.findByUsernameContainingAndOperationTypeAndCreateTimeBetween(username, operationType, startDate, endDate, Pageable.unpaged()).getContent();
            } else if (username != null && !username.isEmpty() && operationType != null && !operationType.isEmpty()) {
                logs = operationLogRepository.findByUsernameContainingAndOperationType(username, operationType, Pageable.unpaged()).getContent();
            } else if (username != null && !username.isEmpty() && startDate != null && endDate != null) {
                logs = operationLogRepository.findByUsernameAndCreateTimeBetween(username, startDate, endDate, Pageable.unpaged()).getContent();
            } else if (operationType != null && !operationType.isEmpty() && startDate != null && endDate != null) {
                logs = operationLogRepository.findByOperationTypeAndCreateTimeBetween(operationType, startDate, endDate, Pageable.unpaged()).getContent();
            } else if (username != null && !username.isEmpty()) {
                logs = operationLogRepository.findByUsernameContaining(username, Pageable.unpaged()).getContent();
            } else if (operationType != null && !operationType.isEmpty()) {
                logs = operationLogRepository.findByOperationType(operationType, Pageable.unpaged()).getContent();
            } else if (startDate != null && endDate != null) {
                logs = operationLogRepository.findByCreateTimeBetween(startDate, endDate, Pageable.unpaged()).getContent();
            } else {
                logs = operationLogRepository.findAll(Pageable.unpaged()).getContent();
            }
            
            // 创建Excel工作簿
            org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("操作日志");
            
            // 创建表头
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            String[] headers = {"日志ID", "用户名", "角色", "操作类型", "操作内容", "IP地址", "操作时间"};
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            
            // 填充数据
            for (int i = 0; i < logs.size(); i++) {
                OperationLog log = logs.get(i);
                org.apache.poi.ss.usermodel.Row dataRow = sheet.createRow(i + 1);
                
                dataRow.createCell(0).setCellValue(log.getId());
                dataRow.createCell(1).setCellValue(log.getUsername());
                dataRow.createCell(2).setCellValue(log.getRole());
                dataRow.createCell(3).setCellValue(log.getOperationType());
                dataRow.createCell(4).setCellValue(log.getOperationContent());
                dataRow.createCell(5).setCellValue(log.getIpAddress() != null ? log.getIpAddress() : "-");
                dataRow.createCell(6).setCellValue(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(log.getCreateTime()));
            }
            
            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=operation-logs-" + new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".xlsx");
            
            // 写入响应流
            workbook.write(response.getOutputStream());
            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取角色描述
     * @param role 角色名称
     * @return 角色描述
     */
    private String getRoleDescription(String role) {
        switch (role) {
            case "admin":
                return "系统管理员";
            case "customer":
                return "客户";
            case "manager":
                return "物流经理";
            case "warehouse":
                return "仓库管理员";
            case "delivery":
                return "配送员";
            case "finance":
                return "财务人员";
            default:
                return "未知角色";
        }
    }
}