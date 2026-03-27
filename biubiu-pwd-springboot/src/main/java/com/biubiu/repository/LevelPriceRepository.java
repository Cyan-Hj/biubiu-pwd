package com.biubiu.repository;

import com.biubiu.entity.LevelPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LevelPriceRepository extends JpaRepository<LevelPrice, Long> {
    Optional<LevelPrice> findByLevel(String level);
    
    boolean existsByLevel(String level);
    
    List<LevelPrice> findAllByOrderBySortOrderAsc();
    
    List<LevelPrice> findBySortOrderGreaterThanEqual(Integer sortOrder);
}
