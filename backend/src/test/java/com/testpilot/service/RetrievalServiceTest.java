package com.testpilot.service;

import com.testpilot.entity.KbItem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分词 / 关键词评分 / Top5 排序单测（纯算法，不依赖 Spring 与数据库）。
 */
class RetrievalServiceTest {

    @Test
    void tokenizeExtractsChineseBigramAndTrigram() {
        List<String> tokens = RetrievalService.tokenize("支付回调");
        assertTrue(tokens.contains("支付"));
        assertTrue(tokens.contains("付回"));
        assertTrue(tokens.contains("回调"));
        assertTrue(tokens.contains("支付回"));
        assertTrue(tokens.contains("付回调"));
    }

    @Test
    void tokenizeExtractsEnglishTokens() {
        List<String> tokens = RetrievalService.tokenize("SQL timeout abc1");
        assertTrue(tokens.contains("sql"));
        assertTrue(tokens.contains("timeout"));
        // 英文词元只取连续字母段（2~31 个字母），"abc1" 拆为 "abc"
        assertTrue(tokens.contains("abc"));
        assertFalse(tokens.contains("abc1"));
    }

    @Test
    void tokenizeDeduplicates() {
        List<String> tokens = RetrievalService.tokenize("测试测试 test test");
        assertEquals(tokens.size(), new LinkedHashSet<String>(tokens).size());
    }

    @Test
    void keywordScorePrefersTitleHits() {
        KbItem kb = new KbItem();
        kb.setTitle("支付回调重复消费");
        kb.setBody("正文内容");
        // "支付" 命中标题 → 3 分；"正文内容" 只在正文 → 1 分
        double score = RetrievalService.keywordScore(Arrays.asList("支付", "正文内容"), kb);
        assertEquals(4.0, score, 1e-9);
    }

    @Test
    void keywordScoreCountsBodyOnlyOnce() {
        KbItem kb = new KbItem();
        kb.setTitle("无关标题");
        kb.setSummary("摘要包含幂等");
        kb.setBody("正文也包含幂等");
        // "幂等" 在摘要与正文都出现，只计 1 分
        assertEquals(1.0, RetrievalService.keywordScore(Arrays.asList("幂等"), kb), 1e-9);
    }

    @Test
    void topKOrdersByScoreDescThenIdAscAndKeepsFive() {
        List<RetrievalService.Hit> hits = new ArrayList<RetrievalService.Hit>();
        for (int i = 1; i <= 7; i++) {
            KbItem kb = new KbItem();
            kb.setId((long) i);
            hits.add(new RetrievalService.Hit(kb, 10 - i, RetrievalService.METHOD_KEYWORD));
        }
        List<RetrievalService.Hit> top = RetrievalService.topK(hits);
        assertEquals(5, top.size());
        assertEquals(10.0 - 1, top.get(0).score, 1e-9);
        assertEquals(1L, top.get(0).kb.getId());
        assertEquals(10.0 - 5, top.get(4).score, 1e-9);
    }

    @Test
    void topKKeepsSameScoreOrderedByIdAsc() {
        KbItem first = new KbItem();
        first.setId(2L);
        KbItem second = new KbItem();
        second.setId(1L);
        List<RetrievalService.Hit> hits = Arrays.asList(
                new RetrievalService.Hit(first, 5.0, RetrievalService.METHOD_KEYWORD),
                new RetrievalService.Hit(second, 5.0, RetrievalService.METHOD_KEYWORD));
        List<RetrievalService.Hit> top = RetrievalService.topK(hits);
        assertEquals(1L, top.get(0).kb.getId());
        assertEquals(2L, top.get(1).kb.getId());
    }
}
