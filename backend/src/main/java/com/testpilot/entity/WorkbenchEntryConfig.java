package com.testpilot.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 工作台入口配置版本：修改后版本号递增，历史执行记录保留当时快照。
 */
@Data
@Entity
@Table(name = "workbench_entry_config")
public class WorkbenchEntryConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_id")
    private Long entryId;

    @Column(name = "task_type")
    private String taskType;

    @Column(name = "version")
    private Integer version;

    /** 触发词（每行一项） */
    @Column(name = "trigger_words")
    private String triggerWords;

    /** 必填信息（每行一项） */
    @Column(name = "required_fields")
    private String requiredFields;

    /** 知识库引用映射（每行一项） */
    @Column(name = "kb_mappings")
    private String kbMappings;

    /** 工作流步骤（严格顺序，每行一项） */
    @Column(name = "workflow_steps")
    private String workflowSteps;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;
}
