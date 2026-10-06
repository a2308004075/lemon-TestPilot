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
 * 分析任务记录：Bug 分析 / 日志排查 / SQL 分析 / 用例生成等单页分析共用。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "task_record")
public class TaskRecord extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_no")
    private String taskNo;

    @Column(name = "title")
    private String title;

    @Column(name = "task_type")
    private String taskType;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "module_name")
    private String moduleName;

    @Column(name = "submodule_name")
    private String submoduleName;

    @Column(name = "risk")
    private String risk;

    @Column(name = "context", columnDefinition = "TEXT")
    private String context;

    /** 待补充 / 已完成 */
    @Column(name = "status")
    private String status;

    /** 待复核 / 通过 / 驳回补充 */
    @Column(name = "review_status")
    private String reviewStatus;

    @Column(name = "completeness")
    private Integer completeness;

    @Column(name = "output_json", columnDefinition = "LONGTEXT")
    private String outputJson;

    /** rule / llm */
    @Column(name = "engine_mode")
    private String engineMode;

    /** manual / assistant */
    @Column(name = "source")
    private String source;
}
