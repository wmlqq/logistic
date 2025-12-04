package com.software.logistic.controller;

import com.software.logistic.common.ResponseResult;
import com.software.logistic.dto.AddressDTO;
import com.software.logistic.entity.Address;
import com.software.logistic.entity.Order;
import com.software.logistic.entity.OrderItem;
import com.software.logistic.entity.Product;
import com.software.logistic.entity.User;
import com.software.logistic.repository.AddressRepository;
import com.software.logistic.repository.OrderItemRepository;
import com.software.logistic.repository.OrderRepository;
import com.software.logistic.repository.ProductRepository;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    @Autowired
    private OperationLogService operationLogService;


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AddressRepository addressRepository;

    /**
     * 客户仪表盘统计
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
            stats.put("totalOrders", 0);
            stats.put("pendingOrders", 0);
            stats.put("shippingOrders", 0);
            stats.put("completedOrders", 0);
            return ResponseResult.success("成功", stats);
        }
        
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("用户不存在"));

        // 统计订单数量
        long totalOrders = orderRepository.countByCustomerId(user.getId());
        long pendingOrders = orderRepository.countByCustomerIdAndStatus(user.getId(), "PENDING");
        long shippingOrders = orderRepository.countByCustomerIdAndStatus(user.getId(), "DELIVERING");
        long completedOrders = orderRepository.countByCustomerIdAndStatus(user.getId(), "COMPLETED");

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", totalOrders);
        stats.put("pendingOrders", pendingOrders);
        stats.put("shippingOrders", shippingOrders);
        stats.put("completedOrders", completedOrders);

        return ResponseResult.success("成功", stats);
    }

    /**
     * 客户最近订单
     * @return 最近订单列表
     */
    @GetMapping("/orders/recent")
    public ResponseResult<?> getRecentOrders() {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        // 检查是否为匿名用户
        if ("anonymousUser".equals(username)) {
            // 匿名用户返回空列表
            return ResponseResult.success("成功", Collections.emptyList());
        }
        
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("用户不存在"));

        // 查询最近5个订单
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Order> orderPage = orderRepository.findByCustomerId(user.getId(), pageable);

        // 转换为响应格式
        List<Map<String, Object>> recentOrders = orderPage.getContent().stream().map(order -> {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("orderId", order.getId());
            orderMap.put("orderNumber", order.getOrderNumber());
            orderMap.put("createTime", order.getCreateTime());
            orderMap.put("deliveryMethod", order.getDeliveryMethod());
            orderMap.put("status", order.getStatus());
            return orderMap;
        }).collect(Collectors.toList());

        return ResponseResult.success("成功", recentOrders);
    }

    /**
     * 客户订单列表
     * @param page 页码
     * @param size 每页条数
     * @param status 订单状态
     * @param orderNumber 订单编号
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
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        // 构建查询条件
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Order> orderPage;

        if (status != null) {
            orderPage = orderRepository.findByCustomerIdAndStatus(user.getId(), status, pageable);
        } else if (orderNumber != null) {
            orderPage = orderRepository.findByCustomerIdAndOrderNumberContaining(user.getId(), orderNumber, pageable);
        } else {
            orderPage = orderRepository.findByCustomerId(user.getId(), pageable);
        }

        // 转换为响应格式
        List<Map<String, Object>> orders = orderPage.getContent().stream().map(order -> {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("orderId", order.getId());
            orderMap.put("orderNumber", order.getOrderNumber());
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
        result.put("total", orderPage.getTotalElements());

        return ResponseResult.success("成功", result);
    }

    /**
     * 客户订单详情
     * @param orderId 订单ID
     * @return 订单详情
     */
    @GetMapping("/orders/{orderId}")
    public ResponseResult<?> getOrderDetail(@PathVariable Long orderId) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        // 查询订单
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getCustomerId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("订单不存在或无权限访问"));

        // 构建响应数据
        Map<String, Object> orderDetail = new HashMap<>();
        orderDetail.put("orderId", order.getId());
        orderDetail.put("orderNumber", order.getOrderNumber());
        orderDetail.put("createTime", order.getCreateTime());
        orderDetail.put("deliveryMethod", order.getDeliveryMethod());
        orderDetail.put("status", order.getStatus());
        orderDetail.put("receiverName", order.getReceiverName());
        orderDetail.put("receiverPhone", order.getReceiverPhone());
        orderDetail.put("receiverAddress", order.getReceiverAddress());
        orderDetail.put("totalAmount", order.getTotalAmount());

        // 查询订单商品
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        // 转换订单商品数据
        List<Map<String, Object>> items = orderItems.stream().map(orderItem -> {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("productName", orderItem.getProductName());
            itemMap.put("productCode", orderItem.getProductCode());
            itemMap.put("quantity", orderItem.getQuantity());
            itemMap.put("price", orderItem.getPrice());
            return itemMap;
        }).collect(Collectors.<Map<String, Object>>toList());
        orderDetail.put("items", items);

        // 模拟配送状态（实际项目中需要关联配送状态表）
        List<Map<String, Object>> deliveryStatus = new ArrayList<>();
        Map<String, Object> status1 = new HashMap<>();
        status1.put("status", "PENDING");
        status1.put("time", order.getCreateTime());
        status1.put("description", "订单已创建");
        deliveryStatus.add(status1);

        if ("DELIVERING".equals(order.getStatus()) || "COMPLETED".equals(order.getStatus())) {
            Map<String, Object> status2 = new HashMap<>();
            status2.put("status", "DELIVERING");
            status2.put("time", order.getUpdateTime());
            status2.put("description", "订单已发货");
            deliveryStatus.add(status2);
        }

        if ("COMPLETED".equals(order.getStatus())) {
            Map<String, Object> status3 = new HashMap<>();
            status3.put("status", "COMPLETED");
            status3.put("time", order.getUpdateTime());
            status3.put("description", "订单已完成");
            deliveryStatus.add(status3);
        }

        orderDetail.put("deliveryStatus", deliveryStatus);

        return ResponseResult.success("成功", orderDetail);
    }

    /**
     * 客户创建订单
     * @param orderData 订单数据
     * @return 创建结果
     */
    @PostMapping("/orders")
    public ResponseResult<?> createOrder(@RequestBody Map<String, Object> orderData, HttpServletRequest request) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        // 解析订单数据
        Long addressId = Long.parseLong(orderData.get("addressId").toString());
        String deliveryMethod = (String) orderData.get("deliveryMethod");
        List<Map<String, Object>> items = (List<Map<String, Object>>) orderData.get("items");
        String expectedDeliveryTimeStr = (String) orderData.get("expectedDeliveryTime");

        // 查询收货地址
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("地址不存在"));

        // 计算总金额
        BigDecimal totalAmount = items.stream()
                .map(item -> {
                    BigDecimal price = new BigDecimal(item.get("price").toString());
                    int quantity = Integer.parseInt(item.get("quantity").toString());
                    return price.multiply(BigDecimal.valueOf(quantity));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 验证并处理期望配送时间
        Date expectedDeliveryTime = null;
        if (expectedDeliveryTimeStr != null && !expectedDeliveryTimeStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                expectedDeliveryTime = sdf.parse(expectedDeliveryTimeStr);
                
                // 验证期望配送时间必须晚于当前时间
                Date currentTime = new Date();
                if (expectedDeliveryTime.before(currentTime)) {
                    throw new RuntimeException("期望配送时间必须晚于当前时间");
                }
            } catch (Exception e) {
                throw new RuntimeException("时间格式错误，应为yyyy-MM-dd");
            }
        }

        // 创建订单
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setCustomerId(user.getId());
        order.setCustomerName(user.getUsername());
        order.setReceiverName(address.getConsigneeName());
        order.setReceiverPhone(address.getConsigneePhone());
        order.setReceiverAddress(address.getProvince() + address.getCity() + address.getDistrict() + address.getDetailAddress());
        order.setDeliveryMethod(deliveryMethod);
        order.setTotalAmount(totalAmount);
        order.setStatus("PENDING");
        
        // 如果期望配送时间不为空，设置到订单中
        if (expectedDeliveryTime != null) {
            order.setExpectedDeliveryTime(expectedDeliveryTime);
        }

        Order savedOrder = orderRepository.save(order);

        // 创建订单商品
        for (Map<String, Object> item : items) {
            Long productId = Long.parseLong(item.get("productId").toString());
            int quantity = Integer.parseInt(item.get("quantity").toString());
            BigDecimal price = new BigDecimal(item.get("price").toString());

            // 查询商品信息
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("商品不存在"));

            // 创建订单商品
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(savedOrder.getId());
            orderItem.setProductId(productId);
            orderItem.setProductName(product.getProductName());
            orderItem.setProductCode(product.getProductCode());
            orderItem.setQuantity(quantity);
            orderItem.setPrice(price);

            orderItemRepository.save(orderItem);
        }

        // 记录创建订单日志
        String ipAddress = request.getRemoteAddr();
        operationLogService.logOperation(
            user.getId(),
            user.getUsername(),
            user.getRole(),
            "CREATE_ORDER",
            "客户创建订单，订单号：" + savedOrder.getOrderNumber(),
            ipAddress
        );

        // 构建响应数据
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", savedOrder.getId());
        result.put("orderNumber", savedOrder.getOrderNumber());

        return ResponseResult.success("订单创建成功", result);
    }

    /**
     * 客户取消订单
     * @param orderId 订单ID
     * @return 取消结果
     */
    @PutMapping("/orders/{orderId}/cancel")
    public ResponseResult<?> cancelOrder(@PathVariable Long orderId, HttpServletRequest request) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        // 查询订单
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getCustomerId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("订单不存在或无权限访问"));

        // 检查订单状态
        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("只有待处理的订单才能取消");
        }

        // 取消订单
        order.setStatus("CANCELLED");
        orderRepository.save(order);
        
        // 记录取消订单日志
        String ipAddress = request.getRemoteAddr();
        operationLogService.logOperation(
            user.getId(),
            user.getUsername(),
            user.getRole(),
            "CANCEL_ORDER",
            "客户取消订单，订单号：" + order.getOrderNumber(),
            ipAddress
        );

        return ResponseResult.success("订单取消成功");
    }

    /**
     * 客户收货地址列表
     * @return 地址列表
     */
    @GetMapping("/addresses")
    public ResponseResult<?> getAddresses() {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        try {
            // 查询该用户的所有地址
            List<Address> addresses = addressRepository.findByUserId(user.getId());

            // 转换为前端需要的格式
            List<Map<String, Object>> addressList = addresses.stream()
                    .map(address -> {
                        Map<String, Object> addressMap = new HashMap<>();
                        addressMap.put("addressId", address.getId());
                        addressMap.put("receiverName", address.getConsigneeName());
                        addressMap.put("receiverPhone", address.getConsigneePhone());
                        addressMap.put("receiverAddress", address.getProvince() + address.getCity() + address.getDistrict() + address.getDetailAddress());
                        addressMap.put("isDefault", address.getIsDefault());
                        return addressMap;
                    })
                    .collect(Collectors.toList());

            return ResponseResult.success(addressList);
        } catch (Exception e) {
            return ResponseResult.error("加载地址列表失败");
        }
    }

    /**
     * 客户添加收货地址
     * @param addressData 地址数据
     * @return 添加结果
     */
    @PostMapping("/addresses")
    public ResponseResult<?> addAddress(@RequestBody Map<String, Object> addressData, HttpServletRequest request) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        // 解析地址数据
        String receiverName = (String) addressData.get("receiverName");
        String receiverPhone = (String) addressData.get("receiverPhone");
        String province = (String) addressData.get("province");
        String city = (String) addressData.get("city");
        String district = (String) addressData.get("district");
        String detailAddress = (String) addressData.get("detailAddress");
        Boolean isDefault = (Boolean) addressData.get("isDefault");

        // 如果设置为默认地址，将其他地址设为非默认
        if (isDefault != null && isDefault) {
            addressRepository.updateIsDefaultByUserId(false, user.getId());
        }

        // 创建地址
        Address address = new Address();
        address.setUserId(user.getId());
        address.setConsigneeName(receiverName);
        address.setConsigneePhone(receiverPhone);
        address.setProvince(province);
        address.setCity(city);
        address.setDistrict(district);
        address.setDetailAddress(detailAddress);
        address.setIsDefault(isDefault != null ? isDefault : false);

        Address savedAddress = addressRepository.save(address);
        
        // 记录添加地址日志
        String ipAddress = request.getRemoteAddr();
        operationLogService.logOperation(
            user.getId(),
            user.getUsername(),
            user.getRole(),
            "ADD_ADDRESS",
            "客户添加收货地址",
            ipAddress
        );

        // 构建响应数据
        Map<String, Object> result = new HashMap<>();
        result.put("addressId", savedAddress.getId());

        return ResponseResult.success("地址添加成功", result);
    }

    /**
     * 客户更新收货地址
     * @param addressId 地址ID
     * @param addressData 地址数据
     * @return 更新结果
     */
    @PutMapping("/addresses/{addressId}")
    public ResponseResult<?> updateAddress(@PathVariable Long addressId, @RequestBody Map<String, Object> addressData, HttpServletRequest request) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        // 查询地址
        Address address = addressRepository.findById(addressId)
                .filter(a -> a.getUserId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("地址不存在或无权限访问"));

        // 解析地址数据
        String receiverName = (String) addressData.get("receiverName");
        String receiverPhone = (String) addressData.get("receiverPhone");
        String province = (String) addressData.get("province");
        String city = (String) addressData.get("city");
        String district = (String) addressData.get("district");
        String detailAddress = (String) addressData.get("detailAddress");
        Boolean isDefault = (Boolean) addressData.get("isDefault");

        // 如果设置为默认地址，将其他地址设为非默认
        if (isDefault != null && isDefault) {
            addressRepository.updateIsDefaultByUserId(false, user.getId());
        }

        // 更新地址
        if (receiverName != null) address.setConsigneeName(receiverName);
        if (receiverPhone != null) address.setConsigneePhone(receiverPhone);
        if (province != null) address.setProvince(province);
        if (city != null) address.setCity(city);
        if (district != null) address.setDistrict(district);
        if (detailAddress != null) address.setDetailAddress(detailAddress);
        if (isDefault != null) address.setIsDefault(isDefault);

        addressRepository.save(address);
        
        // 记录更新地址日志
        String ipAddress = request.getRemoteAddr();
        operationLogService.logOperation(
            user.getId(),
            user.getUsername(),
            user.getRole(),
            "UPDATE_ADDRESS",
            "客户更新收货地址",
            ipAddress
        );

        return ResponseResult.success("地址更新成功");
    }

    /**
     * 客户删除收货地址
     * @param addressId 地址ID
     * @return 删除结果
     */
    @DeleteMapping("/addresses/{addressId}")
    public ResponseResult<?> deleteAddress(@PathVariable Long addressId, HttpServletRequest request) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        // 查询地址
        Address address = addressRepository.findById(addressId)
                .filter(a -> a.getUserId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("地址不存在或无权限访问"));

        // 删除地址
        addressRepository.delete(address);
        
        // 记录删除地址日志
        String ipAddress = request.getRemoteAddr();
        operationLogService.logOperation(
            user.getId(),
            user.getUsername(),
            user.getRole(),
            "DELETE_ADDRESS",
            "客户删除收货地址",
            ipAddress
        );

        return ResponseResult.success("地址删除成功");
    }

    /**
     * 客户获取商品列表
     * @return 商品列表
     */
    @GetMapping("/products")
    public ResponseResult<?> getProducts() {
        // 查询所有商品
        List<Product> products = productRepository.findAll();

        // 转换为响应格式
        List<Map<String, Object>> result = products.stream().map(product -> {
            Map<String, Object> productMap = new HashMap<>();
            productMap.put("productId", product.getId());
            productMap.put("productName", product.getProductName());
            productMap.put("productCode", product.getProductCode());
            productMap.put("specification", product.getSpecification());
            productMap.put("unit", product.getUnit());
            productMap.put("price", product.getPrice());
            productMap.put("remark", product.getRemark());
            productMap.put("stock", product.getStock());
            return productMap;
        }).collect(Collectors.toList());

        return ResponseResult.success("成功", result);
    }

    /**
     * 生成订单编号
     * @return 订单编号
     */
    private String generateOrderNumber() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String dateStr = sdf.format(new Date());
        String randomStr = String.format("%04d", new Random().nextInt(10000));
        return "ORD" + dateStr + randomStr;
    }
}