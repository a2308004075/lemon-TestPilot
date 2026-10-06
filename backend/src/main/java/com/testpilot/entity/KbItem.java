package com.testpilot.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "kb_item")
public class KbItem extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kb_no")
    private String kbNo;

    @Column(name = "title")
    private String title;

    @Column(name = "category")
    private String category;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "module_name")
    private String moduleName;

    @Column(name = "submodule_name")
    private String submoduleName;

    @Column(name = "risk")
    private String risk;

    @Column(name = "version")
    private Integer version;

    /** 待审核 / 已发布 / 已驳回 / 已撤销 */
    @Column(name = "status")
    private String status;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "source_task")
    private String sourceTask;

    @Column(name = "review_note")
    private String reviewNote;

    /** 被引用次数，撤销入库会被阻止 */
    @Column(name = "ref_count")
    private Integer refCount;
}
