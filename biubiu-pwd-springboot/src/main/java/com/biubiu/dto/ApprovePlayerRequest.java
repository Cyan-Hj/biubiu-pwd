package com.biubiu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApprovePlayerRequest {
    @NotBlank(message = "等级不能为空")
    private String level;

    @DecimalMin(value = "1", message = "单价必须大于0")
    private BigDecimal pricePerHour;
}
