package com.testpilot.repository;

import com.testpilot.entity.SysTaskKbRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysTaskKbRuleRepository extends JpaRepository<SysTaskKbRule, Long> {

    List<SysTaskKbRule> findByMainTaskTypeOrderBySortOrderAsc(String mainTaskType);

    void deleteByMainTaskType(String mainTaskType);
}
