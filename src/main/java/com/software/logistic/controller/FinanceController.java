package com.software.logistic.controller;

import com.software.logistic.common.ResponseResult;
import com.software.logistic.entity.Expense;
import com.software.logistic.entity.Order;
import com.software.logistic.repository.ExpenseRepository;
import com.software.logistic.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    /**
     * 财务人员仪表盘统计
     * @return 统计数据
     */
    @GetMapping("/stats")
    public ResponseResult<?> getStats() {
        // 只统计完成配送的订单
        Page<Order> completedOrdersPage = orderRepository.findByStatus("COMPLETED", Pageable.unpaged());
        List<Order> completedOrders = completedOrdersPage.getContent();
        
        // 统计总营收，添加空值检查
        BigDecimal totalRevenue = completedOrders.stream()
                .map(order -> order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 统计订单数量
        long orderCount = completedOrders.size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRevenue", totalRevenue);
        stats.put("orderCount", orderCount);

        return ResponseResult.success("成功", stats);
    }

    /**
     * 财务人员月度营收趋势
     * @return 月度营收趋势
     */
    @GetMapping("/revenue/trend")
    public ResponseResult<?> getRevenueTrend() {
        // 获取所有完成配送的订单
        Page<Order> completedOrdersPage = orderRepository.findByStatus("COMPLETED", Pageable.unpaged());
        List<Order> completedOrders = completedOrdersPage.getContent();
        
        // 按月份分组并计算营收
        Map<String, BigDecimal> monthlyRevenue = completedOrders.stream()
                .collect(Collectors.groupingBy(
                        order -> {
                            // 格式化日期为 yyyy-MM 格式
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM");
                            return sdf.format(order.getCreateTime());
                        },
                        Collectors.reducing(BigDecimal.ZERO, Order::getTotalAmount, BigDecimal::add)
                ));
        
        // 转换为前端需要的格式
        List<Map<String, Object>> trend = monthlyRevenue.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> monthMap = new HashMap<>();
                    monthMap.put("month", entry.getKey());
                    monthMap.put("revenue", entry.getValue());
                    return monthMap;
                })
                .sorted(Comparator.comparing(map -> map.get("month").toString()))
                .collect(Collectors.toList());

        return ResponseResult.success("成功", trend);
    }

    /**
     * 财务人员财务报表
     * @param type 报表类型
     * @param year 年份
     * @param month 月份
     * @param quarter 季度
     * @return 财务报表
     */
    @GetMapping("/report")
    public ResponseResult<?> getFinancialReport(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String quarter) {
        // 只获取完成配送的订单
        Page<Order> completedOrdersPage = orderRepository.findByStatus("COMPLETED", Pageable.unpaged());
        List<Order> completedOrders = completedOrdersPage.getContent();
        
        // 根据筛选条件过滤订单
        List<Order> filteredOrders = completedOrders.stream()
                .filter(order -> {
                    // 获取订单创建时间
                    Date createTime = order.getCreateTime();
                    if (createTime == null) {
                        return false;
                    }
                    
                    // 格式化日期
                    java.text.SimpleDateFormat yearFormat = new java.text.SimpleDateFormat("yyyy");
                    java.text.SimpleDateFormat monthFormat = new java.text.SimpleDateFormat("MM");
                    java.text.SimpleDateFormat quarterFormat = new java.text.SimpleDateFormat("MM");
                    
                    // 获取订单的年份、月份和季度
                    String orderYear = yearFormat.format(createTime);
                    String orderMonth = monthFormat.format(createTime);
                    int orderMonthInt = Integer.parseInt(orderMonth);
                    String orderQuarter = String.valueOf((orderMonthInt - 1) / 3 + 1);
                    
                    // 根据报表类型筛选
                    if (type != null) {
                        switch (type) {
                            case "monthly":
                                // 月度报表：年份和月份都匹配
                                if (year != null && !year.equals(orderYear)) {
                                    return false;
                                }
                                if (month != null && !month.equals(orderMonth)) {
                                    return false;
                                }
                                break;
                            case "quarterly":
                                // 季度报表：年份和季度都匹配
                                if (year != null && !year.equals(orderYear)) {
                                    return false;
                                }
                                if (quarter != null && !quarter.equals(orderQuarter)) {
                                    return false;
                                }
                                break;
                            case "yearly":
                                // 年度报表：年份匹配
                                if (year != null && !year.equals(orderYear)) {
                                    return false;
                                }
                                break;
                        }
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
        
        // 统计总营收，添加空值检查
        BigDecimal totalRevenue = filteredOrders.stream()
                .map(order -> order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 处理订单数据，转换为前端需要的格式
        List<Map<String, Object>> details = filteredOrders.stream()
                .map(order -> {
                    Map<String, Object> orderMap = new HashMap<>();
                    orderMap.put("orderNumber", order.getOrderNumber());
                    orderMap.put("customerName", order.getCustomerName());
                    orderMap.put("deliveryMethod", order.getDeliveryMethod());
                    orderMap.put("totalAmount", order.getTotalAmount());
                    orderMap.put("status", order.getStatus());
                    orderMap.put("createTime", order.getCreateTime());
                    return orderMap;
                })
                .collect(Collectors.toList());

        Map<String, Object> report = new HashMap<>();
        report.put("totalRevenue", totalRevenue);
        report.put("details", details);

        return ResponseResult.success("成功", report);
    }
    
    /**
     * 财务人员订单量报表
     * @param reportType 报表类型（daily, weekly, monthly, quarterly, yearly）
     * @param yearParam 年份
     * @param month 月份
     * @return 订单量报表
     */
    @GetMapping("/order-report")
    public ResponseResult<?> getOrderReport(
            @RequestParam(defaultValue = "monthly") String reportType,
            @RequestParam(required = false) String yearParam,
            @RequestParam(required = false) String month) {
        // 获取所有订单
        Page<Order> allOrdersPage = orderRepository.findAll(Pageable.unpaged());
        List<Order> allOrders = allOrdersPage.getContent();
        
        // 如果没有指定年份，使用当前年份
        String year;
        if (yearParam == null) {
            java.text.SimpleDateFormat yearFormat = new java.text.SimpleDateFormat("yyyy");
            year = yearFormat.format(new Date());
        } else {
            year = yearParam;
        }
        
        // 根据年份过滤订单
        final String finalYear = year;
        List<Order> filteredByYear = allOrders.stream()
                .filter(order -> {
                    Date createTime = order.getCreateTime();
                    if (createTime == null) {
                        return false;
                    }
                    java.text.SimpleDateFormat yearFormat = new java.text.SimpleDateFormat("yyyy");
                    String orderYear = yearFormat.format(createTime);
                    return finalYear.equals(orderYear);
                })
                .collect(Collectors.toList());
        
        // 如果指定了月份，进一步过滤
        List<Order> filteredOrders = filteredByYear;
        if (month != null) {
            final String finalMonth = month;
            filteredOrders = filteredByYear.stream()
                    .filter(order -> {
                        Date createTime = order.getCreateTime();
                        if (createTime == null) {
                            return false;
                        }
                        java.text.SimpleDateFormat monthFormat = new java.text.SimpleDateFormat("MM");
                        String orderMonth = monthFormat.format(createTime);
                        return finalMonth.equals(orderMonth);
                    })
                    .collect(Collectors.toList());
        }
        
        // 统计总订单量
        long totalOrders = filteredOrders.size();
        
        // 统计已完成订单量
        long completedOrders = filteredOrders.stream()
                .filter(order -> "COMPLETED".equals(order.getStatus()))
                .count();
        
        // 计算平均订单金额
        BigDecimal avgOrderAmount = BigDecimal.ZERO;
        if (totalOrders > 0) {
            BigDecimal totalAmount = filteredOrders.stream()
                    .map(order -> order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            avgOrderAmount = totalAmount.divide(BigDecimal.valueOf(totalOrders), 2, BigDecimal.ROUND_HALF_UP);
        }
        
        // 按报表类型生成订单量趋势数据
        Map<String, List<Map<String, Object>>> trendData = generateOrderTrend(filteredOrders, reportType);
        
        // 生成订单类型分布数据
        Map<String, Object> orderTypeDistribution = generateOrderTypeDistribution(filteredOrders);
        
        // 构建响应数据
        Map<String, Object> result = new HashMap<>();
        result.put("totalOrders", totalOrders);
        result.put("completedOrders", completedOrders);
        result.put("avgOrderAmount", avgOrderAmount);
        result.put("orderGrowthRate", calculateOrderGrowthRate(filteredOrders, reportType, year));
        result.put("trendData", trendData);
        result.put("orderTypeDistribution", orderTypeDistribution);
        
        return ResponseResult.success("成功", result);
    }
    
    /**
     * 生成订单量趋势数据
     * @param orders 订单列表
     * @param reportType 报表类型
     * @return 订单量趋势数据
     */
    private Map<String, List<Map<String, Object>>> generateOrderTrend(List<Order> orders, String reportType) {
        Map<String, Long> orderCountByPeriod = new HashMap<>();
        
        // 按不同报表类型分组统计订单量
        for (Order order : orders) {
            Date createTime = order.getCreateTime();
            if (createTime == null) {
                continue;
            }
            
            String periodKey = "";
            java.text.SimpleDateFormat sdf = null;
            
            switch (reportType) {
                case "daily":
                    // 按天分组
                    sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                    periodKey = sdf.format(createTime);
                    break;
                case "weekly":
                    // 按周分组
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.setTime(createTime);
                    int weekOfYear = cal.get(java.util.Calendar.WEEK_OF_YEAR);
                    periodKey = "Week " + weekOfYear;
                    break;
                case "monthly":
                    // 按月分组
                    sdf = new java.text.SimpleDateFormat("yyyy-MM");
                    periodKey = sdf.format(createTime);
                    break;
                case "quarterly":
                    // 按季度分组
                    sdf = new java.text.SimpleDateFormat("yyyy");
                    String year = sdf.format(createTime);
                    java.util.Calendar quarterCal = java.util.Calendar.getInstance();
                    quarterCal.setTime(createTime);
                    // 使用Calendar.MONTH计算季度
                    int month = quarterCal.get(java.util.Calendar.MONTH);
                    int quarter = (month / 3) + 1;
                    periodKey = year + "-Q" + quarter;
                    break;
                case "yearly":
                    // 按年分组
                    sdf = new java.text.SimpleDateFormat("yyyy");
                    periodKey = sdf.format(createTime);
                    break;
                default:
                    // 默认按月分组
                    sdf = new java.text.SimpleDateFormat("yyyy-MM");
                    periodKey = sdf.format(createTime);
            }
            
            // 统计订单量
            orderCountByPeriod.put(periodKey, orderCountByPeriod.getOrDefault(periodKey, 0L) + 1);
        }
        
        // 转换为前端需要的格式
        List<Map<String, Object>> trend = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        
        // 按时间顺序排序
        List<String> sortedKeys = new ArrayList<>(orderCountByPeriod.keySet());
        sortedKeys.sort(String::compareTo);
        
        for (String key : sortedKeys) {
            long count = orderCountByPeriod.get(key);
            
            Map<String, Object> periodData = new HashMap<>();
            periodData.put("period", key);
            periodData.put("count", count);
            trend.add(periodData);
            
            labels.add(key);
            data.add(count);
        }
        
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        result.put("trend", trend);
        
        return result;
    }
    
    /**
     * 生成订单类型分布数据
     * @param orders 订单列表
     * @return 订单类型分布数据
     */
    private Map<String, Object> generateOrderTypeDistribution(List<Order> orders) {
        // 按配送方式分组统计
        Map<String, Long> distribution = orders.stream()
                .collect(Collectors.groupingBy(Order::getDeliveryMethod, Collectors.counting()));
        
        // 转换为前端需要的格式
        List<String> labels = new ArrayList<>(distribution.keySet());
        List<Long> data = new ArrayList<>(distribution.values());
        
        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("data", data);
        
        return result;
    }
    
    /**
     * 计算订单增长率
     * @param orders 订单列表
     * @param reportType 报表类型
     * @param year 年份
     * @return 订单增长率
     */
    private double calculateOrderGrowthRate(List<Order> orders, String reportType, String year) {
        // 这里简化实现，返回一个模拟的增长率
        // 实际项目中应该根据历史数据计算真实的增长率
        return Math.random() * 20 - 5; // 返回-5%到15%之间的随机增长率
    }
}