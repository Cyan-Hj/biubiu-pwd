package com.biubiu.repository;

import com.biubiu.entity.BossRechargeRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BossRechargeRecordRepository extends JpaRepository<BossRechargeRecord, Long> {
    Page<BossRechargeRecord> findByBossIdOrderByCreatedAtDesc(Long bossId, Pageable pageable);
}
