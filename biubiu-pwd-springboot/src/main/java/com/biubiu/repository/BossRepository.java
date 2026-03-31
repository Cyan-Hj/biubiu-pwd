package com.biubiu.repository;

import com.biubiu.entity.Boss;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BossRepository extends JpaRepository<Boss, Long> {

    @Query("SELECT b FROM Boss b WHERE " +
           "(:keyword IS NULL OR b.name LIKE %:keyword% OR b.contactValue LIKE %:keyword%) AND " +
           "(:vipLevel IS NULL OR b.vipLevel = :vipLevel) AND " +
           "(:customerType IS NULL OR b.customerType = :customerType) AND " +
           "(:enabled IS NULL OR b.enabled = :enabled)")
    Page<Boss> findByConditions(@Param("keyword") String keyword,
                                @Param("vipLevel") Integer vipLevel,
                                @Param("customerType") Boss.CustomerType customerType,
                                @Param("enabled") Boolean enabled,
                                Pageable pageable);

    Optional<Boss> findByName(String name);

    boolean existsByName(String name);
}
