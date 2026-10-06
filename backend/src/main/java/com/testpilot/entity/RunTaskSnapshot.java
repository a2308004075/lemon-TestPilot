package com.testpilot.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 执行记录的路由任务快照：保留任务输出当时快照。
 */
@Data
@Entity
@Table(name = "run_task_snapshot")
public class RunTaskSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id")
    private Long runId;

    @Column(name = "task_type")
    private String taskType;

    @Column(name = "task_name")
    private String taskName;

    /** main / aux */
    @Column(name = "role")
    private String role;

    @Column(name = "seq")
    private Integer seq;

    @Column(name = "completeness")
    private Integer completeness;

    @Column(name = "missing_json", columnDefinition = "VARCHAR(1000)")
    private String missingJson;

    @Column(name = "output_json", columnDefinition = "LONGTEXT")
    private String outputJson;
}
