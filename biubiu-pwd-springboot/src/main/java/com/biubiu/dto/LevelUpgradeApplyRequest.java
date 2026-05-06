package com.biubiu.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LevelUpgradeApplyRequest {
    @NotBlank(message = "目标等级不能为空")
    private String requestedLevel;

    private String reason;
}
