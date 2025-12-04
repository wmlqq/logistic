package com.software.logistic.controller;

import com.software.logistic.common.ResponseResult;
import com.software.logistic.entity.Location;
import com.software.logistic.entity.Product;
import com.software.logistic.entity.StockChange;
import com.software.logistic.entity.User;
import com.software.logistic.repository.LocationRepository;
import com.software.logistic.repository.ProductRepository;
import com.software.logistic.repository.StockChangeRepository;
import com.software.logistic.repository.UserRepository;
import com.software.logistic.service.OperationLogService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/warehouse")
public class WarehouseController {

    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private LocationRepository locationRepository;
    
    @Autowired
    private StockChangeRepository stockChangeRepository;
    
    @Autowired
    private OperationLogService operationLogService;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * 仓库管理员仪表盘统计
     * @return 统计数据
     */
    @GetMapping("/stats")
    public ResponseResult<?> getStats() {
        // 统计商品数量（只统计启用状态的商品）
        long totalProducts = productRepository.countByStatus(1);
        long lowStockProducts = productRepository.countByStockLessThanAlertThreshold();
        
        // 统计总库存（只统计启用状态的商品）
        int totalStock = productRepository.findByStatus(1).stream()
                .mapToInt(Product::getStock)
                .sum();
        
        // 计算仓库容量利用率
        List<Location> locations = locationRepository.findAll();
        int totalCapacity = locations.stream()
                .mapToInt(Location::getCapacity)
                .sum();
        int totalUsed = locations.stream()
                .mapToInt(Location::getUsed)
                .sum();
        
        // 计算利用率，避免除以零
        int warehouseCapacity = totalCapacity > 0 ? Math.round((float) totalUsed / totalCapacity * 100) : 0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProducts", totalProducts);
        stats.put("lowStockProducts", lowStockProducts);
        stats.put("totalStock", totalStock);
        stats.put("warehouseCapacity", warehouseCapacity);

        return ResponseResult.success("成功", stats);
    }

    /**
     * 仓库管理员最近库存变动
     * @return 最近库存变动列表
     */
    @GetMapping("/stock/changes")
    public ResponseResult<?> getStockChanges() {
        // 从数据库获取最近的库存变动，按时间倒序排序
        List<StockChange> stockChanges = stockChangeRepository.findAll(Sort.by(Sort.Direction.DESC, "changeTime"));
        
        // 转换为响应格式
        List<Map<String, Object>> changes = stockChanges.stream().map(change -> {
            Map<String, Object> changeMap = new HashMap<>();
            changeMap.put("productName", change.getProductName());
            changeMap.put("type", change.getChangeType());
            changeMap.put("quantity", change.getQuantity());
            changeMap.put("changeTime", change.getChangeTime());
            return changeMap;
        }).collect(Collectors.toList());

        return ResponseResult.success("成功", changes);
    }

    /**
     * 仓库管理员低库存预警
     * @return 低库存商品列表
     */
    @GetMapping("/stock/alerts")
    public ResponseResult<?> getStockAlerts() {
        // 查询低库存商品
        List<Product> lowStockProducts = productRepository.findByStockLessThanAlertThreshold();
        List<Product> zeroStockProducts = productRepository.findByStockZero();

        // 合并所有预警商品
        List<Product> allAlertProducts = new ArrayList<>();
        allAlertProducts.addAll(lowStockProducts);
        // 添加库存为0的商品（去重）
        for (Product zeroStockProduct : zeroStockProducts) {
            boolean exists = allAlertProducts.stream().anyMatch(p -> p.getId().equals(zeroStockProduct.getId()));
            if (!exists) {
                allAlertProducts.add(zeroStockProduct);
            }
        }

        // 转换为响应格式
        List<Map<String, Object>> alerts = allAlertProducts.stream().map(product -> {
            Map<String, Object> alertMap = new HashMap<>();
            alertMap.put("productId", product.getId());
            alertMap.put("productName", product.getProductName());
            alertMap.put("productCode", product.getProductCode());
            alertMap.put("currentStock", product.getStock());
            alertMap.put("alertThreshold", product.getAlertThreshold());
            // 添加仓库位置信息
            if (product.getLocation() != null) {
                alertMap.put("warehouseLocation", product.getLocation().getLocationCode());
            } else {
                alertMap.put("warehouseLocation", "");
            }
            // 添加预警级别：库存为0的商品为严重预警
            alertMap.put("alertLevel", product.getStock() == 0 ? "SEVERE" : "NORMAL");
            return alertMap;
        }).collect(Collectors.toList());

        return ResponseResult.success("成功", alerts);
    }

