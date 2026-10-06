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
@Table(name = "sys_output_template")
public class SysOutputTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_type")
    private String taskType;

    @Column(name = "field_name")
    private String fieldName;

    @Column(name = "field_label")
    private String fieldLabel;

    @Column(name = "required_flag")
    private Boolean requiredFlag;

    @Column(name = "sort_order")
    private Integer sortOrder;

    /** 删除字段仅隐藏（visible=0），原始快照仍保留 */
    @Column(name = "visible")
    private Boolean visible;
}
