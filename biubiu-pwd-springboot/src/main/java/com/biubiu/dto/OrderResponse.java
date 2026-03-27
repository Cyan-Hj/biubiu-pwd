package com.biubiu.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long id;
    private String orderNo;
    private String bossInfo;
    private String serviceContent;
    private BigDecimal serviceHours;
    private BigDecimal pricePerHour;
    private BigDecimal totalAmount;
    private LocalDateTime scheduledTime;
    private String remark;
    private Integer status;
    private Long currentPlayerId;
    private String currentPlayerNickname;
    private Long currentPlayer2Id;
    private String currentPlayer2Nickname;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private BigDecimal actualHours;
    
    // 扩展信息，可用于列表展示
    private String createdByNickname;
    private String assignedByNickname;
    private BigDecimal incomeAmount; // 陪玩师在这单的实际收入
    
    // 取消相关信息
    private String cancelReason;
    private LocalDateTime cancelledAt;
    
    // 订单类型（单人/双人）
    private String playerCount;
    
    // 当前登录用户是否已接单（用于双人订单）
    private Boolean currentUserAccepted;
    
    // 当前登录用户是否已完成（用于双人订单）
    private Boolean currentUserCompleted;
    
    // 另一个陪玩师是否已完成（用于双人订单）
    private Boolean otherPlayerCompleted;
    
    // 陪玩师会话信息列表（用于详情页面显示每个人的完成时间）
    private List<PlayerSessionInfo> playerSessions;
}
