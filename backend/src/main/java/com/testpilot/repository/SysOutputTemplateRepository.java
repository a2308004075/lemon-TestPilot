package com.testpilot.repository;

import com.testpilot.entity.SysOutputTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SysOutputTemplateRepository extends JpaRepository<SysOutputTemplate, Long> {

    List<SysOutputTemplate> findByTaskTypeAndVisibleTrueOrderBySortOrderAsc(String taskType);

    List<SysOutputTemplate> findByTaskTypeOrderBySortOrderAsc(String taskType);

    Optional<SysOutputTemplate> findByTaskTypeAndFieldName(String taskType, String fieldName);
}
