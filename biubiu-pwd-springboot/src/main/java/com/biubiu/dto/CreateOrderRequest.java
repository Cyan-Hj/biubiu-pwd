package com.biubiu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateOrderRequest {
    @NotBlank(message = "老板信息不能为空")
    private String bossInfo;

    @NotBlank(message = "服务内容不能为空")
    private String serviceContent;

    @NotNull(message = "服务时长不能为空")
    @DecimalMin(value = "0.5", message = "服务时长至少0.5小时")
    private BigDecimal serviceHours;

    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "1", message = "单价必须大于0")
    private BigDecimal pricePerHour;

    @NotNull(message = "总价不能为空")
    private BigDecimal totalAmount;

    private LocalDateTime scheduledTime;
    private String remark;
    private String orderType;
    private String playerCount;

    // 客户类型和老板信息
    private String customerType;
    private Long bossId;
    private BigDecimal originalAmount;
    private BigDecimal discountRate;
    private Boolean useBalance;
}
