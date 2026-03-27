package com.biubiu.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "system_config")
public class SystemConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(precision = 5, scale = 4)
    private BigDecimal platformFeeRate = BigDecimal.valueOf(0.2000);

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
