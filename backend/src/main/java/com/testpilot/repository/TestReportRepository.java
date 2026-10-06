package com.testpilot.repository;

import com.testpilot.entity.TestReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestReportRepository extends JpaRepository<TestReport, Long> {

    List<TestReport> findAllByOrderByIdDesc();
}
