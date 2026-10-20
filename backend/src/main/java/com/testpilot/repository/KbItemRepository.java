package com.testpilot.repository;

import com.testpilot.entity.KbItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KbItemRepository extends JpaRepository<KbItem, Long> {

    List<KbItem> findAllByOrderByIdAsc();

    List<KbItem> findByStatus(String status);

    List<KbItem> findByCategory(String category);

    List<KbItem> findByStatusAndCategoryOrderByIdAsc(String status, String category);

    long countByStatus(String status);

    long countByStatusAndCategoryIn(String status, List<String> categories);

    /** 已入库经验：来源于分析任务的知识 */
    @Query("select k from KbItem k where k.sourceTask <> '人工维护' order by k.updatedAt desc")
    List<KbItem> findTaskSourced();

    /** 知识引用检索：按分类匹配（仅已发布；语义/关键词排序由 RetrievalService 完成） */
    @Query("select k from KbItem k where k.status = '已发布' and k.category in :categories order by k.id asc")
    List<KbItem> findPublishedByCategories(@Param("categories") List<String> categories);
}
