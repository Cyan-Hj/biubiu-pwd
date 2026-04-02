package com.biubiu.dto;

import com.biubiu.entity.User;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String phone;
    private String nickname;
    private String avatar;
    private User.Role role;
    private String level;
    private BigDecimal pricePerHour;
    private User.Status status;
    private BigDecimal totalIncome;
    private BigDecimal availableBalance;
    private LocalDateTime createdAt;
    private String token;
}
