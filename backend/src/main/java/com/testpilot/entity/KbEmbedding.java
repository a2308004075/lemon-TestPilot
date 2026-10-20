package com.testpilot.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 知识向量缓存：float32 小端序列化 BLOB，一行一知识覆盖式更新。
 */
@Data
@Entity
@Table(name = "kb_embedding")
public class KbEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 知识主键 kb_item.id */
    @Column(name = "kb_id")
    private Long kbId;

    /** 生成向量时的知识版本；与当前版本不符即视为过期（宁可检索不到不用旧向量） */
    @Column(name = "kb_version")
    private Integer kbVersion;

    @Column(name = "vector", columnDefinition = "LONGBLOB")
    private byte[] vector;

    /** 嵌入模型名 */
    @Column(name = "vector_model")
    private String vectorModel;

    @Column(name = "dim")
    private Integer dim;

    /** ready | pending（重嵌失败置 pending，可查） */
    @Column(name = "status")
    private String status;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
