package com.testpilot.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "regression_list")
public class RegressionList extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title")
    private String title;

    @Column(name = "version")
    private String version;

    /** 执行中 / 已完成 */
    @Column(name = "status")
    private String status;

    @Column(name = "progress")
    private Integer progress;

    @Column(name = "created_date")
    private LocalDate createdDate;
}