    /**
     * 库存商品列表
     * @param page 页码
     * @param size 每页条数
     * @param keyword 搜索关键词
     * @return 商品列表
     */
    @GetMapping("/inventory")
    public ResponseResult<?> getInventory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        // 构建查询条件
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Product> productPage;

        if (keyword != null) {
            productPage = productRepository.findByProductNameContainingOrProductCodeContaining(keyword, keyword, pageable);
        } else {
            productPage = productRepository.findAll(pageable);
        }

        // 转换为响应格式
        List<Map<String, Object>> products = productPage.getContent().stream().map(product -> {
            Map<String, Object> productMap = new HashMap<>();
            productMap.put("productId", product.getId());
            productMap.put("productName", product.getProductName());
            productMap.put("productCode", product.getProductCode());
            productMap.put("specification", product.getSpecification());
            productMap.put("unit", product.getUnit());
            productMap.put("price", product.getPrice());
            productMap.put("stock", product.getStock());
            // 获取位置信息
            if (product.getLocation() != null) {
                productMap.put("warehouseLocation", product.getLocation().getLocationCode());
                productMap.put("locationId", product.getLocation().getId());
            } else {
                productMap.put("warehouseLocation", "");
                productMap.put("locationId", null);
            }
            productMap.put("alertThreshold", product.getAlertThreshold());
            productMap.put("status", product.getStatus());
            productMap.put("remark", product.getRemark());
            return productMap;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", productPage.getTotalElements());
        result.put("pages", productPage.getTotalPages());
        result.put("current", page);
        result.put("records", products);

        return ResponseResult.success("成功", result);
    }

    /**
     * 添加库存商品
     * @param productData 商品数据
     * @return 添加结果
     */
    @PostMapping("/inventory")
    public ResponseResult<?> addProduct(@RequestBody Product productData, HttpServletRequest request) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 创建商品
        Product product = new Product();
        product.setProductName(productData.getProductName());
        product.setProductCode(productData.getProductCode());
        product.setSpecification(productData.getSpecification());
        product.setUnit(productData.getUnit());
        product.setPrice(productData.getPrice());
        product.setStock(productData.getStock());
        product.setAlertThreshold(productData.getAlertThreshold());
        product.setStatus(productData.getStatus());
        product.setRemark(productData.getRemark());

        // 先检查位置容量，如果关联了位置
        Location location = null;
        if (productData.getLocation() != null) {
            location = locationRepository.findById(productData.getLocation().getId())
                    .orElseThrow(() -> new RuntimeException("位置不存在"));
            
            // 检查位置剩余容量是否足够
            int remainingCapacity = location.getCapacity() - location.getUsed();
            if (remainingCapacity < product.getStock()) {
                throw new RuntimeException("位置剩余容量不足，无法存放该商品");
            }
        }
        
        // 容量检查通过，保存商品
        Product savedProduct = productRepository.save(product);
        
        // 如果关联了位置，更新位置使用量
        if (location != null) {
            // 更新使用量
            location.setUsed(location.getUsed() + product.getStock());
            locationRepository.save(location);
            // 设置商品的位置
            savedProduct.setLocation(location);
            productRepository.save(savedProduct);
        }
        
        // 创建库存变动记录（新增商品）
        StockChange stockChange = new StockChange();
        stockChange.setProductId(savedProduct.getId());
        stockChange.setProductName(savedProduct.getProductName());
        stockChange.setChangeType("IN");
        stockChange.setQuantity(savedProduct.getStock());
        stockChange.setBeforeStock(0); // 新增商品，之前库存为0
        stockChange.setAfterStock(savedProduct.getStock());
        stockChange.setOperatorId(currentUser.getId());
        stockChange.setOperatorName(currentUser.getUsername());
        stockChange.setRemark("新增商品");
        stockChangeRepository.save(stockChange);
        
        // 记录添加商品日志
        String ipAddress = request.getRemoteAddr();
        operationLogService.logOperation(
            currentUser.getId(),
            currentUser.getUsername(),
            currentUser.getRole(),
            "ADD_PRODUCT",
            "仓库管理员添加商品，商品名称：" + product.getProductName() + "，商品编码：" + product.getProductCode(),
            ipAddress
        );

        // 构建响应数据
        Map<String, Object> result = new HashMap<>();
        result.put("productId", savedProduct.getId());

        return ResponseResult.success("商品添加成功", result);
    }

