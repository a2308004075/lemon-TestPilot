package com.testpilot.repository;

import com.testpilot.entity.RunStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RunStepRepository extends JpaRepository<RunStep, Long> {

    List<RunStep> findByRunIdOrderBySeqAsc(Long runId);
}
