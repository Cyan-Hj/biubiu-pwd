package com.biubiu.repository;

import com.biubiu.entity.OrderSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderSessionRepository extends JpaRepository<OrderSession, Long> {
    List<OrderSession> findByOrderId(Long orderId);
    
    Optional<OrderSession> findFirstByOrderIdAndPlayerIdAndEndedAtIsNullOrderByIdDesc(Long orderId, Long playerId);
    
    List<OrderSession> findByOrderIdAndPlayerId(Long orderId, Long playerId);
    
    Optional<OrderSession> findByOrderIdAndPlayerIdAndEndedAtIsNull(Long orderId, Long playerId);
}
