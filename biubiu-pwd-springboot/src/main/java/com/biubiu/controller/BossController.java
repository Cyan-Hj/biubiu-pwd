package com.biubiu.controller;

import com.biubiu.dto.ApiResponse;
import com.biubiu.dto.PageResponse;
import com.biubiu.entity.Boss;
import com.biubiu.entity.BossRechargeRecord;
import com.biubiu.entity.User;
import com.biubiu.repository.BossRechargeRecordRepository;
import com.biubiu.repository.BossRepository;
import com.biubiu.repository.OrderRepository;
import com.biubiu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/boss")
@RequiredArgsConstructor
public class BossController {

    private final BossRepository bossRepository;
    private final BossRechargeRecordRepository rechargeRecordRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<PageResponse<BossResponse>> getBosses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer vipLevel,
            @RequestParam(required = false) String customerType,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by("createdAt").descending());
        
        Boss.CustomerType type = null;
        if (customerType != null && !customerType.isEmpty()) {
            type = Boss.CustomerType.valueOf(customerType);
        }
        
        Page<Boss> bossPage = bossRepository.findByConditions(keyword, vipLevel, type, enabled, pageable);
        
        List<BossResponse> list = bossPage.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        PageResponse<BossResponse> response = PageResponse.<BossResponse>builder()
                .list(list)
                .total(bossPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
        
        return ApiResponse.success(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<BossResponse> createBoss(@RequestBody BossRequest request) {
        if (bossRepository.existsByName(request.getName())) {
            throw new RuntimeException("该老板名称已存在");
        }
        
        Boss boss = new Boss();
        boss.setName(request.getName());
        boss.setContactType(Boss.ContactType.valueOf(request.getContactType()));
        boss.setContactValue(request.getContactValue());
        boss.setVipLevel(request.getVipLevel() != null ? request.getVipLevel() : 0);
        boss.setCustomerType(Boss.CustomerType.valueOf(request.getCustomerType()));
        boss.setRemark(request.getRemark());
        
        Integer maxNo = bossRepository.findMaxBossNo();
        int nextNo = (maxNo != null ? maxNo : 0) + 1;
        boss.setBossNo(String.format("B-%04d", nextNo));
        
        Boss saved = bossRepository.save(boss);
        return ApiResponse.success("老板创建成功", convertToResponse(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<BossResponse> updateBoss(@PathVariable Long id, @RequestBody BossRequest request) {
        Boss boss = bossRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("老板不存在"));
        
        if (!boss.getName().equals(request.getName()) && bossRepository.existsByName(request.getName())) {
            throw new RuntimeException("该老板名称已存在");
        }
        
        boss.setName(request.getName());
        boss.setContactType(Boss.ContactType.valueOf(request.getContactType()));
        boss.setContactValue(request.getContactValue());
        boss.setVipLevel(request.getVipLevel() != null ? request.getVipLevel() : 0);
        boss.setCustomerType(Boss.CustomerType.valueOf(request.getCustomerType()));
        boss.setRemark(request.getRemark());
        
        Boss saved = bossRepository.save(boss);
        return ApiResponse.success("老板更新成功", convertToResponse(saved));
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<Void> disableBoss(@PathVariable Long id) {
        Boss boss = bossRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("老板不存在"));
        boss.setEnabled(false);
        bossRepository.save(boss);
        return ApiResponse.success("老板已禁用", null);
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<Void> enableBoss(@PathVariable Long id) {
        Boss boss = bossRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("老板不存在"));
        boss.setEnabled(true);
        bossRepository.save(boss);
        return ApiResponse.success("老板已启用", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<Void> deleteBoss(@PathVariable Long id) {
        Boss boss = bossRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("老板不存在"));

        // 检查该老板是否有订单记录
        long orderCount = orderRepository.countByBossId(id);
        if (orderCount > 0) {
            throw new RuntimeException("该老板存在 " + orderCount + " 个订单记录，不能删除");
        }

        bossRepository.delete(boss);
        return ApiResponse.success("老板删除成功", null);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<BossResponse> getBoss(@PathVariable Long id) {
        Boss boss = bossRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("老板不存在"));
        return ApiResponse.success(convertToResponse(boss));
    }

    @PostMapping("/{id}/recharge")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<BossResponse> recharge(@PathVariable Long id, @RequestBody RechargeRequest request) {
        Boss boss = bossRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("老板不存在"));
        
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("充值金额必须大于0");
        }
        
        User currentUser = getCurrentUser();
        
        // 更新余额
        boss.setBalance(boss.getBalance().add(request.getAmount()));
        boss.setTotalRecharge(boss.getTotalRecharge().add(request.getAmount()));
        Boss saved = bossRepository.save(boss);
        
        // 记录充值
        BossRechargeRecord record = new BossRechargeRecord();
        record.setBossId(id);
        record.setAmount(request.getAmount());
        record.setType(BossRechargeRecord.Type.RECHARGE);
        record.setRemark(request.getRemark());
        record.setOperatorId(currentUser.getId());
        rechargeRecordRepository.save(record);
        
        return ApiResponse.success("充值成功", convertToResponse(saved));
    }

    @PostMapping("/{id}/balance")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<BossResponse> updateBalance(@PathVariable Long id, @RequestBody UpdateBalanceRequest request) {
        Boss boss = bossRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("老板不存在"));
        
        if (request.getNewBalance() == null || request.getNewBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("余额不能为负数");
        }
        
        User currentUser = getCurrentUser();
        
        // 计算调整金额
        BigDecimal oldBalance = boss.getBalance();
        BigDecimal adjustment = request.getNewBalance().subtract(oldBalance);
        
        // 更新余额
        boss.setBalance(request.getNewBalance());
        Boss saved = bossRepository.save(boss);
        
        // 记录调整
        BossRechargeRecord record = new BossRechargeRecord();
        record.setBossId(id);
        record.setAmount(adjustment.abs());
        record.setType(adjustment.compareTo(BigDecimal.ZERO) >= 0 ? BossRechargeRecord.Type.RECHARGE : BossRechargeRecord.Type.DEDUCT);
        record.setRemark("手动调整: " + request.getRemark() + " (原余额: ¥" + oldBalance + ", 新余额: ¥" + request.getNewBalance() + ")");
        record.setOperatorId(currentUser.getId());
        rechargeRecordRepository.save(record);
        
        return ApiResponse.success("余额修改成功", convertToResponse(saved));
    }

    @GetMapping("/{id}/records")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<PageResponse<RechargeRecordResponse>> getRecords(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        
        Boss boss = bossRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("老板不存在"));
        
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by("createdAt").descending());
        Page<BossRechargeRecord> recordPage = rechargeRecordRepository.findByBossIdOrderByCreatedAtDesc(id, pageable);
        
        List<RechargeRecordResponse> list = recordPage.getContent().stream()
                .map(this::convertToRecordResponse)
                .collect(Collectors.toList());
        
        PageResponse<RechargeRecordResponse> response = PageResponse.<RechargeRecordResponse>builder()
                .list(list)
                .total(recordPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
        
        return ApiResponse.success(response);
    }

    @PostMapping("/{id}/recalculate-consumption")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<BossConsumptionDetailResponse> recalculateConsumption(@PathVariable Long id) {
        Boss boss = bossRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("老板不存在"));
        
        // 查询该老板所有已完成的订单
        List<com.biubiu.entity.Order> completedOrders = orderRepository.findByBossIdAndStatus(id, com.biubiu.entity.Order.Status.COMPLETED);
        
        // 详细计算每个订单的金额
        List<OrderConsumptionDetail> details = new java.util.ArrayList<>();
        BigDecimal totalConsumption = BigDecimal.ZERO;
        
        for (com.biubiu.entity.Order order : completedOrders) {
            BigDecimal actualAmount = calculateActualTotalAmount(order);
            totalConsumption = totalConsumption.add(actualAmount);
            
            OrderConsumptionDetail detail = new OrderConsumptionDetail();
            detail.setOrderNo(order.getOrderNo());
            detail.setServiceContent(order.getServiceContent());
            detail.setPricePerHour(order.getPricePerHour());
            detail.setServiceHours(order.getServiceHours());
            detail.setActualHours(order.getActualHours());
            detail.setTotalAmount(order.getTotalAmount());
            detail.setActualAmount(actualAmount);
            detail.setCompletedAt(order.getCompletedAt());
            details.add(detail);
        }
        
        // 保存新的累计消费
        boss.setTotalConsumption(totalConsumption);
        Boss saved = bossRepository.save(boss);
        
        BossConsumptionDetailResponse response = new BossConsumptionDetailResponse();
        response.setBossId(saved.getId());
        response.setBossName(saved.getName());
        response.setTotalConsumption(totalConsumption);
        response.setOrderCount(completedOrders.size());
        response.setOrderDetails(details);
        
        return ApiResponse.success("累计消费已重新计算", response);
    }
    
    /**
     * 计算订单的实际金额（基于实际时长）
     */
    private BigDecimal calculateActualTotalAmount(com.biubiu.entity.Order order) {
        BigDecimal pricePerHour = order.getPricePerHour();
        BigDecimal createdHours = order.getServiceHours();
        BigDecimal actualHours = order.getActualHours() != null ? order.getActualHours() : createdHours;
        
        if (actualHours.compareTo(createdHours) <= 0) {
            return createdHours.multiply(pricePerHour);
        }
        
        // 计算超出时间的费用
        BigDecimal extraMinutes = actualHours.subtract(createdHours).multiply(BigDecimal.valueOf(60));
        int totalExtraMinutes = extraMinutes.intValue();
        int fullHours = totalExtraMinutes / 60;
        int remainingMinutes = totalExtraMinutes % 60;
        
        BigDecimal extraFee = BigDecimal.valueOf(fullHours).multiply(pricePerHour);
        if (remainingMinutes > 15 && remainingMinutes <= 45) {
            extraFee = extraFee.add(pricePerHour.multiply(BigDecimal.valueOf(0.5)));
        } else if (remainingMinutes > 45) {
            extraFee = extraFee.add(pricePerHour);
        }
        
        return createdHours.multiply(pricePerHour).add(extraFee);
    }
    
    // DTO for consumption detail
    @lombok.Data
    public static class OrderConsumptionDetail {
        private String orderNo;
        private String serviceContent;
        private BigDecimal pricePerHour;
        private BigDecimal serviceHours;
        private BigDecimal actualHours;
        private BigDecimal totalAmount;
        private BigDecimal actualAmount;
        private java.time.LocalDateTime completedAt;
    }
    
    @lombok.Data
    public static class BossConsumptionDetailResponse {
        private Long bossId;
        private String bossName;
        private BigDecimal totalConsumption;
        private int orderCount;
        private List<OrderConsumptionDetail> orderDetails;
    }

    private BossResponse convertToResponse(Boss boss) {
        return BossResponse.builder()
                .id(boss.getId())
                .bossNo(boss.getBossNo())
                .name(boss.getName())
                .contactType(boss.getContactType().name())
                .contactValue(boss.getContactValue())
                .vipLevel(boss.getVipLevel())
                .totalConsumption(boss.getTotalConsumption())
                .balance(boss.getBalance())
                .totalRecharge(boss.getTotalRecharge())
                .customerType(boss.getCustomerType().name())
                .enabled(boss.getEnabled())
                .remark(boss.getRemark())
                .createdAt(boss.getCreatedAt())
                .updatedAt(boss.getUpdatedAt())
                .build();
    }

    private RechargeRecordResponse convertToRecordResponse(BossRechargeRecord record) {
        User operator = userRepository.findById(record.getOperatorId()).orElse(null);
        return RechargeRecordResponse.builder()
                .id(record.getId())
                .amount(record.getAmount())
                .type(record.getType().name())
                .orderNo(record.getOrderNo())
                .remark(record.getRemark())
                .operatorName(operator != null ? operator.getNickname() : "未知")
                .createdAt(record.getCreatedAt())
                .build();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String phone = authentication.getName();
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    // DTOs
    @lombok.Data
    @lombok.Builder
    public static class BossResponse {
        private Long id;
        private String bossNo;
        private String name;
        private String contactType;
        private String contactValue;
        private Integer vipLevel;
        private BigDecimal totalConsumption;
        private BigDecimal balance;
        private BigDecimal totalRecharge;
        private String customerType;
        private Boolean enabled;
        private String remark;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;
    }

    @lombok.Data
    public static class BossRequest {
        private String name;
        private String contactType;
        private String contactValue;
        private Integer vipLevel;
        private String customerType;
        private String remark;
    }

    @lombok.Data
    public static class RechargeRequest {
        private BigDecimal amount;
        private String remark;
    }

    @lombok.Data
    public static class UpdateBalanceRequest {
        private BigDecimal newBalance;
        private String remark;
    }

    @lombok.Data
    @lombok.Builder
    public static class RechargeRecordResponse {
        private Long id;
        private BigDecimal amount;
        private String type;
        private String orderNo;
        private String remark;
        private String operatorName;
        private java.time.LocalDateTime createdAt;
    }
}
