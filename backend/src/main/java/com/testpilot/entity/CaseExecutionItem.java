package com.testpilot.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 用例执行条目：创建批次时快照用例编号/标题/优先级，执行时逐条登记结果。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "case_execution_item")
public class CaseExecutionItem extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id")
    private Long batchId;

    /** 顺序（按勾选顺序） */
    @Column(name = "seq_order")
    private Integer seqOrder;

    /** 用例ID（联动 test_case.status） */
    @Column(name = "case_id")
    private Long caseId;

    @Column(name = "case_no_snapshot")
    private String caseNoSnapshot;

    @Column(name = "title_snapshot")
    private String titleSnapshot;

    @Column(name = "priority_snapshot")
    private String prioritySnapshot;

    /** 待执行 / 通过 / 失败 / 阻塞 / 跳过 */
    @Column(name = "status")
    private String status;

    @Column(name = "note")
    private String note;

    /** 执行时间（待执行为空） */
    @Column(name = "executed_at")
    private LocalDateTime executedAt;
}
