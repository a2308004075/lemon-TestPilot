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
@Table(name = "test_report")
public class TestReport extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_no")
    private String reportNo;

    @Column(name = "title")
    private String title;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "version")
    private String version;

    @Column(name = "modules")
    private String modules;

    /** 可上线 / 有条件上线 / 不可上线 */
    @Column(name = "conclusion")
    private String conclusion;

    @Column(name = "pass_count")
    private Integer passCount;

    @Column(name = "fail_count")
    private Integer failCount;

    @Column(name = "block_count")
    private Integer blockCount;

    @Column(name = "report_date")
    private LocalDate reportDate;

    @Column(name = "content_json", columnDefinition = "LONGTEXT")
    private String contentJson;

    @Column(name = "source_task_ids")
    private String sourceTaskIds;
}
