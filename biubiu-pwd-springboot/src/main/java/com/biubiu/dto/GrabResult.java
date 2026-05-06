package com.biubiu.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GrabResult {
    private Long orderId;
    private String orderNo;
    private String status;
    private LocalDateTime priorityWaitUntil;
    private Integer priorityWaitSeconds;
    private String partnerNickname;
    private LocalDateTime lockUntil;
    private Integer lockRemainingSeconds;
}
