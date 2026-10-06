package com.testpilot.repository;

import com.testpilot.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    List<TestCase> findAllByOrderByCreatedDateDescIdDesc();

    List<TestCase> findByPriorityInAndStatusIn(List<String> priorities, List<String> statuses);

    List<TestCase> findByPriority(String priority);

    long countByPriority(String priority);
}
