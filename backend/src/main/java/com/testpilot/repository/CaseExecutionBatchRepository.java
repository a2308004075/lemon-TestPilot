package com.testpilot.repository;

import com.testpilot.entity.CaseExecutionBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CaseExecutionBatchRepository extends JpaRepository<CaseExecutionBatch, Long> {

    List<CaseExecutionBatch> findAllByOrderByIdDesc();

    /** 查询某日最大批次号（如 20261013%） */
    @Query("SELECT MAX(b.batchNo) FROM CaseExecutionBatch b WHERE b.batchNo LIKE ?1%")
    String findMaxBatchNoByPrefix(String prefix);
}
