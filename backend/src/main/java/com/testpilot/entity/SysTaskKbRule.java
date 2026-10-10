package com.testpilot.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 任务与知识关联规则：任务发布知识后自动进入对应分类的检索范围。
 */
@Data
@Entity
@Table(name = "sys_task_kb_rule")
public class SysTaskKbRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "main_task_type")
    private String mainTaskType;

    @Column(name = "category")
    private String category;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "enabled")
    private Boolean enabled;
}
