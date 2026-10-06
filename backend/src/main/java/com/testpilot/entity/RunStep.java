package com.testpilot.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "run_step")
public class RunStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id")
    private Long runId;

    @Column(name = "seq")
    private Integer seq;

    @Column(name = "task_type")
    private String taskType;

    @Column(name = "step_name")
    private String stepName;

    /** done / skipped */
    @Column(name = "status")
    private String status;

    @Column(name = "detail")
    private String detail;
}
