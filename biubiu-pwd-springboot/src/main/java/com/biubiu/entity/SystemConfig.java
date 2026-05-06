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

    @Column(name = "order_cleanup_days")
    private Integer orderCleanupDays = 30;

    @Column(name = "order_cleanup_enabled")
    private Boolean orderCleanupEnabled = false;

    @Column(name = "clear_player_income")
    private Boolean clearPlayerIncome = false;

    @Column(name = "grab_team_lock_seconds")
    private Integer grabTeamLockSeconds = 60;

    @Column(name = "grab_cooldown_seconds")
    private Integer grabCooldownSeconds = 60;

    @Column(name = "grab_priority_wait_seconds")
    private Integer grabPriorityWaitSeconds = 300;

    @Column(name = "grab_polling_interval_seconds")
    private Integer grabPollingIntervalSeconds = 5;

    @Column(name = "grab_enabled")
    private Boolean grabEnabled = true;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
