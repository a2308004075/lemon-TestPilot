package com.testpilot.repository;

import com.testpilot.entity.KbEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KbEmbeddingRepository extends JpaRepository<KbEmbedding, Long> {

    Optional<KbEmbedding> findByKbId(Long kbId);

    List<KbEmbedding> findByKbIdIn(List<Long> kbIds);

    /** 非当前模型的过期模型列表（如换过嵌入模型） */
    @Query("select distinct e.vectorModel from KbEmbedding e where e.vectorModel <> :currentModel")
    List<String> findStaleModels(@Param("currentModel") String currentModel);
}
