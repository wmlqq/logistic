package com.software.logistic.controller;

import com.software.logistic.common.ResponseResult;
import com.software.logistic.entity.DeliveryTask;
import com.software.logistic.entity.Order;
import com.software.logistic.entity.User;
import com.software.logistic.repository.DeliveryTaskRepository;
import com.software.logistic.repository.OrderRepository;
import com.software.logistic.repository.UserRepository;
import com.software.logistic.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/delivery")
public class DeliveryController {

    @Autowired
    private DeliveryTaskRepository deliveryTaskRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OperationLogService operationLogService;

    /**
     * 配送员仪表盘统计
     * @return 统计数据
     */
    @GetMapping("/stats")
    public ResponseResult<?> getStats() {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        // 检查是否为匿名用户
        if ("anonymousUser".equals(username)) {
            // 匿名用户返回空数据或默认数据
            Map<String, Object> stats = new HashMap<>();
            stats.put("pendingTasks", 0);
            stats.put("deliveringTasks", 0);
            stats.put("completedToday", 0);
            stats.put("totalCompleted", 0);
            return ResponseResult.success("成功", stats);
        }
        
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("用户不存在"));

        // 统计配送任务数量
        long pendingTasks = deliveryTaskRepository.countByDeliveryManIdAndStatus(user.getId(), "PENDING");
        long deliveringTasks = deliveryTaskRepository.countByDeliveryManIdAndStatus(user.getId(), "DELIVERING");
        long completedToday = deliveryTaskRepository.countByDeliveryManIdAndStatusAndCreateTimeAfter(user.getId(), "COMPLETED", new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000));
        long totalCompleted = deliveryTaskRepository.countByDeliveryManIdAndStatus(user.getId(), "COMPLETED");

        Map<String, Object> stats = new HashMap<>();
        stats.put("pendingTasks", pendingTasks);
        stats.put("deliveringTasks", deliveringTasks);
        stats.put("completedToday", completedToday);
        stats.put("totalCompleted", totalCompleted);

        return ResponseResult.success("成功", stats);
    }

    /**
     * 配送员待配送任务
     * @return 待配送任务列表
     */
    @GetMapping("/tasks/pending")
    public ResponseResult<?> getPendingTasks() {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        // 检查是否为匿名用户
        if ("anonymousUser".equals(username)) {
            // 匿名用户返回空列表
            return ResponseResult.success("成功", Collections.emptyList());
        }
        
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("用户不存在"));

        // 查询待配送任务
        List<DeliveryTask> tasks = deliveryTaskRepository.findByDeliveryManIdAndStatus(user.getId(), "PENDING");

        // 转换为响应格式
        List<Map<String, Object>> result = tasks.stream().map(task -> {
            Map<String, Object> taskMap = new HashMap<>();
            taskMap.put("taskId", task.getId());
            taskMap.put("orderNumber", task.getOrderNumber());
            taskMap.put("receiverName", task.getReceiverName());
            taskMap.put("receiverAddress", task.getReceiverAddress());
            return taskMap;
        }).collect(Collectors.toList());

        return ResponseResult.success("成功", result);
    }

    /**
     * 配送员配送中任务
     * @return 配送中任务列表
     */
    @GetMapping("/tasks/delivering")
    public ResponseResult<?> getDeliveringTasks() {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        // 检查是否为匿名用户
        if ("anonymousUser".equals(username)) {
            // 匿名用户返回空列表
            return ResponseResult.success("成功", Collections.emptyList());
        }
        
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("用户不存在"));

        // 查询配送中任务
        List<DeliveryTask> tasks = deliveryTaskRepository.findByDeliveryManIdAndStatus(user.getId(), "DELIVERING");

        // 转换为响应格式
        List<Map<String, Object>> result = tasks.stream().map(task -> {
            Map<String, Object> taskMap = new HashMap<>();
            taskMap.put("taskId", task.getId());
            taskMap.put("orderNumber", task.getOrderNumber());
            taskMap.put("receiverName", task.getReceiverName());
            taskMap.put("receiverAddress", task.getReceiverAddress());
            taskMap.put("status", task.getStatus());
            return taskMap;
        }).collect(Collectors.toList());

        return ResponseResult.success("成功", result);
    }

    /**
     * 配送员开始配送
     * @param taskId 任务ID
     * @return 开始配送结果
     */
    @PutMapping("/task/{taskId}/start")
    public ResponseResult<?> startDelivery(@PathVariable Long taskId, HttpServletRequest request) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        // 查询配送任务
        DeliveryTask task = deliveryTaskRepository.findById(taskId)
                .filter(t -> t.getDeliveryManId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("配送任务不存在或无权限访问"));

        // 检查任务状态
        if (!"PENDING".equals(task.getStatus())) {
            throw new RuntimeException("只有待配送的任务才能开始配送");
        }

        // 更新任务状态
        task.setStatus("DELIVERING");
        deliveryTaskRepository.save(task);
        
        // 更新对应的订单状态
        Order order = orderRepository.findById(task.getOrderId())
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        order.setStatus("DELIVERING");
        orderRepository.save(order);
        
        // 记录开始配送日志
        String ipAddress = request.getRemoteAddr();
        operationLogService.logOperation(
            user.getId(),
            user.getUsername(),
            user.getRole(),
            "START_DELIVERY",
            "配送员开始配送任务，任务ID：" + taskId + "，订单号：" + task.getOrderNumber(),
            ipAddress
        );

        return ResponseResult.success("开始配送成功");
    }

    /**
     * 配送员完成配送
     * @param taskId 任务ID
     * @return 完成配送结果
     */
    @PutMapping("/task/{taskId}/complete")
    public ResponseResult<?> completeDelivery(@PathVariable Long taskId, HttpServletRequest request) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        // 查询配送任务
        DeliveryTask task = deliveryTaskRepository.findById(taskId)
                .filter(t -> t.getDeliveryManId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("配送任务不存在或无权限访问"));

        // 检查任务状态
        if (!"DELIVERING".equals(task.getStatus())) {
            throw new RuntimeException("只有配送中的任务才能完成配送");
        }

        // 更新任务状态
        Date now = new Date();
        task.setStatus("COMPLETED");
        task.setDeliveryTime(now);
        deliveryTaskRepository.save(task);
        
        // 更新对应的订单状态和实际送达时间
        Order order = orderRepository.findById(task.getOrderId())
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        order.setStatus("COMPLETED");
        // 格式化时间为yyyy-MM-dd HH:mm:ss字符串
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        order.setDeliveryTime(sdf.format(now));
        orderRepository.save(order);
        
        // 记录完成配送日志
        String ipAddress = request.getRemoteAddr();
        operationLogService.logOperation(
            user.getId(),
            user.getUsername(),
            user.getRole(),
            "COMPLETE_DELIVERY",
            "配送员完成配送任务，任务ID：" + taskId + "，订单号：" + task.getOrderNumber(),
            ipAddress
        );

        return ResponseResult.success("配送完成成功");
    }

    /**
     * 配送员任务列表
     * @return 任务列表
     */
    @GetMapping("/tasks")
    public ResponseResult<?> getTasks() {
        try {
            // 获取当前登录用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("用户不存在"));

            // 查询所有配送任务
            List<DeliveryTask> tasks = deliveryTaskRepository.findByDeliveryManId(user.getId(), Sort.by(Sort.Direction.DESC, "createTime"));

            // 转换为响应格式
            List<Map<String, Object>> result = tasks.stream().map(task -> {
                Map<String, Object> taskMap = new HashMap<>();
                taskMap.put("taskId", task.getId());
                taskMap.put("orderId", task.getOrderId());
                taskMap.put("orderNumber", task.getOrderNumber());
                taskMap.put("deliveryManId", task.getDeliveryManId());
                taskMap.put("deliveryManName", task.getDeliveryManName());
                taskMap.put("receiverName", task.getReceiverName());
                taskMap.put("receiverPhone", task.getReceiverPhone());
                taskMap.put("receiverAddress", task.getReceiverAddress());
                taskMap.put("status", task.getStatus());
                taskMap.put("createTime", task.getCreateTime());
                taskMap.put("updateTime", task.getUpdateTime());
                taskMap.put("expectedDeliveryTime", task.getExpectedDeliveryTime());

                // 模拟商品列表
                List<Map<String, Object>> products = new ArrayList<>();
                Map<String, Object> product = new HashMap<>();
                product.put("productId", 1);
                product.put("productName", "商品示例");
                product.put("productCode", "PROD001");
                product.put("specification", "规格1");
                product.put("unit", "件");
                product.put("quantity", 1);
                product.put("price", 100.00);
                products.add(product);
                taskMap.put("products", products);

                return taskMap;
            }).collect(Collectors.toList());

            return ResponseResult.success("成功", result);
        } catch (Exception e) {
            return ResponseResult.error("获取任务列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 配送员任务详情
     * @param taskId 任务ID
     * @return 任务详情
     */
    @GetMapping("/task/{taskId}")
    public ResponseResult<?> getTaskDetail(@PathVariable Long taskId) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        // 查询配送任务
        DeliveryTask task = deliveryTaskRepository.findById(taskId)
                .filter(t -> t.getDeliveryManId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("配送任务不存在或无权限访问"));

        // 转换为响应格式
        Map<String, Object> taskMap = new HashMap<>();
        taskMap.put("taskId", task.getId());
        taskMap.put("orderId", task.getOrderId());
        taskMap.put("orderNumber", task.getOrderNumber());
        taskMap.put("deliveryManId", task.getDeliveryManId());
        taskMap.put("deliveryManName", task.getDeliveryManName());
        taskMap.put("receiverName", task.getReceiverName());
        taskMap.put("receiverPhone", task.getReceiverPhone());
        taskMap.put("receiverAddress", task.getReceiverAddress());
        taskMap.put("status", task.getStatus());
        taskMap.put("createTime", task.getCreateTime());
        taskMap.put("updateTime", task.getUpdateTime());
        taskMap.put("expectedDeliveryTime", task.getExpectedDeliveryTime());
        taskMap.put("actualDeliveryTime", task.getDeliveryTime());

        // 模拟商品列表
        List<Map<String, Object>> products = new ArrayList<>();
        Map<String, Object> product = new HashMap<>();
        product.put("productId", 1);
        product.put("productName", "商品示例");
        product.put("productCode", "PROD001");
        product.put("specification", "规格1");
        product.put("unit", "件");
        product.put("quantity", 1);
        product.put("price", 100.00);
        products.add(product);
        taskMap.put("products", products);

        return ResponseResult.success("成功", taskMap);
    }
    
    /**
     * 更新配送任务预计送达时间
     * @param taskId 任务ID
     * @param requestBody 请求参数
     * @return 更新结果
     */
    @PutMapping("/task/{taskId}/expected-time")
    public ResponseResult<?> updateExpectedDeliveryTime(@PathVariable Long taskId, @RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        // 查询配送任务
        DeliveryTask task = deliveryTaskRepository.findById(taskId)
                .filter(t -> t.getDeliveryManId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("配送任务不存在或无权限访问"));

        // 获取预计送达时间
        String expectedTimeStr = (String) requestBody.get("expectedDeliveryTime");
        try {
            // 解析时间字符串，支持两种格式：yyyy-MM-dd HH:mm:ss 和 yyyy-MM-dd HH:mm
            SimpleDateFormat sdf;
            if (expectedTimeStr.length() == 16) { // yyyy-MM-dd HH:mm
                sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            } else { // yyyy-MM-dd HH:mm:ss
                sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            }
            Date expectedTime = sdf.parse(expectedTimeStr);
            
            // 验证预计送达时间必须晚于当前时间
            Date currentTime = new Date();
            if (expectedTime.before(currentTime)) {
                throw new RuntimeException("预计送达时间必须晚于当前时间");
            }
            
            // 更新预计送达时间
            task.setExpectedDeliveryTime(expectedTime);
            deliveryTaskRepository.save(task);
            
            // 记录更新预计时间日志
            String ipAddress = request.getRemoteAddr();
            operationLogService.logOperation(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                "UPDATE_EXPECTED_TIME",
                "配送员更新预计送达时间，任务ID：" + taskId + "，新预计时间：" + expectedTimeStr,
                ipAddress
            );
            
            return ResponseResult.success("预计送达时间更新成功");
        } catch (Exception e) {
            throw new RuntimeException("时间格式错误，应为yyyy-MM-dd HH:mm或yyyy-MM-dd HH:mm:ss");
        }
    }
}