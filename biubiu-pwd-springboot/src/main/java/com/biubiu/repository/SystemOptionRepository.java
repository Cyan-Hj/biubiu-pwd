package com.biubiu.repository;

import com.biubiu.entity.SystemOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemOptionRepository extends JpaRepository<SystemOption, Long> {
    List<SystemOption> findByTypeOrderBySortOrderAsc(SystemOption.OptionType type);

    boolean existsByTypeAndValue(SystemOption.OptionType type, String value);
}
