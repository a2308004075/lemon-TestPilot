package com.testpilot.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 操作审计：详情抽屉"操作记录"时间线数据源。
 */
@Data
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** task / case / kb / regression / report / module / llm */
    @Column(name = "entity_type")
    private String entityType;

    /** 业务编号（taskNo / caseNo / kbNo 等） */
    @Column(name = "entity_no")
    private String entityNo;

    @Column(name = "action")
    private String action;

    @Column(name = "detail")
    private String detail;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
