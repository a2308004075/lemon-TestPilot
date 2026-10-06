package com.testpilot.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 执行记录：AI 测试助手一次完整流程。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "run_record")
public class RunRecord extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_no")
    private String runNo;

    @Column(name = "title")
    private String title;

    @Column(name = "main_task")
    private String mainTask;

    @Column(name = "route_count")
    private Integer routeCount;

    @Column(name = "step_count")
    private Integer stepCount;

    @Column(name = "risk")
    private String risk;

    /** 待补充 / 已完成 */
    @Column(name = "status")
    private String status;

    @Column(name = "missing_count")
    private Integer missingCount;

    /** 待复核 / 通过 / 驳回补充 */
    @Column(name = "review_status")
    private String reviewStatus;

    @Column(name = "input_text", columnDefinition = "TEXT")
    private String inputText;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "module_name")
    private String moduleName;

    @Column(name = "submodule_name")
    private String submoduleName;

    /** rule / llm */
    @Column(name = "engine_mode")
    private String engineMode;
}
