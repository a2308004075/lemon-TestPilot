package com.testpilot.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 检索命中软引用：可追溯，不参与撤销保护（与硬引用 ref_count 区分）。
 */
@Data
@Entity
@Table(name = "kb_retrieval_hit")
public class KbRetrievalHit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 执行记录 run_record.id */
    @Column(name = "run_id")
    private Long runId;

    /** 路由任务类型（taskType，如 testcase_gen） */
    @Column(name = "route_id")
    private String routeId;

    /** 知识主键 kb_item.id */
    @Column(name = "kb_id")
    private Long kbId;

    /** 命中时标题快照 */
    @Column(name = "title_snapshot")
    private String titleSnapshot;

    /** 命中时版本快照 */
    @Column(name = "version_snapshot")
    private Integer versionSnapshot;

    /** 相似度 / 关键词得分 */
    @Column(name = "score")
    private BigDecimal score;

    /** semantic | keyword */
    @Column(name = "method")
    private String method;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
