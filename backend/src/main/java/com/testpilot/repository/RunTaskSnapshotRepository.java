package com.testpilot.repository;

import com.testpilot.entity.RunTaskSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RunTaskSnapshotRepository extends JpaRepository<RunTaskSnapshot, Long> {

    List<RunTaskSnapshot> findByRunIdOrderBySeqAsc(Long runId);
}
