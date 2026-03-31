package com.biubiu.repository;

import com.biubiu.entity.VipLevelConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VipLevelConfigRepository extends JpaRepository<VipLevelConfig, Long> {
    List<VipLevelConfig> findAllByOrderBySortOrderAsc();

    Optional<VipLevelConfig> findByLevel(Integer level);

    boolean existsByLevel(Integer level);
}
