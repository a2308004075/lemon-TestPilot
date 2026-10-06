package com.testpilot.repository;

import com.testpilot.entity.SysTaskRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysTaskRuleRepository extends JpaRepository<SysTaskRule, Long> {

    List<SysTaskRule> findByMainTaskTypeOrderBySortOrderAsc(String mainTaskType);

    List<SysTaskRule> findAllByOrderByMainTaskTypeAscSortOrderAsc();
}
