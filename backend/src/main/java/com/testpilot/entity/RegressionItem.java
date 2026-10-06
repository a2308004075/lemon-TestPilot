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
@Table(name = "regression_item")
public class RegressionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "list_id")
    private Long listId;

    @Column(name = "seq")
    private Integer seq;

    @Column(name = "title")
    private String title;

    @Column(name = "priority")
    private String priority;

    /** case / bug / rule */
    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "source_ref")
    private String sourceRef;

    @Column(name = "module_label")
    private String moduleLabel;

    /** 未执行 / 通过 / 失败 / 阻塞 */
    @Column(name = "status")
    private String status;
}
