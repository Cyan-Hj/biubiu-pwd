package com.biubiu.dto;

import com.biubiu.entity.User;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PlayerResponse {
    private Long id;
    private String nickname;
    private String phone;
    private String level;
    private BigDecimal pricePerHour;
    private User.Status status;
    private BigDecimal totalIncome;
    private BigDecimal availableBalance;
    private Long activeOrdersCount;
}
