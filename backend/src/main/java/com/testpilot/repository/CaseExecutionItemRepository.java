package com.testpilot.repository;

import com.testpilot.entity.CaseExecutionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CaseExecutionItemRepository extends JpaRepository<CaseExecutionItem, Long> {

    List<CaseExecutionItem> findByBatchIdOrderBySeqOrderAsc(Long batchId);

    void deleteByBatchId(Long batchId);
}
