package com.biubiu.controller;

import com.biubiu.dto.ApiResponse;
import com.biubiu.entity.LevelPrice;
import com.biubiu.entity.SystemConfig;
import com.biubiu.entity.SystemOption;
import com.biubiu.repository.LevelPriceRepository;
import com.biubiu.repository.SystemConfigRepository;
import com.biubiu.repository.SystemOptionRepository;
import com.biubiu.service.OrderCleanupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final SystemConfigRepository systemConfigRepository;
    private final LevelPriceRepository levelPriceRepository;
    private final SystemOptionRepository systemOptionRepository;
    private final OrderCleanupService orderCleanupService;

    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<SystemConfig> getConfig() {
        SystemConfig config = systemConfigRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> {
                    SystemConfig newConfig = new SystemConfig();
                    newConfig.setPlatformFeeRate(BigDecimal.valueOf(0.2));
                    return systemConfigRepository.save(newConfig);
                });
        return ApiResponse.success(config);
    }

    @PostMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> updateConfig(@RequestBody SystemConfig request) {
        SystemConfig config = systemConfigRepository.findFirstByOrderByIdAsc()
                .orElse(new SystemConfig());
        
        if (request.getPlatformFeeRate() != null) {
            config.setPlatformFeeRate(request.getPlatformFeeRate());
        }
        
        if (request.getOrderCleanupDays() != null) {
            config.setOrderCleanupDays(request.getOrderCleanupDays());
        }
        
        if (request.getOrderCleanupEnabled() != null) {
            config.setOrderCleanupEnabled(request.getOrderCleanupEnabled());
        }
        
        if (request.getClearPlayerIncome() != null) {
            config.setClearPlayerIncome(request.getClearPlayerIncome());
        }
        
        systemConfigRepository.save(config);
        return ApiResponse.success("配置更新成功", null);
    }

    @PostMapping("/cleanup/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CleanupResultResponse> manualCleanupOrders(@RequestBody(required = false) CleanupRequestDTO request) {
        OrderCleanupService.CleanupResult result;
        
        System.out.println("=== Cleanup Request ===");
        System.out.println("Request: " + request);
        if (request != null) {
            System.out.println("CutoffDate String: " + request.getCutoffDate());
            System.out.println("CutoffDateTime: " + request.getCutoffDateTime());
            System.out.println("CleanupCompleted: " + request.isCleanupCompleted());
            System.out.println("CleanupCancelled: " + request.isCleanupCancelled());
            System.out.println("ClearPlayerIncome: " + request.isClearPlayerIncome());
        }
        
        if (request != null && request.getCutoffDateTime() != null) {
            // 使用高级清理 - 自定义参数
            OrderCleanupService.CleanupRequest cleanupRequest = new OrderCleanupService.CleanupRequest();
            cleanupRequest.setCutoffDate(request.getCutoffDateTime());
            cleanupRequest.setCleanupCompleted(request.isCleanupCompleted());
            cleanupRequest.setCleanupCancelled(request.isCleanupCancelled());
            cleanupRequest.setClearPlayerIncome(request.isClearPlayerIncome());
            result = orderCleanupService.manualCleanupAdvanced(cleanupRequest);
        } else {
            // 使用传统清理 - 依赖系统配置
            System.out.println("Using traditional cleanup");
            result = orderCleanupService.manualCleanup();
        }
        
        CleanupResultResponse response = new CleanupResultResponse(
            result.orderCount,
            result.sessionCount,
            result.financialCount,
            result.operationLogCount,
            result.screenshotCount,
            result.clearedPlayerCount
        );
        return ApiResponse.success("清理完成", response);
    }
    
    public static class CleanupRequestDTO {
        private String cutoffDate;
        private boolean cleanupCompleted = true;
        private boolean cleanupCancelled = true;
        private boolean clearPlayerIncome = false;
        
        public String getCutoffDate() {
            return cutoffDate;
        }
        
        public void setCutoffDate(String cutoffDate) {
            this.cutoffDate = cutoffDate;
        }
        
        public java.time.LocalDateTime getCutoffDateTime() {
            if (cutoffDate == null || cutoffDate.isEmpty()) {
                return null;
            }
            try {
                // 支持格式: "YYYY-MM-DD" (日期) 或 "YYYY-MM-DD HH:mm:ss" (日期时间)
                if (cutoffDate.length() == 10) {
                    // 只有日期，转换为当天结束时间 23:59:59
                    return java.time.LocalDate.parse(cutoffDate).atTime(23, 59, 59);
                }
                // 支持格式: "YYYY-MM-DD HH:mm:ss" 或 "YYYY-MM-DDTHH:mm:ss"
                String normalized = cutoffDate.replace(" ", "T");
                return java.time.LocalDateTime.parse(normalized);
            } catch (Exception e) {
                // 如果解析失败，尝试使用 DateTimeFormatter
                try {
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    return java.time.LocalDateTime.parse(cutoffDate, formatter);
                } catch (Exception e2) {
                    return null;
                }
            }
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
    
    public static class CleanupResultResponse {
        public int orderCount;
        public int sessionCount;
        public int financialCount;
        public int operationLogCount;
        public int screenshotCount;
        public int clearedPlayerCount;
        
        public CleanupResultResponse(int orderCount, int sessionCount, int financialCount, int operationLogCount, int screenshotCount, int clearedPlayerCount) {
            this.orderCount = orderCount;
            this.sessionCount = sessionCount;
            this.financialCount = financialCount;
            this.operationLogCount = operationLogCount;
            this.screenshotCount = screenshotCount;
            this.clearedPlayerCount = clearedPlayerCount;
        }
    }

    @GetMapping("/levels")
    public ApiResponse<List<LevelPrice>> getLevelPrices() {
        return ApiResponse.success(levelPriceRepository.findAllByOrderBySortOrderAsc());
    }

    @PostMapping("/levels")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> updateLevelPrice(@RequestBody LevelPrice request) {
        LevelPrice levelPrice = levelPriceRepository.findById(request.getId())
                .orElseThrow(() -> new RuntimeException("等级配置不存在"));
        
        levelPrice.setDefaultPrice(request.getDefaultPrice());
        levelPriceRepository.save(levelPrice);
        
        return ApiResponse.success("等级价格更新成功", null);
    }

    @PostMapping("/levels/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LevelPrice> createLevelPrice(@RequestBody LevelPrice request) {
        if (levelPriceRepository.existsByLevel(request.getLevel())) {
            throw new RuntimeException("该等级名称已存在");
        }
        
        // 获取当前最大排序值
        List<LevelPrice> allLevels = levelPriceRepository.findAllByOrderBySortOrderAsc();
        int maxSortOrder = allLevels.stream()
                .mapToInt(LevelPrice::getSortOrder)
                .max()
                .orElse(-1);
        
        LevelPrice levelPrice = new LevelPrice();
        levelPrice.setLevel(request.getLevel());
        levelPrice.setDefaultPrice(request.getDefaultPrice());
        levelPrice.setSortOrder(maxSortOrder + 1);
        LevelPrice saved = levelPriceRepository.save(levelPrice);
        
        return ApiResponse.success("等级创建成功", saved);
    }

    @DeleteMapping("/levels/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteLevelPrice(@PathVariable Long id) {
        LevelPrice levelPrice = levelPriceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("等级配置不存在"));
        
        levelPriceRepository.delete(levelPrice);
        return ApiResponse.success("等级删除成功", null);
    }

    @PostMapping("/levels/sort")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> sortLevelPrices(@RequestBody List<LevelPrice> request) {
        // 更新所有等级的排序
        for (int i = 0; i < request.size(); i++) {
            final int sortOrder = i;
            final Long levelId = request.get(i).getId();
            LevelPrice levelPrice = levelPriceRepository.findById(levelId)
                    .orElseThrow(() -> new RuntimeException("等级配置不存在: " + levelId));
            levelPrice.setSortOrder(sortOrder);
            levelPriceRepository.save(levelPrice);
        }
        return ApiResponse.success("排序更新成功", null);
    }

    // ==================== 系统选项管理（注意事项 & 服务项目） ====================

    @GetMapping("/options")
    public ApiResponse<List<SystemOption>> getSystemOptions(@RequestParam String type) {
        SystemOption.OptionType optionType = SystemOption.OptionType.valueOf(type.toUpperCase());
        return ApiResponse.success(systemOptionRepository.findByTypeOrderBySortOrderAsc(optionType));
    }

    @PostMapping("/options")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SystemOption> createSystemOption(@RequestBody SystemOption request) {
        if (systemOptionRepository.existsByTypeAndValue(request.getType(), request.getValue())) {
            throw new RuntimeException("该选项值已存在");
        }

        // 获取当前最大排序值
        List<SystemOption> existingOptions = systemOptionRepository.findByTypeOrderBySortOrderAsc(request.getType());
        int maxSortOrder = existingOptions.stream()
                .mapToInt(SystemOption::getSortOrder)
                .max()
                .orElse(-1);

        SystemOption option = new SystemOption();
        option.setType(request.getType());
        option.setValue(request.getValue());
        option.setDescription(request.getDescription());
        option.setSortOrder(maxSortOrder + 1);
        SystemOption saved = systemOptionRepository.save(option);

        return ApiResponse.success("选项创建成功", saved);
    }

    @PutMapping("/options/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SystemOption> updateSystemOption(@PathVariable Long id, @RequestBody SystemOption request) {
        SystemOption option = systemOptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("选项不存在"));

        // 检查是否有其他相同类型的选项使用了相同的值
        if (!option.getValue().equals(request.getValue()) &&
            systemOptionRepository.existsByTypeAndValue(option.getType(), request.getValue())) {
            throw new RuntimeException("该选项值已存在");
        }

        option.setValue(request.getValue());
        option.setDescription(request.getDescription());
        SystemOption saved = systemOptionRepository.save(option);

        return ApiResponse.success("选项更新成功", saved);
    }

    @DeleteMapping("/options/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteSystemOption(@PathVariable Long id) {
        SystemOption option = systemOptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("选项不存在"));

        systemOptionRepository.delete(option);
        return ApiResponse.success("选项删除成功", null);
    }

    @PostMapping("/options/sort")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> sortSystemOptions(@RequestBody List<SystemOption> request) {
        for (int i = 0; i < request.size(); i++) {
            final int sortOrder = i;
            final Long optionId = request.get(i).getId();
            SystemOption option = systemOptionRepository.findById(optionId)
                    .orElseThrow(() -> new RuntimeException("选项不存在: " + optionId));
            option.setSortOrder(sortOrder);
            systemOptionRepository.save(option);
        }
        return ApiResponse.success("排序更新成功", null);
    }
}
