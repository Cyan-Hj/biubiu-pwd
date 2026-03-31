package com.biubiu.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "boss")
public class Boss {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ContactType contactType;

    @Column(nullable = false, length = 50)
    private String contactValue;

    @Column(nullable = false)
    private Integer vipLevel = 0;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalConsumption = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalRecharge = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CustomerType customerType = CustomerType.SCATTER;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(length = 500)
    private String remark;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum ContactType {
        WECHAT, QQ
    }

    public enum CustomerType {
        SCATTER, REGULAR
    }
}
