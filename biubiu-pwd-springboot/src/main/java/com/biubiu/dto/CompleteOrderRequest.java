package com.biubiu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CompleteOrderRequest {
    @NotNull(message = "实际时长不能为空")
    @DecimalMin(value = "0.5", message = "实际时长至少0.5小时")
    private BigDecimal actualHours;
}