    /**
     * 仓库位置列表
     * @return 仓库位置列表
     */
    @GetMapping("/locations")
    public ResponseResult<?> getLocations() {
        // 从数据库获取所有仓库位置
        List<Location> locationList = locationRepository.findAll();
        
        // 转换为响应格式
        List<Map<String, Object>> locations = locationList.stream().map(location -> {
            Map<String, Object> locationMap = new HashMap<>();
            locationMap.put("locationId", location.getId());
            locationMap.put("locationCode", location.getLocationCode());
            locationMap.put("description", location.getDescription());
            locationMap.put("capacity", location.getCapacity());
            locationMap.put("used", location.getUsed());
            locationMap.put("status", location.getStatus());
            locationMap.put("remark", location.getRemark());
            return locationMap;
        }).collect(Collectors.toList());

        return ResponseResult.success("成功", locations);
    }
    
    /**
     * 添加仓库位置
     * @param locationData 仓库位置数据
     * @return 添加结果
     */
    @PostMapping("/locations")
    public ResponseResult<?> addLocation(@RequestBody Map<String, Object> locationData) {
        // 创建仓库位置实体
        Location location = new Location();
        location.setLocationCode((String) locationData.get("locationCode"));
        location.setDescription((String) locationData.get("description"));
        location.setCapacity(Integer.parseInt(locationData.get("capacity").toString()));
        location.setUsed(0); // 初始使用量为0
        location.setStatus(1); // 默认状态为启用
        location.setRemark((String) locationData.get("remark"));
        location.setCreateTime(new Date());
        location.setUpdateTime(new Date());
        
        // 保存到数据库
        Location savedLocation = locationRepository.save(location);
        
        // 构建响应数据
        Map<String, Object> result = new HashMap<>();
        result.put("locationId", savedLocation.getId());
        
        return ResponseResult.success("仓库位置添加成功", result);
    }

