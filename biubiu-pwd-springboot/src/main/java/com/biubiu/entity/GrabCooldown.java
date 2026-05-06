package com.biubiu.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "grab_cooldowns")
public class GrabCooldown {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private User player;

    @Column(name = "cooldown_until", nullable = false)
    private LocalDateTime cooldownUntil;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
