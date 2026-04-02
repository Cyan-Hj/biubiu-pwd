package com.biubiu.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "boss_recharge_record")
public class BossRechargeRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bossId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Type type;

    @Column(length = 50)
    private String orderNo;

    @Column(length = 500)
    private String remark;

    @Column(nullable = false)
    private Long operatorId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum Type {
        RECHARGE, DEDUCT
    }
}
