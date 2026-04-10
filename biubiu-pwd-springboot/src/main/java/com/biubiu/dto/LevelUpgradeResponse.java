package com.biubiu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevelUpgradeResponse {
    private Long id;
    private Long playerId;
    private String playerNickname;
    private String playerNo;
    private String currentLevel;
    private String requestedLevel;
    private BigDecimal requestedLevelPrice;
    private String status;
    private String reason;
    private String reviewerNickname;
    private String reviewComment;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
