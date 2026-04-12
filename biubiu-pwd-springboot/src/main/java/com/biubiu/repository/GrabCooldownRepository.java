package com.biubiu.repository;

import com.biubiu.entity.GrabCooldown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface GrabCooldownRepository extends JpaRepository<GrabCooldown, Long> {
    boolean existsByOrderIdAndPlayerIdAndCooldownUntilAfter(Long orderId, Long playerId, LocalDateTime now);
    void deleteByOrderId(Long orderId);

    @Modifying
    @Query("DELETE FROM GrabCooldown gc WHERE gc.cooldownUntil < :now")
    void deleteExpiredCooldowns(@Param("now") LocalDateTime now);
}
