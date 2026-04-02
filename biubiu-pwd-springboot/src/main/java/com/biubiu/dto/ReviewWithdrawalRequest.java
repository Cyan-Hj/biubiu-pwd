package com.biubiu.dto;

import com.biubiu.entity.WithdrawalRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewWithdrawalRequest {
    @NotNull(message = "状态不能为空")
    private WithdrawalRequest.Status status;

    private String rejectReason;
}
