package com.biubiu.repository;

import com.biubiu.entity.LevelUpgradeApplication;
import com.biubiu.entity.LevelUpgradeApplication.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LevelUpgradeApplicationRepository extends JpaRepository<LevelUpgradeApplication, Long> {

    List<LevelUpgradeApplication> findByPlayerIdOrderByCreatedAtDesc(Long playerId);

    List<LevelUpgradeApplication> findByStatusOrderByCreatedAtDesc(ApplicationStatus status);

    List<LevelUpgradeApplication> findAllByOrderByCreatedAtDesc();

    boolean existsByPlayerIdAndStatus(Long playerId, ApplicationStatus status);

    @Query("SELECT a FROM LevelUpgradeApplication a WHERE a.status = 'PENDING' ORDER BY a.createdAt DESC")
    List<LevelUpgradeApplication> findPendingApplications();

    long countByStatus(ApplicationStatus status);
}
