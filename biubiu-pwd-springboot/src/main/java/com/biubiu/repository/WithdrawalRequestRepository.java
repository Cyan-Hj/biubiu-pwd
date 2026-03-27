package com.biubiu.repository;

import com.biubiu.entity.WithdrawalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {

    @Query("SELECT w FROM WithdrawalRequest w WHERE w.status = 'pending' ORDER BY w.createdAt ASC")
    List<WithdrawalRequest> findPendingRequests();

    @Query("SELECT w FROM WithdrawalRequest w WHERE w.player.id = :playerId ORDER BY w.createdAt DESC")
    List<WithdrawalRequest> findByPlayerId(@Param("playerId") Long playerId);

    long countByStatus(WithdrawalRequest.Status status);
}
