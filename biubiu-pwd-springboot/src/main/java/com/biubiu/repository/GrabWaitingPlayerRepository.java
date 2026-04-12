package com.biubiu.repository;

import com.biubiu.entity.GrabWaitingPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrabWaitingPlayerRepository extends JpaRepository<GrabWaitingPlayer, Long> {
    List<GrabWaitingPlayer> findByOrderId(Long orderId);
    boolean existsByOrderIdAndPlayerId(Long orderId, Long playerId);
    void deleteByOrderIdAndPlayerId(Long orderId, Long playerId);
    void deleteByOrderId(Long orderId);
    List<GrabWaitingPlayer> findByPlayerId(Long playerId);
}
