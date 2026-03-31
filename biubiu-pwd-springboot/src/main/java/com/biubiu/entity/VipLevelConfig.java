package com.biubiu.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "vip_level_config")
public class VipLevelConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer level;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal discountRate;

    @Column(precision = 10, scale = 2)
    private BigDecimal upgradeConsumption;

    @Column(precision = 10, scale = 2)
    private BigDecimal upgradeRecharge;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
