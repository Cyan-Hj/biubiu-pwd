package com.biubiu.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class GrabHallOrderResponse {
    private Long id;
    private String orderNo;
    private String orderType;
    private String playerCount;
    private String grabStatus;
    private BigDecimal pricePerHour;
    private BigDecimal totalAmount;
    private BigDecimal serviceHours;
    private BigDecimal estimatedIncome;
    private String bossInfo;
    private String customerType;
    private String serviceContent;
    private LocalDateTime scheduledTime;
    private LocalDateTime publishedAt;
    private List<WaitingPlayerInfo> waitingPlayers;
    private Boolean isLocked;
    private Integer lockRemainingSeconds;
    private Long grabPartnerId;
    private String grabPartnerNickname;
    private Long grabLeaderId;
    private String priorityLevel;
    private Boolean isPriorityWaiting;
    private Integer priorityWaitRemainingSeconds;
    private List<PriorityWaitPlayerInfo> priorityWaitPlayers;

    @Data
    @Builder
    public static class WaitingPlayerInfo {
        private Long id;
        private String playerNo;
        private String nickname;
        private String level;
        private BigDecimal pricePerHour;
    }

    @Data
    @Builder
    public static class PriorityWaitPlayerInfo {
        private Long id;
        private String nickname;
        private String level;
    }
}
