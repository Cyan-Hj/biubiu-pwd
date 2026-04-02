package com.biubiu.dto;

import com.biubiu.entity.WithdrawalRequest;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class WithdrawalResponse {
    private Long id;
    private String playerNickname;
    private BigDecimal amount;
    private String paymentMethod;
    private String accountInfo;
    private String realName;
    private WithdrawalRequest.Status status;
    private String rejectReason;
    private LocalDateTime createdAt;
}
