package com.biubiu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReplenishOrderRequest {
    @NotBlank(message = "老板信息不能为空")
    private String bossInfo;

    @NotBlank(message = "服务内容不能为空")
    private String serviceContent;

    @NotNull(message = "服务时长不能为空")
    private BigDecimal serviceHours;

    @NotNull(message = "单价不能为空")
    private BigDecimal pricePerHour;

    @NotNull(message = "总价不能为空")
    private BigDecimal totalAmount;

    private BigDecimal actualHours;
    private LocalDateTime scheduledTime;
    private String remark;
    private String orderType;
    private String playerCount;
    private String customerType;
    private Long bossId;

    private Long currentPlayerId;
    private Long currentPlayer2Id;
    private String originalOrderNo;
    private LocalDateTime originalCreatedAt;
    private LocalDateTime originalCompletedAt;
}
