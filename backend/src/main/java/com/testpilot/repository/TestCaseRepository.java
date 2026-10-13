package com.testpilot.repository;

import com.testpilot.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    List<TestCase> findAllByOrderByCreatedDateDescIdDesc();

    List<TestCase> findByPriorityInAndStatusIn(List<String> priorities, List<String> statuses);

    List<TestCase> findByPriority(String priority);

    long countByPriority(String priority);

    long countByStatus(String status);
}
