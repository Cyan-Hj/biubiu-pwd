package com.biubiu.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PlayerSessionInfo {
    private Long playerId;
    private String playerNickname;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private BigDecimal actualHours;
}