    /**
     * 调整库存
     * @param productId 商品ID
     * @param adjustData 调整数据
     * @return 调整结果
     */
    @PutMapping("/inventory/{productId}/adjust")
    public ResponseResult<?> adjustStock(@PathVariable Long productId, @RequestBody Map<String, Object> adjustData, HttpServletRequest request) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 查询商品
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));

        // 解析调整数据
        int quantity = Integer.parseInt(adjustData.get("quantity").toString());
        String type = (String) adjustData.get("type");
        
        // 记录调整前的库存
        int beforeStock = product.getStock();
        int afterStock = beforeStock;

        // 调整库存
        if ("IN".equals(type)) {
            afterStock = beforeStock + quantity;
        } else if ("OUT".equals(type)) {
            if (beforeStock < quantity) {
                throw new RuntimeException("库存不足");
            }
            afterStock = beforeStock - quantity;
        } else {
            throw new RuntimeException("无效的调整类型");
        }
        
        // 如果商品关联了位置，检查入库时位置剩余容量是否足够
        if (product.getLocation() != null && "IN".equals(type)) {
            Location location = locationRepository.findById(product.getLocation().getId())
                    .orElseThrow(() -> new RuntimeException("位置不存在"));
            
            // 计算位置使用量的变化
            int stockDiff = afterStock - beforeStock;
            // 检查位置剩余容量是否足够
            int remainingCapacity = location.getCapacity() - location.getUsed();
            if (remainingCapacity < stockDiff) {
                throw new RuntimeException("位置剩余容量不足，无法完成入库操作");
            }
        }
        
        // 更新商品库存
        product.setStock(afterStock);
        Product updatedProduct = productRepository.save(product);
        
        // 如果商品关联了位置，更新位置的使用量
        if (updatedProduct.getLocation() != null) {
            Location location = locationRepository.findById(updatedProduct.getLocation().getId())
                    .orElseThrow(() -> new RuntimeException("位置不存在"));
            
            // 计算位置使用量的变化
            int stockDiff = afterStock - beforeStock;
            // 更新位置使用量
            location.setUsed(location.getUsed() + stockDiff);
            locationRepository.save(location);
        }
        
        // 创建库存变动记录
        StockChange stockChange = new StockChange();
        stockChange.setProductId(product.getId());
        stockChange.setProductName(product.getProductName());
        stockChange.setChangeType(type);
        stockChange.setQuantity(quantity);
        stockChange.setBeforeStock(beforeStock);
        stockChange.setAfterStock(afterStock);
        stockChange.setOperatorId(currentUser.getId());
        stockChange.setOperatorName(currentUser.getUsername());
        stockChange.setRemark((String) adjustData.get("remark"));
        stockChangeRepository.save(stockChange);
        
        // 记录库存调整日志
        String ipAddress = request.getRemoteAddr();
        operationLogService.logOperation(
            currentUser.getId(),
            currentUser.getUsername(),
            currentUser.getRole(),
            "ADJUST_STOCK",
            "仓库管理员调整库存，商品名称：" + product.getProductName() + "，调整类型：" + type + "，调整数量：" + quantity,
            ipAddress
        );

        return ResponseResult.success("库存调整成功");
    }
    
    /**
     * 编辑商品状态
     * @param productId 商品ID
     * @param statusData 状态数据
     * @return 编辑结果
     */
    @PutMapping("/inventory/{productId}/status")
    public ResponseResult<?> editProductStatus(@PathVariable Long productId, @RequestBody Map<String, Object> statusData, HttpServletRequest request) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 查询商品
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));
        
        // 解析状态数据
        Integer status = (Integer) statusData.get("status");
        
        // 更新商品状态
        product.setStatus(status);
        productRepository.save(product);
        
        // 记录商品状态修改日志
        String ipAddress = request.getRemoteAddr();
        String statusText = status == 1 ? "启用" : "禁用";
        operationLogService.logOperation(
            currentUser.getId(),
            currentUser.getUsername(),
            currentUser.getRole(),
            "UPDATE_PRODUCT_STATUS",
            "仓库管理员修改商品状态，商品名称：" + product.getProductName() + "，新状态：" + statusText,
            ipAddress
        );
        
        return ResponseResult.success("商品状态修改成功");
    }
    
    /**
     * 获取单个仓库位置详情
     * @param locationId 位置ID
     * @return 位置详情和商品列表
     */
    @GetMapping("/locations/{locationId}")
    public ResponseResult<?> getLocationDetail(@PathVariable Long locationId) {
        // 查询位置
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("位置不存在"));
        
        // 构建响应数据，包含位置信息和商品列表
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("locationId", location.getId());
        locationData.put("locationCode", location.getLocationCode());
        locationData.put("description", location.getDescription());
        locationData.put("capacity", location.getCapacity());
        locationData.put("used", location.getUsed());
        locationData.put("status", location.getStatus());
        locationData.put("remark", location.getRemark());
        
        // 转换商品列表，避免循环引用
        List<Map<String, Object>> productList = location.getProducts().stream().map(product -> {
            Map<String, Object> productMap = new HashMap<>();
            productMap.put("productName", product.getProductName());
            productMap.put("productCode", product.getProductCode());
            productMap.put("stock", product.getStock());
            return productMap;
        }).collect(Collectors.toList());
        
        locationData.put("products", productList);
        
        return ResponseResult.success("成功", locationData);
    }
    
    /**
     * 编辑仓库位置
     * @param locationId 位置ID
     * @param locationData 位置数据
     * @return 编辑结果
     */
    @PutMapping("/locations/{locationId}")
    public ResponseResult<?> editLocation(@PathVariable Long locationId, @RequestBody Map<String, Object> locationData) {
        // 查询位置
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("位置不存在"));
        
        // 更新位置信息
        location.setLocationCode((String) locationData.get("locationCode"));
        location.setDescription((String) locationData.get("description"));
        location.setCapacity(Integer.parseInt(locationData.get("capacity").toString()));
        location.setUpdateTime(new Date());
        
        // 保存更新后的位置
        locationRepository.save(location);
        
        return ResponseResult.success("仓库位置编辑成功");
    }
    
    /**
     * 下载库存导入模板
     * @param response HTTP响应
     */
    @GetMapping("/inventory/export/template")
    public void downloadImportTemplate(HttpServletResponse response) {
        try {
            // 创建Excel工作簿
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("库存导入模板");
            
            // 创建表头行
            XSSFRow headerRow = sheet.createRow(0);
            String[] headers = {"商品名称", "商品编码", "规格", "单位", "价格", "库存", "预警阈值", "状态", "备注", "位置编码"};
            
            // 设置表头样式
            XSSFCellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            
            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // 填充表头
            for (int i = 0; i < headers.length; i++) {
                XSSFCell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.autoSizeColumn(i);
            }
            
            // 创建示例行
            XSSFRow exampleRow = sheet.createRow(1);
            String[] exampleData = {"测试商品", "TEST001", "100g", "袋", "10.00", "100", "20", "1", "测试备注", "A01"};
            
            XSSFCellStyle exampleStyle = workbook.createCellStyle();
            exampleStyle.setAlignment(HorizontalAlignment.CENTER);
            exampleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            exampleStyle.setBorderTop(BorderStyle.THIN);
            exampleStyle.setBorderBottom(BorderStyle.THIN);
            exampleStyle.setBorderLeft(BorderStyle.THIN);
            exampleStyle.setBorderRight(BorderStyle.THIN);
            
            // 填充示例数据
            for (int i = 0; i < exampleData.length; i++) {
                XSSFCell cell = exampleRow.createCell(i);
                cell.setCellValue(exampleData[i]);
                cell.setCellStyle(exampleStyle);
            }
            
            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=库存导入模板.xlsx");
            
            // 写入响应流
            workbook.write(response.getOutputStream());
            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 导入库存数据
     * @param file 上传的Excel文件
     * @return 导入结果
     */
    @PostMapping("/inventory/import")
    public ResponseResult<?> importInventory(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            // 获取当前登录用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("用户不存在"));
            
            // 创建Excel工作簿
            XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
            XSSFSheet sheet = workbook.getSheetAt(0);
            
            // 跳过表头行
            int rowStart = 1;
            int rowEnd = sheet.getLastRowNum();
            
            // 导入结果统计
            int successCount = 0;
            int failCount = 0;
            List<String> errorMessages = new ArrayList<>();
            
            for (int i = rowStart; i <= rowEnd; i++) {
                XSSFRow row = sheet.getRow(i);
                if (row == null) continue;
                
                try {
                    // 读取单元格数据
                    String productName = getCellValue(row.getCell(0));
                    String productCode = getCellValue(row.getCell(1));
                    String specification = getCellValue(row.getCell(2));
                    String unit = getCellValue(row.getCell(3));
                    BigDecimal price = new BigDecimal(getCellValue(row.getCell(4)));
                    Integer stock = Integer.parseInt(getCellValue(row.getCell(5)));
                    Integer alertThreshold = Integer.parseInt(getCellValue(row.getCell(6)));
                    Integer status = Integer.parseInt(getCellValue(row.getCell(7)));
                    String remark = getCellValue(row.getCell(8));
                    String locationCode = getCellValue(row.getCell(9));
                    
                    // 检查商品编码是否已存在
                    if (productRepository.findByProductCode(productCode) != null) {
                        failCount++;
                        errorMessages.add("第" + (i + 1) + "行：商品编码" + productCode + "已存在");
                        continue;
                    }
                    
                    // 创建商品
                    Product product = new Product();
                    product.setProductName(productName);
                    product.setProductCode(productCode);
                    product.setSpecification(specification);
                    product.setUnit(unit);
                    product.setPrice(price);
                    product.setStock(stock);
                    product.setAlertThreshold(alertThreshold);
                    product.setStatus(status);
                    product.setRemark(remark);
                    
                    // 如果指定了位置编码，关联位置
                    if (locationCode != null && !locationCode.isEmpty()) {
                        Optional<Location> locationOptional = locationRepository.findByLocationCode(locationCode);
                        if (locationOptional.isPresent()) {
                            Location location = locationOptional.get();
                            // 检查位置剩余容量是否足够
                            int remainingCapacity = location.getCapacity() - location.getUsed();
                            if (remainingCapacity < stock) {
                                failCount++;
                                errorMessages.add("第" + (i + 1) + "行：位置" + locationCode + "剩余容量不足，无法存放该商品");
                                continue;
                            }
                            
                            // 更新位置使用量
                            location.setUsed(location.getUsed() + stock);
                            locationRepository.save(location);
                            
                            // 设置商品位置
                            product.setLocation(location);
                        } else {
                            failCount++;
                            errorMessages.add("第" + (i + 1) + "行：位置编码" + locationCode + "不存在");
                            continue;
                        }
                    }
                    
                    // 保存商品
                    productRepository.save(product);
                    successCount++;
                    
                    // 创建库存变动记录
                    StockChange stockChange = new StockChange();
                    stockChange.setProductId(product.getId());
                    stockChange.setProductName(product.getProductName());
                    stockChange.setChangeType("IN");
                    stockChange.setQuantity(product.getStock());
                    stockChange.setBeforeStock(0);
                    stockChange.setAfterStock(product.getStock());
                    stockChange.setOperatorId(currentUser.getId());
                    stockChange.setOperatorName(currentUser.getUsername());
                    stockChange.setRemark("导入商品");
                    stockChangeRepository.save(stockChange);
                    
                } catch (Exception e) {
                    failCount++;
                    errorMessages.add("第" + (i + 1) + "行：导入失败 - " + e.getMessage());
                }
            }
            
            // 关闭工作簿
            workbook.close();
            
            // 记录库存导入日志
            String ipAddress = request.getRemoteAddr();
            operationLogService.logOperation(
                currentUser.getId(),
                currentUser.getUsername(),
                currentUser.getRole(),
                "IMPORT_INVENTORY",
                "仓库管理员导入库存数据，成功：" + successCount + "条，失败：" + failCount + "条",
                ipAddress
            );
            
            // 构建导入结果
            Map<String, Object> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("errorMessages", errorMessages);
            
            return ResponseResult.success("导入完成", result);
        } catch (Exception e) {
            return ResponseResult.error("导入失败：" + e.getMessage());
        }
    }
    
    /**
     * 导出库存数据
     * @param format 导出格式 (excel/csv)
     * @param scope 导出范围 (all/low-stock/available)
     * @param response HTTP响应
     */
    @GetMapping("/inventory/export")
    public void exportInventory(@RequestParam String format, @RequestParam String scope, HttpServletResponse response) {
        try {
            // 查询商品数据
            List<Product> products;
            
            if ("low-stock".equals(scope)) {
                // 低库存商品
                products = productRepository.findByStockLessThanAlertThreshold();
            } else if ("available".equals(scope)) {
                // 可用商品
                products = productRepository.findByStatus(1);
            } else {
                // 全部商品
                products = productRepository.findAll();
            }
            
            if ("excel".equals(format)) {
                // 导出为Excel
                exportToExcel(products, response);
            } else if ("csv".equals(format)) {
                // 导出为CSV
                exportToCsv(products, response);
            } else {
                throw new RuntimeException("不支持的导出格式");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 导出为Excel
     * @param products 商品列表
     * @param response HTTP响应
     * @throws Exception 异常
     */
    private void exportToExcel(List<Product> products, HttpServletResponse response) throws Exception {
        // 创建Excel工作簿
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("库存数据");
        
        // 创建表头行
        XSSFRow headerRow = sheet.createRow(0);
        String[] headers = {"商品名称", "商品编码", "规格", "单位", "价格", "库存", "预警阈值", "状态", "备注", "位置编码"};
        
        // 设置表头样式
        XSSFCellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        
        XSSFFont headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        
        // 填充表头
        for (int i = 0; i < headers.length; i++) {
            XSSFCell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.autoSizeColumn(i);
        }
        
        // 填充数据行
        XSSFCellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.CENTER);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            XSSFRow dataRow = sheet.createRow(i + 1);
            
            dataRow.createCell(0).setCellValue(product.getProductName());
            dataRow.createCell(1).setCellValue(product.getProductCode());
            dataRow.createCell(2).setCellValue(product.getSpecification());
            dataRow.createCell(3).setCellValue(product.getUnit());
            dataRow.createCell(4).setCellValue(product.getPrice().toString());
            dataRow.createCell(5).setCellValue(product.getStock());
            dataRow.createCell(6).setCellValue(product.getAlertThreshold());
            dataRow.createCell(7).setCellValue(product.getStatus() == 1 ? "可用" : "不可用");
            dataRow.createCell(8).setCellValue(product.getRemark());
            dataRow.createCell(9).setCellValue(product.getLocation() != null ? product.getLocation().getLocationCode() : "");
            
            // 设置数据行样式
            for (int j = 0; j < headers.length; j++) {
                dataRow.getCell(j).setCellStyle(dataStyle);
            }
        }
        
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=库存数据_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".xlsx");
        
        // 写入响应流
        workbook.write(response.getOutputStream());
        workbook.close();
    }
    
    /**
     * 导出为CSV
     * @param products 商品列表
     * @param response HTTP响应
     * @throws Exception 异常
     */
    private void exportToCsv(List<Product> products, HttpServletResponse response) throws Exception {
        // 设置响应头
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=库存数据_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".csv");
        
        // 创建CSV写入器
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
            // 写入BOM，解决Excel打开CSV中文乱码问题
            writer.write(new String(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF }));
            
            // 写入表头
            writer.println("商品名称,商品编码,规格,单位,价格,库存,预警阈值,状态,备注,位置编码");
            
            // 写入数据行
            for (Product product : products) {
                String locationCode = product.getLocation() != null ? product.getLocation().getLocationCode() : "";
                writer.println(
                    product.getProductName() + "," +
                    product.getProductCode() + "," +
                    product.getSpecification() + "," +
                    product.getUnit() + "," +
                    product.getPrice() + "," +
                    product.getStock() + "," +
                    product.getAlertThreshold() + "," +
                    (product.getStatus() == 1 ? "可用" : "不可用") + "," +
                    product.getRemark() + "," +
                    locationCode
                );
            }
        }
    }
    
    /**
     * 获取Excel单元格值
     * @param cell 单元格
     * @return 单元格值
     */
    private String getCellValue(XSSFCell cell) {
        if (cell == null) {
            return "";
        }
        
        String value = "";
        switch (cell.getCellType()) {
            case STRING:
                value = cell.getStringCellValue();
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    value = new SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue());
                } else {
                    value = String.valueOf(cell.getNumericCellValue());
                    // 去除小数末尾的.0
                    if (value.endsWith(".0")) {
                        value = value.substring(0, value.length() - 2);
                    }
                }
                break;
            case BOOLEAN:
                value = String.valueOf(cell.getBooleanCellValue());
                break;
            case FORMULA:
                value = String.valueOf(cell.getCellFormula());
                break;
            default:
                value = "";
        }
        
        return value != null ? value.trim() : "";
    }
}