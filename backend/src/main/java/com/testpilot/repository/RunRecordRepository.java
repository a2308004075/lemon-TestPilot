package com.testpilot.repository;

import com.testpilot.entity.RunRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RunRecordRepository extends JpaRepository<RunRecord, Long> {

    List<RunRecord> findAllByOrderByUpdatedAtDescIdDesc();

    List<RunRecord> findByStatus(String status);

    long countByStatus(String status);

    long countByReviewStatus(String reviewStatus);

    List<RunRecord> findTop5ByOrderByUpdatedAtDescIdDesc();
}
