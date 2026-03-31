package com.biubiu.controller;

import com.biubiu.dto.ApiResponse;
import com.biubiu.entity.VipLevelConfig;
import com.biubiu.repository.VipLevelConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/system/vip-levels")
@RequiredArgsConstructor
public class VipLevelController {

    private final VipLevelConfigRepository vipLevelConfigRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<List<VipLevelConfig>> getVipLevels() {
        return ApiResponse.success(vipLevelConfigRepository.findAllByOrderBySortOrderAsc());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<VipLevelConfig> createVipLevel(@RequestBody VipLevelRequest request) {
        if (vipLevelConfigRepository.existsByLevel(request.getLevel())) {
            throw new RuntimeException("该等级已存在");
        }

        VipLevelConfig config = new VipLevelConfig();
        config.setLevel(request.getLevel());
        config.setName(request.getName());
        config.setDiscountRate(request.getDiscountRate());
        config.setUpgradeConsumption(request.getUpgradeConsumption());
        config.setUpgradeRecharge(request.getUpgradeRecharge());
        
        // 设置排序
        List<VipLevelConfig> existing = vipLevelConfigRepository.findAllByOrderBySortOrderAsc();
        config.setSortOrder(existing.size());
        
        VipLevelConfig saved = vipLevelConfigRepository.save(config);
        return ApiResponse.success("VIP等级创建成功", saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<VipLevelConfig> updateVipLevel(@PathVariable Long id, @RequestBody VipLevelRequest request) {
        VipLevelConfig config = vipLevelConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("VIP等级不存在"));

        // 检查等级是否被其他配置使用
        if (!config.getLevel().equals(request.getLevel())) {
            if (vipLevelConfigRepository.existsByLevel(request.getLevel())) {
                throw new RuntimeException("该等级已存在");
            }
            config.setLevel(request.getLevel());
        }

        config.setName(request.getName());
        config.setDiscountRate(request.getDiscountRate());
        config.setUpgradeConsumption(request.getUpgradeConsumption());
        config.setUpgradeRecharge(request.getUpgradeRecharge());

        VipLevelConfig saved = vipLevelConfigRepository.save(config);
        return ApiResponse.success("VIP等级更新成功", saved);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteVipLevel(@PathVariable Long id) {
        VipLevelConfig config = vipLevelConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("VIP等级不存在"));

        // 检查是否是默认等级（level=0）
        if (config.getLevel() == 0) {
            throw new RuntimeException("默认等级不能删除");
        }

        vipLevelConfigRepository.delete(config);
        
        // 重新排序
        List<VipLevelConfig> remaining = vipLevelConfigRepository.findAllByOrderBySortOrderAsc();
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setSortOrder(i);
            vipLevelConfigRepository.save(remaining.get(i));
        }
        
        return ApiResponse.success("VIP等级删除成功", null);
    }

    @lombok.Data
    public static class VipLevelRequest {
        private Integer level;
        private String name;
        private BigDecimal discountRate;
        private BigDecimal upgradeConsumption;
        private BigDecimal upgradeRecharge;
    }
}
