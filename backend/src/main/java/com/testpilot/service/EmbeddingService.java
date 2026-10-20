package com.testpilot.service;

import com.testpilot.common.BizException;
import com.testpilot.common.CryptoService;
import com.testpilot.common.JsonUtil;
import com.testpilot.entity.KbEmbedding;
import com.testpilot.entity.KbItem;
import com.testpilot.entity.SysLlmConfig;
import com.testpilot.repository.KbEmbeddingRepository;
import com.testpilot.repository.KbItemRepository;
import com.testpilot.repository.SysLlmConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 嵌入服务：调用 OpenAI 兼容 /embeddings 接口生成知识向量（float32 小端序列化 BLOB）。
 * 未配置 embedding 用途厂商或调用失败不阻断业务：失败置 pending 可查、可重嵌，检索层自动降级关键词。
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    /** 用途隔离：向量生成只取 purpose='embedding' 的启用配置 */
    public static final String PURPOSE_EMBEDDING = "embedding";
    public static final String STATUS_READY = "ready";
    public static final String STATUS_PENDING = "pending";

    private static final int LAZY_LIMIT = 16;
    private static final int EMBED_TEXT_LIMIT = 2000;

    @Autowired
    private SysLlmConfigRepository llmRepo;
    @Autowired
    private KbEmbeddingRepository embeddingRepo;
    @Autowired
    private KbItemRepository kbRepo;
    @Autowired
    private CryptoService cryptoService;

    // ---------------- 序列化 / 相似度（静态，单测直接覆盖） ----------------

    /** float32 小端序列化 */
    public static byte[] serialize(float[] vector) {
        ByteBuffer buf = ByteBuffer.allocate(vector.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float v : vector) {
            buf.putFloat(v);
        }
        return buf.array();
    }

    /** 反序列化：4 字节不对齐返回 null（视为不可用） */
    public static float[] deserialize(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length % 4 != 0) {
            return null;
        }
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        float[] out = new float[bytes.length / 4];
        for (int i = 0; i < out.length; i++) {
            out[i] = buf.getFloat();
        }
        return out;
    }

    /** 余弦相似度；零向量与维度不一致返回 0（防护） */
    public static double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) {
            return 0d;
        }
        double dot = 0d;
        double normA = 0d;
        double normB = 0d;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0d || normB == 0d) {
            return 0d;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /** 向量行是否可用于当前知识：ready + 版本一致 + 当前模型一致 */
    public static boolean isUsable(KbEmbedding row, KbItem kb, String currentModel) {
        return row != null
                && STATUS_READY.equals(row.getStatus())
                && row.getVector() != null && row.getVector().length > 0
                && row.getKbVersion() != null && row.getKbVersion().equals(kb.getVersion())
                && currentModel != null && !currentModel.isEmpty()
                && currentModel.equals(row.getVectorModel());
    }

    // ---------------- 厂商配置 ----------------

    /** 是否有可用的 embedding 厂商配置 */
    public boolean isEmbeddingEnabled() {
        return findEmbeddingProvider() != null;
    }

    public SysLlmConfig findEmbeddingProvider() {
        for (SysLlmConfig cfg : llmRepo.findByEnabledTrueAndPurpose(PURPOSE_EMBEDDING)) {
            if (cfg.getBaseUrl() != null && !cfg.getBaseUrl().trim().isEmpty()
                    && cfg.getApiKey() != null && !cfg.getApiKey().trim().isEmpty()
                    && cfg.getModelName() != null && !cfg.getModelName().trim().isEmpty()) {
                return cfg;
            }
        }
        return null;
    }

    // ---------------- 向量生成 ----------------

    /** 调用 /embeddings 生成单条文本向量；失败抛异常由调用方决定降级 */
    @SuppressWarnings("unchecked")
    public float[] embed(SysLlmConfig cfg, String text) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMs = (cfg.getTimeoutSeconds() != null ? cfg.getTimeoutSeconds() : 60) * 1000;
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(timeoutMs);
        RestTemplate rest = new RestTemplate(factory);

        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("model", cfg.getModelName());
        body.put("input", text);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 存储值为 AES-GCM 密文（enc: 前缀），调用前解密；存量明文原样透传
        headers.setBearerAuth(cryptoService.decrypt(cfg.getApiKey()));

        String url = cfg.getBaseUrl().replaceAll("/+$", "") + "/embeddings";
        ResponseEntity<String> resp = rest.exchange(url, HttpMethod.POST,
                new HttpEntity<Map<String, Object>>(body, headers), String.class);
        Map<String, Object> respMap = JsonUtil.readMap(resp.getBody());
        Object rawData = respMap.get("data");
        if (!(rawData instanceof List) || ((List<?>) rawData).isEmpty()) {
            throw new IllegalStateException("嵌入响应缺少 data");
        }
        Map<String, Object> first = null;
        for (Object o : (List<Object>) rawData) {
            if (!(o instanceof Map)) {
                continue;
            }
            Map<String, Object> item = (Map<String, Object>) o;
            if (first == null) {
                first = item;
            }
            Object idx = item.get("index");
            if (idx instanceof Number && ((Number) idx).intValue() == 0) {
                first = item;
                break;
            }
        }
        Object rawVec = first == null ? null : first.get("embedding");
        if (!(rawVec instanceof List) || ((List<?>) rawVec).isEmpty()) {
            throw new IllegalStateException("嵌入响应缺少 embedding");
        }
        List<Object> vec = (List<Object>) rawVec;
        float[] out = new float[vec.size()];
        for (int i = 0; i < vec.size(); i++) {
            Object v = vec.get(i);
            if (!(v instanceof Number)) {
                throw new IllegalStateException("嵌入向量元素非法");
            }
            out[i] = ((Number) v).floatValue();
        }
        return out;
    }

    /** 文字化知识条目（标题 + 摘要 + 正文截断），用于生成嵌入 */
    private static String embedText(KbItem kb) {
        StringBuilder sb = new StringBuilder();
        sb.append(kb.getTitle() == null ? "" : kb.getTitle()).append('\n');
        if (kb.getSummary() != null && !kb.getSummary().isEmpty()) {
            sb.append(kb.getSummary()).append('\n');
        }
        String body = kb.getBody() == null ? "" : kb.getBody();
        sb.append(body.length() > EMBED_TEXT_LIMIT ? body.substring(0, EMBED_TEXT_LIMIT) : body);
        return sb.toString();
    }

    /**
     * 同步嵌入单条知识：成功覆盖式保存（ready）；失败保留旧向量仅置 pending（可查、可重嵌）。
     * 返回是否成功。
     */
    public boolean embedKnowledge(Long kbId) {
        SysLlmConfig cfg = findEmbeddingProvider();
        KbItem kb = kbRepo.findById(kbId).orElse(null);
        if (cfg == null || kb == null) {
            return false;
        }
        KbEmbedding row = embeddingRepo.findByKbId(kbId).orElse(null);
        if (row == null) {
            row = new KbEmbedding();
            row.setKbId(kbId);
            row.setVector(new byte[0]);
            row.setDim(0);
            row.setStatus(STATUS_PENDING);
        }
        try {
            float[] vector = embed(cfg, embedText(kb));
            row.setKbVersion(kb.getVersion());
            row.setVector(serialize(vector));
            row.setVectorModel(cfg.getModelName());
            row.setDim(vector.length);
            row.setStatus(STATUS_READY);
            embeddingRepo.save(row);
            return true;
        } catch (Exception e) {
            log.warn("知识嵌入失败 kbId={}：{}", kbId, e.getMessage());
            try {
                if (row.getKbVersion() == null) {
                    row.setKbVersion(kb.getVersion());
                }
                row.setStatus(STATUS_PENDING);
                embeddingRepo.save(row);
            } catch (Exception ignore) {
                // 状态回写失败不阻断业务
            }
            return false;
        }
    }

    /** 审核通过 / 编辑升版触发：fire-and-forget，失败置 pending 不阻断业务 */
    @Async
    public void asyncEmbed(Long kbId) {
        try {
            if (findEmbeddingProvider() == null) {
                return;
            }
            embedKnowledge(kbId);
        } catch (Exception e) {
            log.warn("异步嵌入异常 kbId={}：{}", kbId, e.getMessage());
        }
    }

    /**
     * 检索时懒补偿：候选集内失效向量（无行 / pending / 版本过期 / 模型不符）批量补嵌，
     * 单次上限 16 条；返回实际补嵌成功条数（失败仅缺席本次语义，不报错）。
     */
    public int lazyCompensate(List<KbItem> candidates) {
        SysLlmConfig cfg = findEmbeddingProvider();
        if (cfg == null || candidates == null || candidates.isEmpty()) {
            return 0;
        }
        List<Long> ids = new ArrayList<Long>();
        for (KbItem kb : candidates) {
            ids.add(kb.getId());
        }
        Map<Long, KbEmbedding> rows = new LinkedHashMap<Long, KbEmbedding>();
        for (KbEmbedding e : embeddingRepo.findByKbIdIn(ids)) {
            rows.put(e.getKbId(), e);
        }
        int count = 0;
        for (KbItem kb : candidates) {
            if (count >= LAZY_LIMIT) {
                break;
            }
            if (isUsable(rows.get(kb.getId()), kb, cfg.getModelName())) {
                continue;
            }
            if (embedKnowledge(kb.getId())) {
                count++;
            }
        }
        return count;
    }

    /** 向量状态监控（知识库页状态条）：total/ready/pending/staleVersion/currentModel/staleModels */
    public Map<String, Object> status() {
        SysLlmConfig cfg = findEmbeddingProvider();
        String currentModel = cfg == null || cfg.getModelName() == null ? "" : cfg.getModelName();
        List<KbItem> published = kbRepo.findByStatus("已发布");
        Map<Long, KbEmbedding> rows = new LinkedHashMap<Long, KbEmbedding>();
        if (!published.isEmpty()) {
            List<Long> ids = new ArrayList<Long>();
            for (KbItem kb : published) {
                ids.add(kb.getId());
            }
            for (KbEmbedding e : embeddingRepo.findByKbIdIn(ids)) {
                rows.put(e.getKbId(), e);
            }
        }
        long ready = 0;
        long pending = 0;
        long staleVersion = 0;
        for (KbItem kb : published) {
            KbEmbedding e = rows.get(kb.getId());
            boolean versionOk = e != null && e.getKbVersion() != null
                    && e.getKbVersion().equals(kb.getVersion());
            if (e != null && !versionOk) {
                staleVersion++;
            }
            if (isUsable(e, kb, currentModel)) {
                ready++;
            } else {
                pending++;
            }
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("enabled", cfg != null);
        result.put("total", published.size());
        result.put("ready", ready);
        result.put("pending", pending);
        result.put("staleVersion", staleVersion);
        result.put("currentModel", currentModel);
        result.put("staleModels", currentModel.isEmpty()
                ? new ArrayList<String>() : embeddingRepo.findStaleModels(currentModel));
        return result;
    }

    /** 手动全量重嵌（换嵌入模型后）；无可用配置 → 业务异常提示先配置 */
    public Map<String, Object> reembedAll() {
        if (findEmbeddingProvider() == null) {
            throw new BizException("未配置启用中的 embedding 用途厂商：请到「配置中心 → 大模型」新增模型配置，用途选 embedding");
        }
        List<KbItem> published = kbRepo.findByStatus("已发布");
        int success = 0;
        int failed = 0;
        for (KbItem kb : published) {
            if (embedKnowledge(kb.getId())) {
                success++;
            } else {
                failed++;
            }
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("total", published.size());
        result.put("success", success);
        result.put("failed", failed);
        return result;
    }

    /** 配置中心「测试连接」（embedding 用途）：真实调用 /embeddings 验证维度与连通性 */
    public Map<String, Object> testEmbedding(SysLlmConfig cfg) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        if (cfg.getBaseUrl() == null || cfg.getBaseUrl().trim().isEmpty()) {
            result.put("ok", false);
            result.put("message", "请先填写 Base URL");
            return result;
        }
        if (cfg.getApiKey() == null || cfg.getApiKey().trim().isEmpty()) {
            result.put("ok", false);
            result.put("message", "请先填写 API Key");
            return result;
        }
        if (cfg.getModelName() == null || cfg.getModelName().trim().isEmpty()) {
            result.put("ok", false);
            result.put("message", "请先填写模型名（如 text-embedding-3-small）");
            return result;
        }
        try {
            long start = System.currentTimeMillis();
            float[] vector = embed(cfg, "连通性测试：TestPilot 知识库语义检索");
            long cost = System.currentTimeMillis() - start;
            result.put("ok", true);
            result.put("message", "连接成功，向量维度 " + vector.length + "（" + cost + " ms）");
        } catch (Exception e) {
            result.put("ok", false);
            result.put("message", "连接失败：" + e.getMessage());
        }
        return result;
    }
}
