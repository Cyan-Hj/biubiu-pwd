package com.biubiu.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "level_prices")
public class LevelPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String level;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal defaultPrice;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
