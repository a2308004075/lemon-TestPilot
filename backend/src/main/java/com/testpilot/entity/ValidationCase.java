package com.testpilot.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "validation_case")
public class ValidationCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_no")
    private String caseNo;

    @Column(name = "title")
    private String title;

    @Column(name = "material", columnDefinition = "TEXT")
    private String material;

    /** 预期路由（逗号分隔） */
    @Column(name = "expected_routes")
    private String expectedRoutes;

    /** 预期检查（每行一项） */
    @Column(name = "expected_checks")
    private String expectedChecks;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    /** 通过 / 未通过 */
    @Column(name = "last_result")
    private String lastResult;

    @Column(name = "last_routes")
    private String lastRoutes;
}
