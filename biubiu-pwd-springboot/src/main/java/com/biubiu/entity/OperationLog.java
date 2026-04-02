package com.biubiu.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "operation_logs")
public class OperationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "operator_id")
    private User operator;

    @Column(nullable = false, length = 20)
    private String operationType; // CREATE, ASSIGN, REASSIGN, ACCEPT, CANCEL, COMPLETE

    @Column(length = 20, nullable = true)
    @Enumerated(EnumType.STRING)
    private Order.Status oldStatus;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Order.Status newStatus;

    @Column(columnDefinition = "TEXT")
    private String details;

    @CreationTimestamp
    private LocalDateTime createdAt;
}