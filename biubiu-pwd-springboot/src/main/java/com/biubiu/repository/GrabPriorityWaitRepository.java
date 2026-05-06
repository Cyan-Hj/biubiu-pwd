package com.biubiu.repository;

import com.biubiu.entity.GrabPriorityWait;
import com.biubiu.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GrabPriorityWaitRepository extends JpaRepository<GrabPriorityWait, Long> {
    List<GrabPriorityWait> findByOrderId(Long orderId);
    List<GrabPriorityWait> findByWaitUntilBefore(LocalDateTime now);
    boolean existsByOrderIdAndPlayerId(Long orderId, Long playerId);
    void deleteByOrderIdAndPlayerId(Long orderId, Long playerId);
    void deleteByOrderId(Long orderId);
    List<GrabPriorityWait> findByPlayerId(Long playerId);

    @Query("SELECT gpw FROM GrabPriorityWait gpw WHERE gpw.player.status = :status")
    List<GrabPriorityWait> findByPlayerStatus(@Param("status") User.Status status);
}
