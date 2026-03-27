package com.biubiu.dto;

import com.biubiu.entity.User;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdatePlayerRequest {
    private String nickname;
    private String level;

    @DecimalMin(value = "1", message = "单价必须大于0")
    private BigDecimal pricePerHour;

    private User.Status status;
}
