package com.testpilot.repository;

import com.testpilot.entity.TaskRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRecordRepository extends JpaRepository<TaskRecord, Long> {

    List<TaskRecord> findAllByOrderByUpdatedAtDescIdDesc();

    List<TaskRecord> findByTaskTypeOrderByUpdatedAtDescIdDesc(String taskType);

    Optional<TaskRecord> findByTaskNo(String taskNo);

    long countByStatus(String status);

    long countByReviewStatus(String reviewStatus);

    List<TaskRecord> findTop5ByOrderByUpdatedAtDescIdDesc();
}
