package com.biubiu.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String orderNo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String bossInfo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String serviceContent;

    @Column(nullable = false, precision = 4, scale = 1)
    private BigDecimal serviceHours;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    private LocalDateTime scheduledTime;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Column(nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private Status status = Status.PENDING_ASSIGN;

    @ManyToOne
    @JoinColumn(name = "current_player_id")
    private User currentPlayer;

    @ManyToOne
    @JoinColumn(name = "current_player2_id")
    private User currentPlayer2;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PlayerCount playerCount = PlayerCount.SINGLE;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime assignedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @Column(precision = 4, scale = 1)
    private BigDecimal actualHours;

    @Column(columnDefinition = "TEXT")
    private String cancelReason;

    private LocalDateTime cancelledAt;

    public enum Status {
        PENDING_ASSIGN,    // 0: 待分配
        PENDING_ACCEPT,    // 1: 待接单（单人）/ 待接单1（双人第一个）
        PENDING_ACCEPT_2,  // 2: 待接单2（双人第二个）
        IN_SERVICE,        // 3: 服务中
        COMPLETED,         // 4: 已完成
        CANCELLED          // 5: 已取消
    }

    public enum PlayerCount {
        SINGLE,    // 单人
        DOUBLE     // 双人
    }
}
