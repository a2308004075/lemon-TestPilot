package com.testpilot.service;

import com.testpilot.entity.KbEmbedding;
import com.testpilot.entity.KbItem;
import com.testpilot.entity.KbRetrievalHit;
import com.testpilot.entity.SysLlmConfig;
import com.testpilot.repository.KbEmbeddingRepository;
import com.testpilot.repository.KbItemRepository;
import com.testpilot.repository.KbRetrievalHitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 检索服务：三级降级（semantic → keyword → none），对齐详细设计 4.6.2。
 * 候选集固定为「已发布 + 任务分类白名单」；分类为空（prompt_test）→ 直接 none（不消费知识库）。
 * 命中落 kb_retrieval_hit 软引用（可追溯，不参与撤销保护）。
 */
@Service
public class RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);

    public static final String METHOD_SEMANTIC = "semantic";
    public static final String METHOD_KEYWORD = "keyword";
    public static final String METHOD_NONE = "none";

    /** 语义相似度阈值：低于阈值视为未命中（宁可检索不到，不用弱相关冒充事实） */
    public static final double SEMANTIC_THRESHOLD = 0.30;
    public static final int TOP_K = 5;

    private static final int MAX_TOKENS = 800;
    private static final int TITLE_WEIGHT = 3;
    private static final int BODY_WEIGHT = 1;
    private static final int QUERY_LIMIT = 4000;
    private static final int TITLE_SNAPSHOT_LIMIT = 200;
    private static final Pattern EN_TOKEN = Pattern.compile("[a-z]{2,31}");
    private static final Pattern CJK_SEG = Pattern.compile("[\\u4e00-\\u9fa5]+");

    @Autowired
    private KbItemRepository kbRepo;
    @Autowired
    private KbEmbeddingRepository embeddingRepo;
    @Autowired
    private KbRetrievalHitRepository hitRepo;
    @Autowired
    private EmbeddingService embeddingService;

    /** 单条命中 */
    public static class Hit {
        public final KbItem kb;
        public final double score;
        public final String method;

        public Hit(KbItem kb, double score, String method) {
            this.kb = kb;
            this.score = score;
            this.method = method;
        }
    }

    /** 检索结果；method=none 为显式未命中（前端展示"未检索到相关知识（未虚构）"） */
    public static class Result {
        public final String method;
        public final List<Hit> hits;

        public Result(String method, List<Hit> hits) {
            this.method = method;
            this.hits = hits;
        }

        public List<KbItem> items() {
            List<KbItem> list = new ArrayList<KbItem>();
            for (Hit h : hits) {
                list.add(h.kb);
            }
            return list;
        }
    }

    // ---------------- 对外入口 ----------------

    /** 任务执行链路入口：文本 × 分类白名单 */
    public Result retrieve(String text, List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return none();
        }
        return retrieveFrom(text, kbRepo.findPublishedByCategories(categories));
    }

    /** 候选集自定义入口（案例验证 / 人侧搜索复用同一降级链路） */
    public Result retrieveFrom(String text, List<KbItem> candidates) {
        if (candidates == null || candidates.isEmpty() || text == null || text.trim().isEmpty()) {
            return none();
        }
        if (embeddingService.isEmbeddingEnabled()) {
            List<Hit> semantic = semanticHits(text, candidates);
            if (!semantic.isEmpty()) {
                return new Result(METHOD_SEMANTIC, semantic);
            }
        }
        List<Hit> keyword = keywordHits(text, candidates);
        if (!keyword.isEmpty()) {
            return new Result(METHOD_KEYWORD, keyword);
        }
        return none();
    }

    /** 人侧搜索（知识库页）：mode=keyword 直走关键词；其余走三级降级 */
    public Map<String, Object> search(String q, String mode) {
        String text = q == null ? "" : q.trim();
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        if (text.isEmpty()) {
            result.put("method", METHOD_NONE);
            result.put("list", new ArrayList<Map<String, Object>>());
            return result;
        }
        Result r;
        if (METHOD_KEYWORD.equals(mode)) {
            List<Hit> hits = keywordHits(text, kbRepo.findByStatus("已发布"));
            r = hits.isEmpty() ? none() : new Result(METHOD_KEYWORD, hits);
        } else {
            r = retrieveFrom(text, kbRepo.findByStatus("已发布"));
        }
        result.put("method", r.method);
        result.put("list", toHitMaps(r.hits));
        return result;
    }

    /** 命中落库（软引用）：任务执行链路专用；失败不阻断业务 */
    public void recordHits(Long runId, String routeId, Result result) {
        if (runId == null || result == null || result.hits.isEmpty()) {
            return;
        }
        try {
            for (Hit h : result.hits) {
                KbRetrievalHit hit = new KbRetrievalHit();
                hit.setRunId(runId);
                hit.setRouteId(routeId == null ? "" : routeId);
                hit.setKbId(h.kb.getId());
                String title = h.kb.getTitle() == null ? "" : h.kb.getTitle();
                hit.setTitleSnapshot(title.length() > TITLE_SNAPSHOT_LIMIT
                        ? title.substring(0, TITLE_SNAPSHOT_LIMIT) : title);
                hit.setVersionSnapshot(h.kb.getVersion() == null ? 0 : h.kb.getVersion());
                hit.setScore(round4(h.score));
                hit.setMethod(h.method);
                hitRepo.save(hit);
            }
        } catch (Exception e) {
            log.warn("检索命中落库失败 runId={}：{}", runId, e.getMessage());
        }
    }

    // ---------------- 三级内部实现 ----------------

    private List<Hit> semanticHits(String text, List<KbItem> candidates) {
        SysLlmConfig cfg = embeddingService.findEmbeddingProvider();
        if (cfg == null) {
            return new ArrayList<Hit>();
        }
        float[] queryVector;
        try {
            String query = text.trim();
            queryVector = embeddingService.embed(cfg,
                    query.length() > QUERY_LIMIT ? query.substring(0, QUERY_LIMIT) : query);
        } catch (Exception e) {
            log.warn("查询向量生成失败，降级关键词检索：{}", e.getMessage());
            return new ArrayList<Hit>();
        }
        // 懒补偿在候选准备阶段完成；补偿失败的条目仅缺席本次 semantic，不报错
        embeddingService.lazyCompensate(candidates);
        Map<Long, KbEmbedding> rows = loadEmbeddings(candidates);
        List<Hit> hits = new ArrayList<Hit>();
        for (KbItem kb : candidates) {
            KbEmbedding row = rows.get(kb.getId());
            if (!EmbeddingService.isUsable(row, kb, cfg.getModelName())) {
                continue;
            }
            float[] kv = EmbeddingService.deserialize(row.getVector());
            if (kv == null || kv.length != queryVector.length) {
                continue;
            }
            double score = EmbeddingService.cosine(queryVector, kv);
            if (score >= SEMANTIC_THRESHOLD) {
                hits.add(new Hit(kb, score, METHOD_SEMANTIC));
            }
        }
        return topK(hits);
    }

    private List<Hit> keywordHits(String text, List<KbItem> candidates) {
        List<String> tokens = tokenize(text);
        if (tokens.isEmpty()) {
            return new ArrayList<Hit>();
        }
        List<Hit> hits = new ArrayList<Hit>();
        for (KbItem kb : candidates) {
            double score = keywordScore(tokens, kb);
            if (score > 0) {
                hits.add(new Hit(kb, score, METHOD_KEYWORD));
            }
        }
        return topK(hits);
    }

    private Map<Long, KbEmbedding> loadEmbeddings(List<KbItem> candidates) {
        Map<Long, KbEmbedding> rows = new LinkedHashMap<Long, KbEmbedding>();
        List<Long> ids = new ArrayList<Long>();
        for (KbItem kb : candidates) {
            ids.add(kb.getId());
        }
        if (ids.isEmpty()) {
            return rows;
        }
        for (KbEmbedding e : embeddingRepo.findByKbIdIn(ids)) {
            rows.put(e.getKbId(), e);
        }
        return rows;
    }

    private static Result none() {
        return new Result(METHOD_NONE, new ArrayList<Hit>());
    }

    // ---------------- 分词 / 评分 / 排序（静态，单测直接覆盖） ----------------

    /** 分词：中文连续段 2~3 字滑窗 n-gram + 英文词元（[a-zA-Z]{2,}），去重保序，上限 800 */
    public static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<String>();
        if (text == null || text.trim().isEmpty()) {
            return tokens;
        }
        Set<String> seen = new LinkedHashSet<String>();
        String lower = text.toLowerCase();
        Matcher en = EN_TOKEN.matcher(lower);
        while (en.find() && tokens.size() < MAX_TOKENS) {
            String t = en.group();
            if (seen.add(t)) {
                tokens.add(t);
            }
        }
        Matcher cjk = CJK_SEG.matcher(lower);
        while (cjk.find() && tokens.size() < MAX_TOKENS) {
            String seg = cjk.group();
            boolean full = false;
            for (int n = 2; n <= 3 && !full; n++) {
                for (int i = 0; i + n <= seg.length(); i++) {
                    String t = seg.substring(i, i + n);
                    if (seen.add(t)) {
                        tokens.add(t);
                    }
                    if (tokens.size() >= MAX_TOKENS) {
                        full = true;
                        break;
                    }
                }
            }
        }
        return tokens;
    }

    /** 关键词得分 = Σ（词元 ∈ 标题 ? 3 : 正文（摘要+正文）含 ? 1 : 0） */
    static double keywordScore(List<String> tokens, KbItem kb) {
        String title = kb.getTitle() == null ? "" : kb.getTitle().toLowerCase();
        StringBuilder bodySb = new StringBuilder();
        if (kb.getSummary() != null) {
            bodySb.append(kb.getSummary()).append('\n');
        }
        if (kb.getBody() != null) {
            bodySb.append(kb.getBody());
        }
        String body = bodySb.toString().toLowerCase();
        double score = 0d;
        for (String token : tokens) {
            if (title.contains(token)) {
                score += TITLE_WEIGHT;
            } else if (body.contains(token)) {
                score += BODY_WEIGHT;
            }
        }
        return score;
    }

    /** Top5 稳定排序：score 降序、id 升序 */
    static List<Hit> topK(List<Hit> hits) {
        List<Hit> sorted = new ArrayList<Hit>(hits);
        sorted.sort((a, b) -> {
            int cmp = Double.compare(b.score, a.score);
            if (cmp != 0) {
                return cmp;
            }
            return Long.compare(a.kb.getId(), b.kb.getId());
        });
        if (sorted.size() > TOP_K) {
            return new ArrayList<Hit>(sorted.subList(0, TOP_K));
        }
        return sorted;
    }

    private static List<Map<String, Object>> toHitMaps(List<Hit> hits) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Hit h : hits) {
            list.add(toHitMap(h));
        }
        return list;
    }

    public static Map<String, Object> toHitMap(Hit h) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", h.kb.getId());
        m.put("kbNo", h.kb.getKbNo());
        m.put("title", h.kb.getTitle());
        m.put("category", h.kb.getCategory());
        m.put("version", h.kb.getVersion());
        m.put("risk", h.kb.getRisk());
        m.put("score", round4(h.score));
        m.put("method", h.method);
        return m;
    }

    static BigDecimal round4(double v) {
        return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP);
    }
}
