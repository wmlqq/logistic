package com.software.logistic.controller;

import com.software.logistic.common.ResponseResult;
import com.software.logistic.entity.DeliveryTask;
import com.software.logistic.entity.Location;
import com.software.logistic.entity.Order;
import com.software.logistic.entity.OrderItem;
import com.software.logistic.entity.Product;
import com.software.logistic.entity.StockChange;
import com.software.logistic.entity.User;
import com.software.logistic.repository.DeliveryTaskRepository;
import com.software.logistic.repository.LocationRepository;
import com.software.logistic.repository.OrderItemRepository;
import com.software.logistic.repository.OrderRepository;
import com.software.logistic.repository.ProductRepository;
import com.software.logistic.repository.StockChangeRepository;
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
@RequestMapping("/api/manager")
public class ManagerController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private DeliveryTaskRepository deliveryTaskRepository;
    
    @Autowired
    private LocationRepository locationRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private StockChangeRepository stockChangeRepository;
    
    @Autowired
    private OperationLogService operationLogService;

    /**
     * 物流经理仪表盘统计
     * @return 统计数据
     */
    @GetMapping("/stats")
    public ResponseResult<?> getStats() {
        // 统计订单数量
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByStatus("PENDING");
        long deliveringOrders = orderRepository.countByStatus("DELIVERING");
        long completedOrders = orderRepository.countByStatus("COMPLETED");

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", totalOrders);
        stats.put("pendingOrders", pendingOrders);
        stats.put("deliveringOrders", deliveringOrders);
        stats.put("completedOrders", completedOrders);

        return ResponseResult.success("成功", stats);
    }

    /**
     * 物流经理最近订单
     * @return 最近订单列表
     */
    @GetMapping("/orders/recent")
    public ResponseResult<?> getRecentOrders() {
        // 查询最近5个订单
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Order> orderPage = orderRepository.findAll(pageable);

        // 转换为响应格式
        List<Map<String, Object>> recentOrders = orderPage.getContent().stream().map(order -> {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("orderId", order.getId());
            orderMap.put("orderNumber", order.getOrderNumber());
            orderMap.put("customerName", order.getCustomerName());
            orderMap.put("status", order.getStatus());
            return orderMap;
        }).collect(Collectors.toList());

        return ResponseResult.success("成功", recentOrders);
    }

    /**
     * 物流经理配送员状态
     * @return 配送员状态列表
     */
    @GetMapping("/delivery/status")
    public ResponseResult<?> getDeliveryStatus() {
        // 查询所有配送员
        List<User> deliveryMen = userRepository.findByRole("delivery");

        // 转换为响应格式
        List<Map<String, Object>> result = deliveryMen.stream().map(deliveryMan -> {
            Map<String, Object> deliveryMap = new HashMap<>();
            deliveryMap.put("name", deliveryMan.getUsername());
            deliveryMap.put("status", "AVAILABLE"); // 模拟状态，实际项目中需要根据配送任务计算
            deliveryMap.put("pendingOrders", 0); // 模拟数据
            deliveryMap.put("completedToday", 5); // 模拟数据
            return deliveryMap;
        }).collect(Collectors.toList());

        return ResponseResult.success("成功", result);
    }

    /**
     * 物流经理订单列表
     * @param page 页码
     * @param size 每页条数
     * @param status 订单状态
     * @param orderNumber 订单编号
     * @param customerName 客户名称
     * @param deliveryMethod 配送方式
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 订单列表
     */
    @GetMapping("/orders")
    public ResponseResult<?> getOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String deliveryMethod,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        // 构建查询条件
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Order> orderPage;

        // 支持多条件查询
        if (status != null && !status.isEmpty()) {
            // 按状态查询
            orderPage = orderRepository.findByStatus(status, pageable);
        } else if (deliveryMethod != null && !deliveryMethod.isEmpty()) {
            // 按配送方式查询
            orderPage = orderRepository.findByDeliveryMethod(deliveryMethod, pageable);
        } else if (orderNumber != null || customerName != null) {
            // 按订单编号或客户名称模糊查询
            String keyword = orderNumber != null ? orderNumber : customerName;
            orderPage = orderRepository.findByOrderNumberContainingOrCustomerNameContaining(keyword, keyword, pageable);
        } else {
            // 查询所有订单
            orderPage = orderRepository.findAll(pageable);
        }

        // 转换为响应格式
        List<Map<String, Object>> orders = orderPage.getContent().stream().map(order -> {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("orderId", order.getId());
            orderMap.put("orderNumber", order.getOrderNumber());
            orderMap.put("customerName", order.getCustomerName());
            orderMap.put("createTime", order.getCreateTime());
            orderMap.put("deliveryMethod", order.getDeliveryMethod());
            orderMap.put("status", order.getStatus());
            orderMap.put("totalAmount", order.getTotalAmount());
            return orderMap;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("orders", orders);
        result.put("totalPages", orderPage.getTotalPages());
        result.put("currentPage", page);

        return ResponseResult.success("成功", result);
    }

    /**
     * 物流经理审核订单
     * @param orderId 订单ID
     * @return 审核结果
     */
    @PutMapping("/orders/{orderId}/approve")
    public ResponseResult<?> approveOrder(@PathVariable Long orderId, HttpServletRequest request) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username).orElseThrow();
        
        // 查询订单
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("订单不存在"));

        // 检查订单状态
        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("只有待处理的订单才能审核");
        }

        // 查询订单商品
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        // 检查库存是否充足
        for (OrderItem orderItem : orderItems) {
            Product product = productRepository.findById(orderItem.getProductId())
                    .orElseThrow(() -> new RuntimeException("商品不存在"));
            
            if (product.getStock() < orderItem.getQuantity()) {
                throw new RuntimeException("商品" + product.getProductName() + "库存不足");
            }
        }

        // 更新商品库存并生成出库记录
        for (OrderItem orderItem : orderItems) {
            Product product = productRepository.findById(orderItem.getProductId())
                    .orElseThrow(() -> new RuntimeException("商品不存在"));
            
            // 记录变动前库存
            int beforeStock = product.getStock();
            // 减少库存
            int afterStock = beforeStock - orderItem.getQuantity();
            product.setStock(afterStock);
            productRepository.save(product);
            
            // 如果商品关联了仓库位置，更新位置的已使用量
            if (product.getLocation() != null) {
                Location location = locationRepository.findById(product.getLocation().getId())
                        .orElseThrow(() -> new RuntimeException("位置不存在"));
                
                // 计算库存变化量
                int stockDiff = afterStock - beforeStock;
                // 更新位置使用量
                location.setUsed(location.getUsed() + stockDiff);
                locationRepository.save(location);
            }
            
            // 生成出库记录
            StockChange stockChange = new StockChange();
            stockChange.setProductId(product.getId());
            stockChange.setProductName(product.getProductName());
            stockChange.setChangeType("OUT");
            stockChange.setQuantity(orderItem.getQuantity());
            stockChange.setBeforeStock(beforeStock);
            stockChange.setAfterStock(afterStock);
            stockChange.setOperatorId(currentUser.getId());
            stockChange.setOperatorName(currentUser.getUsername());
            stockChange.setRemark("订单审核出库，订单号：" + order.getOrderNumber());
            stockChangeRepository.save(stockChange);
        }

        // 审核通过，更新订单状态为已审核
        order.setStatus("APPROVED");
        orderRepository.save(order);
        
        // 记录审批订单日志
        String ipAddress = request.getRemoteAddr();
        operationLogService.logOperation(
            currentUser.getId(),
            currentUser.getUsername(),
            currentUser.getRole(),
            "APPROVE_ORDER",
            "物流经理审批订单，订单号：" + order.getOrderNumber(),
            ipAddress
        );

        return ResponseResult.success("订单审核成功");
    }

    /**
     * 物流经理分配订单
     * @param orderId 订单ID
     * @param assignData 分配数据
     * @return 分配结果
     */
    @PutMapping("/orders/{orderId}/assign")
    public ResponseResult<?> assignOrder(@PathVariable Long orderId, @RequestBody Map<String, Object> assignData, HttpServletRequest request) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username).orElseThrow();
        
        // 查询订单
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("订单不存在"));

        // 检查订单状态
        if (!"APPROVED".equals(order.getStatus())) {
            throw new RuntimeException("只有已审核的订单才能分配");
        }

        // 解析分配数据
        Long deliveryId = Long.parseLong(assignData.get("deliveryId").toString());

        // 查询配送员
        User deliveryMan = userRepository.findById(deliveryId)
                .filter(u -> "delivery".equals(u.getRole()))
                .orElseThrow(() -> new RuntimeException("配送员不存在"));

        // 更新订单状态
        order.setStatus("ASSIGNED");
        orderRepository.save(order);

        // 创建配送任务
        DeliveryTask deliveryTask = new DeliveryTask();
        deliveryTask.setOrderId(order.getId());
        deliveryTask.setOrderNumber(order.getOrderNumber());
        deliveryTask.setDeliveryManId(deliveryId);
        deliveryTask.setDeliveryManName(deliveryMan.getUsername());
        deliveryTask.setReceiverName(order.getReceiverName());
        deliveryTask.setReceiverPhone(order.getReceiverPhone());
        deliveryTask.setReceiverAddress(order.getReceiverAddress());
        deliveryTask.setStatus("PENDING");

        deliveryTaskRepository.save(deliveryTask);
        
        // 记录分配订单日志
        String ipAddress = request.getRemoteAddr();
        operationLogService.logOperation(
            currentUser.getId(),
            currentUser.getUsername(),
            currentUser.getRole(),
            "ASSIGN_ORDER",
            "物流经理分配订单，订单号：" + order.getOrderNumber() + "，分配给配送员：" + deliveryMan.getUsername(),
            ipAddress
        );

        return ResponseResult.success("订单分配成功");
    }

    /**
     * 物流经理配送任务列表
     * @param page 页码
     * @param size 每页条数
     * @param status 任务状态
     * @param deliveryManId 配送员ID
     * @return 配送任务列表
     */
    @GetMapping("/delivery/tasks")
    public ResponseResult<?> getDeliveryTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long deliveryManId) {
        // 构建查询条件
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<DeliveryTask> taskPage;

        if (status != null && deliveryManId != null) {
            // 根据状态和配送员ID查询
            taskPage = deliveryTaskRepository.findByDeliveryManIdAndStatus(deliveryManId, status, pageable);
        } else if (status != null) {
            // 根据状态查询
            taskPage = deliveryTaskRepository.findByStatus(status, pageable);
        } else if (deliveryManId != null) {
            // 根据配送员ID查询
            taskPage = deliveryTaskRepository.findByDeliveryManId(deliveryManId, pageable);
        } else {
            // 查询所有
            taskPage = deliveryTaskRepository.findAll(pageable);
        }

        // 转换为响应格式
        List<Map<String, Object>> tasks = taskPage.getContent().stream().map(task -> {
            Map<String, Object> taskMap = new HashMap<>();
            taskMap.put("taskId", task.getId());
            taskMap.put("orderId", task.getOrderId());
            taskMap.put("orderNumber", task.getOrderNumber());
            taskMap.put("receiverName", task.getReceiverName());
            taskMap.put("receiverPhone", task.getReceiverPhone());
            taskMap.put("receiverAddress", task.getReceiverAddress());
            taskMap.put("status", task.getStatus());
            taskMap.put("deliveryManId", task.getDeliveryManId());
            taskMap.put("deliveryManName", task.getDeliveryManName());
            taskMap.put("createTime", task.getCreateTime());
            taskMap.put("updateTime", task.getUpdateTime());
            taskMap.put("expectedDeliveryTime", "2025-01-01 18:00:00"); // 模拟数据
            return taskMap;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("tasks", tasks);
        result.put("totalPages", taskPage.getTotalPages());
        result.put("currentPage", page);

        return ResponseResult.success("成功", result);
    }

    /**
     * 物流经理分配配送任务
     * @param taskId 任务ID
     * @param assignData 分配数据
     * @return 分配结果
     */
    @PutMapping("/delivery/tasks/{taskId}/assign")
    public ResponseResult<?> assignDeliveryTask(@PathVariable Long taskId, @RequestBody Map<String, Object> assignData, HttpServletRequest request) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username).orElseThrow();
        
        // 查询配送任务
        DeliveryTask task = deliveryTaskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("配送任务不存在"));

        // 解析分配数据
        Long deliveryManId = Long.parseLong(assignData.get("deliveryManId").toString());

        // 查询配送员
        User deliveryMan = userRepository.findById(deliveryManId)
                .filter(u -> "delivery".equals(u.getRole()))
                .orElseThrow(() -> new RuntimeException("配送员不存在"));

        // 更新配送任务
        task.setDeliveryManId(deliveryManId);
        task.setDeliveryManName(deliveryMan.getUsername());
        task.setStatus("ASSIGNED");
        deliveryTaskRepository.save(task);
        
        // 记录分配配送任务日志
        String ipAddress = request.getRemoteAddr();
        operationLogService.logOperation(
            currentUser.getId(),
            currentUser.getUsername(),
            currentUser.getRole(),
            "ASSIGN_DELIVERY_TASK",
            "物流经理分配配送任务，任务ID：" + task.getId() + "，订单号：" + task.getOrderNumber() + "，分配给配送员：" + deliveryMan.getUsername(),
            ipAddress
        );

        return ResponseResult.success("任务分配成功");
    }
    
    /**
     * 物流经理查看配送任务详情
     * @param taskId 任务ID
     * @return 配送任务详情
     */
    @GetMapping("/delivery/tasks/{taskId}")
    public ResponseResult<?> getDeliveryTaskDetail(@PathVariable Long taskId) {
        // 查询配送任务
        DeliveryTask task = deliveryTaskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("配送任务不存在"));
        
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
        taskMap.put("expectedDeliveryTime", "2025-01-01 18:00:00"); // 模拟数据
        
        return ResponseResult.success("成功", taskMap);
    }

    /**
     * 物流经理获取仓库列表
     * @param page 页码
     * @param size 每页条数
     * @return 仓库列表
     */
    @GetMapping("/warehouses")
    public ResponseResult<?> getWarehouses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        // 查询所有仓库位置
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "locationCode"));
        Page<Location> locationPage = locationRepository.findAll(pageable);
        
        // 转换为响应格式
        List<Map<String, Object>> warehouses = locationPage.getContent().stream().map(location -> {
            Map<String, Object> warehouseMap = new HashMap<>();
            warehouseMap.put("warehouseId", location.getId());
            warehouseMap.put("warehouseName", location.getLocationCode());
            warehouseMap.put("address", location.getDescription());
            warehouseMap.put("managerName", "管理员"); // 模拟数据
            warehouseMap.put("phone", "13800138000"); // 模拟数据
            warehouseMap.put("totalInventory", location.getUsed());
            return warehouseMap;
        }).collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("warehouses", warehouses);
        result.put("totalPages", locationPage.getTotalPages());
        result.put("currentPage", page);
        result.put("total", locationPage.getTotalElements());

        return ResponseResult.success("成功", result);
    }
    
    /**
     * 物流经理获取库存预警
     * @return 库存预警列表
     */
    @GetMapping("/warehouses/inventory/alerts")
    public ResponseResult<?> getInventoryAlerts() {
        // 查询低库存商品
        List<Product> lowStockProducts = productRepository.findByStockLessThanAlertThreshold();
        
        // 转换为响应格式
        List<Map<String, Object>> alerts = lowStockProducts.stream().map(product -> {
            Map<String, Object> alertMap = new HashMap<>();
            alertMap.put("productId", product.getId());
            alertMap.put("productName", product.getProductName());
            alertMap.put("currentStock", product.getStock());
            alertMap.put("alertThreshold", product.getAlertThreshold());
            alertMap.put("warehouseName", product.getLocation() != null ? product.getLocation().getLocationCode() : "未分配");
            return alertMap;
        }).collect(Collectors.toList());

        return ResponseResult.success("成功", alerts);
    }
    
    /**
     * 物流经理获取用户列表（按角色筛选）
     * @param role 角色
     * @return 用户列表
     */
    @GetMapping("/users")
    public ResponseResult<?> getUsers(@RequestParam(required = false) String role) {
        // 查询用户列表
        List<User> users;
        
        // 只返回角色为仓库管理员的用户
        users = userRepository.findByRole("warehouse");
        
        // 转换为响应格式
        List<Map<String, Object>> userList = users.stream().map(user -> {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("username", user.getUsername());
            userMap.put("name", user.getUsername()); // 由于User实体没有name字段，使用username代替
            userMap.put("phone", user.getPhone());
            userMap.put("email", user.getEmail());
            return userMap;
        }).collect(Collectors.toList());

        return ResponseResult.success("成功", userList);
    }

    /**
     * 物流经理查看仓库详情
     * @param warehouseId 仓库ID
     * @return 仓库详情
     */
    @GetMapping("/warehouses/{warehouseId}")
    public ResponseResult<?> getWarehouseDetail(@PathVariable Long warehouseId) {
        // 查询仓库
        Location location = locationRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("仓库不存在或无权限访问"));

        // 转换为响应格式
        Map<String, Object> warehouseMap = new HashMap<>();
        warehouseMap.put("warehouseId", location.getId());
        warehouseMap.put("warehouseName", location.getLocationCode());
        warehouseMap.put("address", location.getDescription());
        warehouseMap.put("managerName", "管理员"); // 模拟数据
        warehouseMap.put("phone", "13800138000"); // 模拟数据
        warehouseMap.put("totalInventory", location.getUsed());
        warehouseMap.put("capacity", location.getCapacity());
        warehouseMap.put("availableSpace", location.getCapacity() - location.getUsed());

        return ResponseResult.success("成功", warehouseMap);
    }

    /**
     * 物流经理查看商品详情
     * @param productId 商品ID
     * @return 商品详情
     */
    @GetMapping("/products/{productId}")
    public ResponseResult<?> getProductDetail(@PathVariable Long productId) {
        // 查询商品
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在或无权限访问"));

        // 转换为响应格式
        Map<String, Object> productMap = new HashMap<>();
        productMap.put("productId", product.getId());
        productMap.put("productName", product.getProductName());
        productMap.put("productCode", product.getProductCode());
        productMap.put("stock", product.getStock());
        productMap.put("alertThreshold", product.getAlertThreshold());
        productMap.put("price", product.getPrice());
        productMap.put("warehouseName", product.getLocation() != null ? product.getLocation().getLocationCode() : "未分配");

        return ResponseResult.success("成功", productMap);
    }

    /**
     * 物流经理获取订单报表数据
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 订单报表数据
     */
    @GetMapping("/reports/orders")
    public ResponseResult<?> getOrderReport(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        // 生成模拟数据
        List<Map<String, Object>> reportData = new ArrayList<>();
        
        // 生成最近30天的模拟数据
        for (int i = 29; i >= 0; i--) {
            Map<String, Object> data = new HashMap<>();
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -i);
            data.put("date", new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime()));
            data.put("orderCount", (int) (Math.random() * 50) + 10); // 10-60个订单
            data.put("totalAmount", (int) (Math.random() * 5000) + 1000); // 1000-6000元
            reportData.add(data);
        }
        
        return ResponseResult.success("成功", reportData);
    }

    /**
     * 物流经理获取配送效率报表数据
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 配送效率报表数据
     */
    @GetMapping("/reports/delivery-efficiency")
    public ResponseResult<?> getDeliveryEfficiencyReport(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        // 生成模拟数据
        List<Map<String, Object>> reportData = new ArrayList<>();
        
        // 模拟5个配送员的数据
        String[] deliveryNames = {"张三", "李四", "王五", "赵六", "钱七"};
        for (String name : deliveryNames) {
            Map<String, Object> data = new HashMap<>();
            data.put("deliveryName", name);
            data.put("completedTasks", (int) (Math.random() * 30) + 10); // 10-40个任务
            data.put("efficiency", Math.round(Math.random() * 20) + 80); // 80-100%效率
            reportData.add(data);
        }
        
        return ResponseResult.success("成功", reportData);
    }

    /**
     * 物流经理导出订单报表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 导出文件
     */
    @GetMapping("/reports/orders/export")
    public ResponseResult<?> exportOrderReport(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        // 简单返回成功，实际项目中需要实现文件导出逻辑
        return ResponseResult.success("报表导出成功");
    }

    /**
     * 物流经理导出配送效率报表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 导出文件
     */
    @GetMapping("/reports/delivery-efficiency/export")
    public ResponseResult<?> exportDeliveryEfficiencyReport(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        // 简单返回成功，实际项目中需要实现文件导出逻辑
        return ResponseResult.success("报表导出成功");
    }

    /**
     * 物流经理获取配送员列表
     * @return 配送员列表
     */
    @GetMapping("/delivery/men")
    public ResponseResult<?> getDeliveryMen() {
        // 查询所有配送员
        List<User> deliveryMen = userRepository.findByRole("delivery");

        // 转换为响应格式
        List<Map<String, Object>> result = deliveryMen.stream().map(deliveryMan -> {
            // 统计配送员的任务数量
            long pendingTasks = deliveryTaskRepository.countByDeliveryManIdAndStatus(deliveryMan.getId(), "PENDING") + 
                              deliveryTaskRepository.countByDeliveryManIdAndStatus(deliveryMan.getId(), "ASSIGNED");
            long deliveringTasks = deliveryTaskRepository.countByDeliveryManIdAndStatus(deliveryMan.getId(), "DELIVERING");
            long completedTasks = deliveryTaskRepository.countByDeliveryManIdAndStatus(deliveryMan.getId(), "COMPLETED");
            
            // 动态计算工作状态
            String workStatus = "AVAILABLE";
            if (deliveringTasks > 0) {
                workStatus = "DELIVERING";
            }
            
            Map<String, Object> deliveryMap = new HashMap<>();
            deliveryMap.put("deliveryManId", deliveryMan.getId());
            deliveryMap.put("name", deliveryMan.getUsername());
            deliveryMap.put("phone", deliveryMan.getPhone());
            deliveryMap.put("status", workStatus); // 动态计算的工作状态
            deliveryMap.put("pendingTasks", pendingTasks);
            deliveryMap.put("completedToday", completedTasks);
            return deliveryMap;
        }).collect(Collectors.toList());

        return ResponseResult.success("成功", result);
    }

    /**
     * 物流经理获取配送员列表（兼容旧端点）
     * @return 配送员列表
     */
    @GetMapping("/delivery-men")
    public ResponseResult<?> getDeliveryMenOld() {
        return getDeliveryMen();
    }
    
    /**
     * 物流经理查看配送员详情
     * @param deliveryManId 配送员ID
     * @return 配送员详情
     */
    @GetMapping("/delivery/men/{deliveryManId}")
    public ResponseResult<?> getDeliveryManDetail(@PathVariable Long deliveryManId) {
        // 查询配送员
        User deliveryMan = userRepository.findById(deliveryManId)
                .filter(u -> "delivery".equals(u.getRole()))
                .orElseThrow(() -> new RuntimeException("配送员不存在或无权限访问"));
        
        // 统计配送员的任务数量
        long pendingTasks = deliveryTaskRepository.countByDeliveryManIdAndStatus(deliveryManId, "PENDING") + 
                          deliveryTaskRepository.countByDeliveryManIdAndStatus(deliveryManId, "ASSIGNED");
        long deliveringTasks = deliveryTaskRepository.countByDeliveryManIdAndStatus(deliveryManId, "DELIVERING");
        long completedTasks = deliveryTaskRepository.countByDeliveryManIdAndStatus(deliveryManId, "COMPLETED");
        
        // 动态计算工作状态
        String workStatus = "AVAILABLE";
        if (deliveringTasks > 0) {
            workStatus = "DELIVERING";
        }
        
        // 转换为响应格式
        Map<String, Object> deliveryManMap = new HashMap<>();
        deliveryManMap.put("deliveryManId", deliveryMan.getId());
        deliveryManMap.put("name", deliveryMan.getUsername());
        deliveryManMap.put("phone", deliveryMan.getPhone());
        deliveryManMap.put("email", deliveryMan.getEmail());
        deliveryManMap.put("status", workStatus); // 动态计算的工作状态
        deliveryManMap.put("pendingTasks", pendingTasks);
        deliveryManMap.put("deliveringTasks", deliveringTasks);
        deliveryManMap.put("completedTasks", completedTasks);
        deliveryManMap.put("totalTasks", pendingTasks + deliveringTasks + completedTasks);
        deliveryManMap.put("createTime", deliveryMan.getCreateTime());
        
        return ResponseResult.success("成功", deliveryManMap);
    }

    /**
     * 物流经理查看订单详情
     * @param orderId 订单ID
     * @return 订单详情
     */
    @GetMapping("/orders/{orderId}")
    public ResponseResult<?> getOrderDetail(@PathVariable Long orderId) {
        // 查询订单，物流经理可以查看所有订单
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在或无权限访问"));

        // 查询订单商品
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        // 转换为响应格式
        Map<String, Object> orderMap = new HashMap<>();
        orderMap.put("orderId", order.getId());
        orderMap.put("orderNumber", order.getOrderNumber());
        orderMap.put("customerId", order.getCustomerId());
        orderMap.put("customerName", order.getCustomerName());
        orderMap.put("createTime", order.getCreateTime());
        orderMap.put("deliveryMethod", order.getDeliveryMethod());
        orderMap.put("status", order.getStatus());
        orderMap.put("totalAmount", order.getTotalAmount());
        orderMap.put("receiverName", order.getReceiverName());
        orderMap.put("receiverPhone", order.getReceiverPhone());
        orderMap.put("receiverAddress", order.getReceiverAddress());
        orderMap.put("remark", order.getRemark());

        // 转换订单商品数据
        List<Map<String, Object>> items = orderItems.stream().map(orderItem -> {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("productName", orderItem.getProductName());
            itemMap.put("productCode", orderItem.getProductCode());
            itemMap.put("quantity", orderItem.getQuantity());
            itemMap.put("price", orderItem.getPrice());
            
            // 查询商品当前库存
            Product product = productRepository.findById(orderItem.getProductId())
                    .orElseThrow(() -> new RuntimeException("商品不存在"));
            itemMap.put("stock", product.getStock());
            
            return itemMap;
        }).collect(Collectors.<Map<String, Object>>toList());
        orderMap.put("items", items);

        return ResponseResult.success("成功", orderMap);
    }
}