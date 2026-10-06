package com.testpilot.repository;

import com.testpilot.entity.WorkbenchEntryConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkbenchEntryConfigRepository extends JpaRepository<WorkbenchEntryConfig, Long> {

    Optional<WorkbenchEntryConfig> findTopByTaskTypeOrderByVersionDesc(String taskType);

    List<WorkbenchEntryConfig> findByTaskTypeOrderByVersionDesc(String taskType);
}
