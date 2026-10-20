package com.testpilot.repository;

import com.testpilot.entity.KbRetrievalHit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KbRetrievalHitRepository extends JpaRepository<KbRetrievalHit, Long> {

    /** 软引用影响分析：被检索命中总次数 */
    long countByKbId(Long kbId);

    /** 软引用影响分析：最近 5 次命中 */
    List<KbRetrievalHit> findTop5ByKbIdOrderByCreatedAtDescIdDesc(Long kbId);
}
