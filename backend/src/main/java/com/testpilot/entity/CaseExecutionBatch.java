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
 * 用例执行批次（6.11 W3）：勾选用例创建，逐条登记执行结果。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "case_execution_batch")
public class CaseExecutionBatch extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 批次号：YYYYMMDDNNN（如 20261013001） */
    @Column(name = "batch_no")
    private String batchNo;

    @Column(name = "title")
    private String title;

    /** 执行中 / 已完成 */
    @Column(name = "status")
    private String status;

    @Column(name = "total")
    private Integer total;

    @Column(name = "passed")
    private Integer passed;

    @Column(name = "failed")
    private Integer failed;

    @Column(name = "blocked")
    private Integer blocked;

    @Column(name = "skipped")
    private Integer skipped;
}
