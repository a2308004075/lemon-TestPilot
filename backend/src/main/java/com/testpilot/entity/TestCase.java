package com.testpilot.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "test_case")
public class TestCase extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_no")
    private String caseNo;

    @Column(name = "title")
    private String title;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "module_name")
    private String moduleName;

    @Column(name = "submodule_name")
    private String submoduleName;

    @Column(name = "priority")
    private String priority;

    @Column(name = "type")
    private String type;

    @Column(name = "preconditions", columnDefinition = "TEXT")
    private String preconditions;

    @Column(name = "steps", columnDefinition = "TEXT")
    private String steps;

    @Column(name = "expected", columnDefinition = "TEXT")
    private String expected;

    @Column(name = "test_data", columnDefinition = "TEXT")
    private String testData;

    /** 未执行 / 通过 / 失败 / 阻塞 */
    @Column(name = "status")
    private String status;

    @Column(name = "version")
    private Integer version;

    @Column(name = "linked_bug_no")
    private String linkedBugNo;

    @Column(name = "created_date")
    private LocalDate createdDate;
}
