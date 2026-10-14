package com.testpilot.repository;

import com.testpilot.entity.CaseExecutionBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CaseExecutionBatchRepository extends JpaRepository<CaseExecutionBatch, Long> {

    List<CaseExecutionBatch> findAllByOrderByIdDesc();

    boolean existsByBatchNo(String batchNo);
}
