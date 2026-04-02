package com.biubiu.repository;

import com.biubiu.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNo(String orderNo);

    @Query("SELECT o FROM Order o WHERE " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:playerId IS NULL OR o.currentPlayer.id = :playerId OR o.currentPlayer2.id = :playerId) AND " +
           "(:today IS NULL OR :today = false OR DATE(o.createdAt) = CURRENT_DATE) AND " +
           "(:startDate IS NULL OR DATE(o.createdAt) >= :startDate) AND " +
           "(:endDate IS NULL OR DATE(o.createdAt) <= :endDate) AND " +
           "(:excludeCancelled IS NULL OR :excludeCancelled = false OR o.status <> :cancelledStatus)")
    Page<Order> findOrders(@Param("status") Order.Status status,
                          @Param("playerId") Long playerId,
                          @Param("today") Boolean today,
                          @Param("startDate") java.time.LocalDate startDate,
                          @Param("endDate") java.time.LocalDate endDate,
                          @Param("excludeCancelled") Boolean excludeCancelled,
                          @Param("cancelledStatus") Order.Status cancelledStatus,
                          Pageable pageable);

    @Query("SELECT o FROM Order o WHERE (o.currentPlayer.id = :playerId OR o.currentPlayer2.id = :playerId) ORDER BY o.createdAt DESC")
    List<Order> findByPlayerId(@Param("playerId") Long playerId);

    long countByStatus(Order.Status status);
    
    List<Order> findByStatus(Order.Status status);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE (o.currentPlayer.id = :playerId OR o.currentPlayer2.id = :playerId) AND o.status = :status")
    long countByCurrentPlayerIdAndStatus(@Param("playerId") Long playerId, @Param("status") Order.Status status);

    @Query("SELECT o FROM Order o WHERE (o.currentPlayer.id = :playerId OR o.currentPlayer2.id = :playerId) AND o.status = :status")
    List<Order> findByCurrentPlayerIdAndStatus(@Param("playerId") Long playerId, @Param("status") Order.Status status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :startTime")
    long countByCreatedAtAfter(@Param("startTime") LocalDateTime startTime);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.createdAt >= :startTime")
    java.math.BigDecimal sumAmountByCreatedAtAfter(@Param("startTime") LocalDateTime startTime);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = :status AND o.createdAt >= :startTime")
    java.math.BigDecimal sumTotalAmountByStatusAndCreatedAtAfter(@Param("status") Order.Status status, @Param("startTime") LocalDateTime startTime);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = :status")
    java.math.BigDecimal sumTotalAmountByStatus(@Param("status") Order.Status status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.createdAt >= :startTime")
    Long countByStatusAndCreatedAtAfter(@Param("status") Order.Status status, @Param("startTime") LocalDateTime startTime);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = :status AND o.createdAt >= :startTime")
    java.math.BigDecimal sumAmountByStatusAndCreatedAtAfter(@Param("status") Order.Status status, @Param("startTime") LocalDateTime startTime);

    @Query("SELECT DATE(o.createdAt) as date, COALESCE(SUM(o.totalAmount), 0) as amount, COUNT(o) as count " +
           "FROM Order o WHERE o.status = :status AND o.createdAt >= :startTime " +
           "GROUP BY DATE(o.createdAt) ORDER BY DATE(o.createdAt) DESC")
    List<Object[]> findDailyIncomeStats(@Param("status") Order.Status status, @Param("startTime") LocalDateTime startTime);

    List<Order> findByStatusAndCreatedAtBefore(Order.Status status, LocalDateTime createdAt);
}
