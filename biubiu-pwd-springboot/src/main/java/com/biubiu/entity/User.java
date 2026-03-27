package com.biubiu.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 11)
    private String phone;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(length = 255)
    private String avatar;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private Role role = Role.player;

    @Column(length = 20)
    private String level = "青铜";

    @Column(precision = 10, scale = 2)
    private BigDecimal pricePerHour = BigDecimal.valueOf(50.00);

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private Status status = Status.pending;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalIncome = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Role {
        player, customer_service, admin
    }

    public enum Status {
        pending, active, disabled
    }
}
