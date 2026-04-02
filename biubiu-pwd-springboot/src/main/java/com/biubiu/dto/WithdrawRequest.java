package com.biubiu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WithdrawRequest {
    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "1", message = "金额必须大于0")
    private BigDecimal amount;

    @NotBlank(message = "提现方式不能为空")
    private String paymentMethod;

    @NotBlank(message = "账号不能为空")
    private String accountInfo;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;
}
