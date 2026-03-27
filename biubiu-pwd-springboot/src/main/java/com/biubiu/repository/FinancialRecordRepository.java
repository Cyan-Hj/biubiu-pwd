package com.biubiu.repository;

import com.biubiu.dto.PlayerIncomeSummary;
import com.biubiu.entity.FinancialRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, Long> {

    Optional<FinancialRecord> findFirstByOrderIdAndPlayerIdAndTypeOrderByIdDesc(Long orderId,
                                                                                Long playerId,
                                                                                FinancialRecord.Type type);

    @Query("SELECT fr FROM FinancialRecord fr WHERE fr.player.id = :playerId ORDER BY fr.createdAt DESC")
    Page<FinancialRecord> findByPlayerId(@Param("playerId") Long playerId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(fr.amount), 0) FROM FinancialRecord fr " +
           "WHERE fr.player.id = :playerId AND fr.type = 'income'")
    BigDecimal sumTotalIncomeByPlayerId(@Param("playerId") Long playerId);

    @Query("SELECT COALESCE(SUM(fr.amount), 0) FROM FinancialRecord fr " +
           "WHERE fr.player.id = :playerId AND fr.type = 'income' " +
           "AND fr.createdAt >= :startOfDay")
    BigDecimal sumTodayIncomeByPlayerId(@Param("playerId") Long playerId,
                                        @Param("startOfDay") LocalDateTime startOfDay);

    @Query("SELECT COALESCE(SUM(fr.amount), 0) FROM FinancialRecord fr WHERE fr.type = 'income'")
    BigDecimal sumTotalIncome();

    @Query("SELECT COALESCE(SUM(fr.amount), 0) FROM FinancialRecord fr " +
           "WHERE fr.type = 'income' AND fr.createdAt >= :startTime")
    BigDecimal sumIncomeByCreatedAtAfter(@Param("startTime") LocalDateTime startTime);

    @Query("SELECT fr.player.id as playerId, fr.player.nickname as nickname, SUM(fr.amount) as totalIncome " +
           "FROM FinancialRecord fr " +
           "WHERE fr.type = 'income' " +
           "GROUP BY fr.player.id, fr.player.nickname " +
           "ORDER BY SUM(fr.amount) DESC")
    List<PlayerIncomeSummary> findTopPlayerIncomes(Pageable pageable);
    
    List<FinancialRecord> findByOrderId(Long orderId);
}
