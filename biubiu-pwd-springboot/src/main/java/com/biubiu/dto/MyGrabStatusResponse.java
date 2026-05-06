package com.biubiu.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MyGrabStatusResponse {
    private WaitingOrderInfo waitingOrder;
    private PriorityWaitingOrderInfo priorityWaitingOrder;
    private LockedOrderInfo lockedOrder;
    private List<CooldownInfo> cooldowns;
    private Boolean hasActiveOrder;

    @Data
    @Builder
    public static class WaitingOrderInfo {
        private Long orderId;
        private String orderNo;
        private String grabType;
        private LocalDateTime waitingSince;
        private List<OtherWaiterInfo> otherWaiters;
    }

    @Data
    @Builder
    public static class OtherWaiterInfo {
        private Long id;
        private String nickname;
        private String level;
    }

    @Data
    @Builder
    public static class PriorityWaitingOrderInfo {
        private Long orderId;
        private String orderNo;
        private LocalDateTime priorityWaitUntil;
        private Integer priorityWaitRemainingSeconds;
    }

    @Data
    @Builder
    public static class LockedOrderInfo {
        private Long orderId;
        private String orderNo;
        private String partnerNickname;
        private LocalDateTime lockUntil;
        private Integer lockRemainingSeconds;
    }

    @Data
    @Builder
    public static class CooldownInfo {
        private Long orderId;
        private String orderNo;
        private LocalDateTime cooldownUntil;
        private Integer remainingSeconds;
    }
}
