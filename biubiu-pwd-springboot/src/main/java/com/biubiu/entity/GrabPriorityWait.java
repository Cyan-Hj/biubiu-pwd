package com.biubiu.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "grab_priority_waits")
public class GrabPriorityWait {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private User player;

    @Column(name = "wait_until", nullable = false)
    private LocalDateTime waitUntil;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
