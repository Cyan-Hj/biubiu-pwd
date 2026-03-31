package com.biubiu.service;

import com.biubiu.entity.Order;
import com.biubiu.entity.SystemConfig;
import com.biubiu.entity.User;
import com.biubiu.repository.FinancialRecordRepository;
import com.biubiu.repository.OperationLogRepository;
import com.biubiu.repository.OrderRepository;
import com.biubiu.repository.OrderSessionRepository;
import com.biubiu.repository.SystemConfigRepository;
import com.biubiu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCleanupService {

    @Value("${file.upload.path:uploads/}")
    private String uploadPath;

    private final SystemConfigRepository systemConfigRepository;
    private final OrderRepository orderRepository;
    private final OrderSessionRepository orderSessionRepository;
    private final FinancialRecordRepository financialRecordRepository;
    private final OperationLogRepository operationLogRepository;
    private final UserRepository userRepository;

    /**
     * 删除订单关联的截图文件
     */
    private void deleteOrderScreenshots(Order order) {
        deleteScreenshotFile(order.getStartScreenshotUrl());
        deleteScreenshotFile(order.getEndScreenshotUrl());
    }

    /**
     * 删除单个截图文件
     */
    private void deleteScreenshotFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }
        try {
            // 解析文件路径，URL格式为 /uploads/xxx.jpg
            String filename = fileUrl;
            if (fileUrl.startsWith("/uploads/")) {
                filename = fileUrl.substring("/uploads/".length());
            }
            Path filePath = Paths.get(uploadPath, filename);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("已删除截图文件: {}", filePath);
            }
        } catch (Exception e) {
            log.warn("删除截图文件失败: {}, 错误: {}", fileUrl, e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupOldOrders() {
        SystemConfig config = systemConfigRepository.findFirstByOrderByIdAsc().orElse(null);
        
        if (config == null || !Boolean.TRUE.equals(config.getOrderCleanupEnabled())) {
            log.info("订单自动清理功能未启用，跳过清理");
            return;
        }
        
        Integer cleanupDays = config.getOrderCleanupDays();
        if (cleanupDays == null || cleanupDays <= 0) {
            cleanupDays = 30;
        }
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(cleanupDays);
        
        List<Order> ordersToDelete = orderRepository.findByStatusAndCreatedAtBefore(
            Order.Status.COMPLETED, cutoffDate
        );
        
        List<Order> cancelledOrders = orderRepository.findByStatusAndCreatedAtBefore(
            Order.Status.CANCELLED, cutoffDate
        );
        ordersToDelete.addAll(cancelledOrders);
        
        if (ordersToDelete.isEmpty()) {
            log.info("没有需要清理的订单");
            return;
        }
        
        log.info("开始清理 {} 天前的已完成/已取消订单，共 {} 条", cleanupDays, ordersToDelete.size());
        
        int sessionCount = 0;
        int financialCount = 0;
        int operationLogCount = 0;
        int screenshotCount = 0;
        
        for (Order order : ordersToDelete) {
            // 删除关联的截图文件
            deleteOrderScreenshots(order);
            screenshotCount++;
            sessionCount += orderSessionRepository.deleteByOrderId(order.getId());
            financialCount += financialRecordRepository.deleteByOrderId(order.getId());
            operationLogCount += operationLogRepository.deleteByOrderId(order.getId());
        }
        
        orderRepository.deleteAll(ordersToDelete);
        
        // 如果启用了清除陪玩师累计收入，则清零所有陪玩师的累计收入
        int clearedPlayerCount = 0;
        if (Boolean.TRUE.equals(config.getClearPlayerIncome())) {
            clearedPlayerCount = clearAllPlayersIncome();
        }
        
        log.info("订单清理完成：订单 {} 条，会话 {} 条，财务记录 {} 条，操作日志 {} 条，截图 {} 张，清零陪玩师 {} 人", 
            ordersToDelete.size(), sessionCount, financialCount, operationLogCount, screenshotCount, clearedPlayerCount);
    }
    
    private int clearAllPlayersIncome() {
        List<User> players = userRepository.findByRole(User.Role.player);
        int count = 0;
        for (User player : players) {
            if (player.getTotalIncome() != null && player.getTotalIncome().compareTo(BigDecimal.ZERO) > 0) {
                player.setTotalIncome(BigDecimal.ZERO);
                player.setAvailableBalance(BigDecimal.ZERO);
                userRepository.save(player);
                count++;
            }
        }
        log.info("已清零 {} 位陪玩师的累计收入和可提现余额", count);
        return count;
    }

    @Transactional
    public CleanupResult manualCleanup() {
        SystemConfig config = systemConfigRepository.findFirstByOrderByIdAsc().orElse(null);
        
        if (config == null || !Boolean.TRUE.equals(config.getOrderCleanupEnabled())) {
            throw new RuntimeException("订单自动清理功能未启用");
        }
        
        Integer cleanupDays = config.getOrderCleanupDays();
        if (cleanupDays == null || cleanupDays <= 0) {
            cleanupDays = 30;
        }
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(cleanupDays);
        
        List<Order> ordersToDelete = orderRepository.findByStatusAndCreatedAtBefore(
            Order.Status.COMPLETED, cutoffDate
        );
        
        List<Order> cancelledOrders = orderRepository.findByStatusAndCreatedAtBefore(
            Order.Status.CANCELLED, cutoffDate
        );
        ordersToDelete.addAll(cancelledOrders);
        
        int sessionCount = 0;
        int financialCount = 0;
        int operationLogCount = 0;
        int screenshotCount = 0;
        
        for (Order order : ordersToDelete) {
            // 删除关联的截图文件
            deleteOrderScreenshots(order);
            screenshotCount++;
            sessionCount += orderSessionRepository.deleteByOrderId(order.getId());
            financialCount += financialRecordRepository.deleteByOrderId(order.getId());
            operationLogCount += operationLogRepository.deleteByOrderId(order.getId());
        }
        
        orderRepository.deleteAll(ordersToDelete);
        
        // 如果启用了清除陪玩师累计收入，则清零所有陪玩师的累计收入
        int clearedPlayerCount = 0;
        if (Boolean.TRUE.equals(config.getClearPlayerIncome())) {
            clearedPlayerCount = clearAllPlayersIncome();
        }
        
        log.info("手动清理完成：订单 {} 条，截图 {} 张", ordersToDelete.size(), screenshotCount);
        
        return new CleanupResult(ordersToDelete.size(), sessionCount, financialCount, operationLogCount, clearedPlayerCount);
    }
    
    /**
     * 高级手动清理 - 支持自定义选项
     * @param request 清理请求参数
     */
    @Transactional
    public CleanupResult manualCleanupAdvanced(CleanupRequest request) {
        LocalDateTime cutoffDate = request.getCutoffDate();
        
        log.info("开始手动清理订单，截止日期：{}，清理已完成：{}，清理已取消：{}，清除收入：{}", 
            cutoffDate, request.isCleanupCompleted(), request.isCleanupCancelled(), request.isClearPlayerIncome());
        
        List<Order> ordersToDelete = new java.util.ArrayList<>();
        
        // 清理已完成订单
        if (request.isCleanupCompleted()) {
            ordersToDelete.addAll(orderRepository.findByStatusAndCreatedAtBefore(
                Order.Status.COMPLETED, cutoffDate
            ));
        }
        
        // 清理已取消订单
        if (request.isCleanupCancelled()) {
            ordersToDelete.addAll(orderRepository.findByStatusAndCreatedAtBefore(
                Order.Status.CANCELLED, cutoffDate
            ));
        }
        
        int sessionCount = 0;
        int financialCount = 0;
        int operationLogCount = 0;
        int screenshotCount = 0;
        
        for (Order order : ordersToDelete) {
            // 删除关联的截图文件
            deleteOrderScreenshots(order);
            screenshotCount++;
            sessionCount += orderSessionRepository.deleteByOrderId(order.getId());
            financialCount += financialRecordRepository.deleteByOrderId(order.getId());
            operationLogCount += operationLogRepository.deleteByOrderId(order.getId());
        }
        
        orderRepository.deleteAll(ordersToDelete);
        
        // 如果选择了清除陪玩师累计收入，则清零所有陪玩师的累计收入
        int clearedPlayerCount = 0;
        if (request.isClearPlayerIncome()) {
            clearedPlayerCount = clearAllPlayersIncome();
        }
        
        log.info("手动清理完成：订单 {} 条，会话 {} 条，财务记录 {} 条，操作日志 {} 条，截图 {} 张，清零陪玩师 {} 人", 
            ordersToDelete.size(), sessionCount, financialCount, operationLogCount, screenshotCount, clearedPlayerCount);
        
        return new CleanupResult(ordersToDelete.size(), sessionCount, financialCount, operationLogCount, clearedPlayerCount);
    }
    
    public static class CleanupRequest {
        private LocalDateTime cutoffDate;
        private boolean cleanupCompleted = true;
        private boolean cleanupCancelled = true;
        private boolean clearPlayerIncome = false;
        
        public LocalDateTime getCutoffDate() {
            return cutoffDate;
        }
        
        public void setCutoffDate(LocalDateTime cutoffDate) {
            this.cutoffDate = cutoffDate;
        }
        
        public boolean isCleanupCompleted() {
            return cleanupCompleted;
        }
        
        public void setCleanupCompleted(boolean cleanupCompleted) {
            this.cleanupCompleted = cleanupCompleted;
        }
        
        public boolean isCleanupCancelled() {
            return cleanupCancelled;
        }
        
        public void setCleanupCancelled(boolean cleanupCancelled) {
            this.cleanupCancelled = cleanupCancelled;
        }
        
        public boolean isClearPlayerIncome() {
            return clearPlayerIncome;
        }
        
        public void setClearPlayerIncome(boolean clearPlayerIncome) {
            this.clearPlayerIncome = clearPlayerIncome;
        }
    }
    
    public static class CleanupResult {
        public int orderCount;
        public int sessionCount;
        public int financialCount;
        public int operationLogCount;
        public int screenshotCount;
        public int clearedPlayerCount;
        
        public CleanupResult(int orderCount, int sessionCount, int financialCount, int operationLogCount, int clearedPlayerCount) {
            this.orderCount = orderCount;
            this.sessionCount = sessionCount;
            this.financialCount = financialCount;
            this.operationLogCount = operationLogCount;
            this.screenshotCount = orderCount; // 每个订单最多2张截图
            this.clearedPlayerCount = clearedPlayerCount;
        }
    }
}
