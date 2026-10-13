package com.testpilot.engine;

import com.testpilot.common.CryptoService;
import com.testpilot.common.JsonUtil;
import com.testpilot.entity.KbItem;
import com.testpilot.entity.SysLlmConfig;
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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM 客户端：调用 OpenAI 兼容 /chat/completions 接口。
 * 未启用或调用失败时返回 null，由规则引擎降级，保证无 API Key 也能完整体验。
 */
@Service
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    @Autowired
    private SysLlmConfigRepository llmRepo;
    @Autowired
    private CryptoService cryptoService;

    /** 是否有可用的已启用厂商 */
    public boolean isEnabled() {
        return findEnabled() != null;
    }

    public SysLlmConfig findEnabled() {
        for (SysLlmConfig cfg : llmRepo.findByEnabledTrue()) {
            if (cfg.getBaseUrl() != null && !cfg.getBaseUrl().trim().isEmpty()
                    && cfg.getApiKey() != null && !cfg.getApiKey().trim().isEmpty()) {
                return cfg;
            }
        }
        return null;
    }

    /**
     * 结构化分析：成功返回 Map（引擎模式 llm），失败返回 null（调用方降级）。
     */
    public Map<String, Object> analyze(SysLlmConfig cfg, String taskType, AnalysisContext ctx,
                                       List<String> requiredFields) {
        try {
            String content = chat(cfg, buildSystemPrompt(taskType), buildUserPrompt(taskType, ctx));
            Map<String, Object> out = extractJson(content);
            if (out == null) {
                return null;
            }
            // 安全校验：必须包含关键字段，缺则补默认
            if (!out.containsKey("risk_level")) {
                out.put("risk_level", ctx.getRisk());
            }
            if (!out.containsKey("human_review_points")) {
                out.put("human_review_points", TaskTypes.HUMAN_REVIEW_POINTS);
            }
            if (!out.containsKey("need_more_info")) {
                out.put("need_more_info", ctx.getMissing());
            }
            return out;
        } catch (Exception e) {
            log.warn("LLM 调用失败，降级规则引擎：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 回归清单生成（回归页「分析并生成」直连）：基于变更内容与影响模块输出回归项。
     * 成功返回非空 item 列表（title/priority/reason），失败返回 null（调用方降级规则聚合）。
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> generateRegressionItems(SysLlmConfig cfg, String title,
                                                             String changeContent, AnalysisContext ctx) {
        try {
            String content = chat(cfg, regressionSystemPrompt(),
                    regressionUserPrompt(title, changeContent, ctx));
            Map<String, Object> out = extractJson(content);
            if (out == null) {
                return null;
            }
            Object raw = out.get("regression_items");
            if (!(raw instanceof List)) {
                return null;
            }
            List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
            for (Object o : (List<Object>) raw) {
                if (!(o instanceof Map)) {
                    continue;
                }
                Map<String, Object> m = (Map<String, Object>) o;
                String itemTitle = m.get("title") == null ? "" : m.get("title").toString().trim();
                if (itemTitle.isEmpty() || itemTitle.length() > 200) {
                    continue;
                }
                String priority = m.get("priority") == null
                        ? "P1" : m.get("priority").toString().trim().toUpperCase();
                if (!Arrays.asList("P0", "P1", "P2").contains(priority)) {
                    priority = "P1";
                }
                Map<String, Object> item = new LinkedHashMap<String, Object>();
                item.put("title", itemTitle);
                item.put("priority", priority);
                item.put("reason", m.get("reason") == null ? "" : m.get("reason").toString().trim());
                items.add(item);
                if (items.size() >= 30) {
                    break;
                }
            }
            return items.isEmpty() ? null : items;
        } catch (Exception e) {
            log.warn("回归清单 LLM 生成失败，降级规则聚合：{}", e.getMessage());
            return null;
        }
    }

    /** 配置中心「测试连接」 */
    public Map<String, Object> testConnection(SysLlmConfig cfg) {
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
        try {
            long start = System.currentTimeMillis();
            String reply = chat(cfg, "你是连通性测试助手。", "请回复：ok");
            long cost = System.currentTimeMillis() - start;
            result.put("ok", true);
            result.put("message", "连接成功，模型响应正常（" + cost + " ms）："
                    + (reply != null && reply.length() > 50 ? reply.substring(0, 50) : reply));
        } catch (Exception e) {
            result.put("ok", false);
            result.put("message", "连接失败：" + e.getMessage());
        }
        return result;
    }

    // ---------------- 内部实现 ----------------

    private String chat(SysLlmConfig cfg, String systemPrompt, String userPrompt) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMs = (cfg.getTimeoutSeconds() != null ? cfg.getTimeoutSeconds() : 60) * 1000;
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(timeoutMs);
        RestTemplate rest = new RestTemplate(factory);

        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("model", cfg.getModelName());
        List<Map<String, String>> messages = new ArrayList<Map<String, String>>();
        messages.add(message("system", systemPrompt));
        messages.add(message("user", userPrompt));
        body.put("messages", messages);
        body.put("temperature", cfg.getTemperature() != null ? cfg.getTemperature().doubleValue() : 0.2);
        body.put("max_tokens", cfg.getMaxOutput() != null ? cfg.getMaxOutput() : 4096);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 存储值为 AES-GCM 密文（enc: 前缀），调用前解密；存量明文原样透传
        headers.setBearerAuth(cryptoService.decrypt(cfg.getApiKey()));

        String url = cfg.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
        ResponseEntity<String> resp = rest.exchange(url, HttpMethod.POST,
                new HttpEntity<Map<String, Object>>(body, headers), String.class);
        Map<String, Object> respMap = JsonUtil.readMap(resp.getBody());
        List<Map<String, Object>> choices =
                (List<Map<String, Object>>) respMap.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("模型响应缺少 choices");
        }
        Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
        return msg != null ? (String) msg.get("content") : null;
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    private String regressionSystemPrompt() {
        return "你是资深测试工程师，正在 TestPilot 个人 QA 工作台为一次版本变更生成可执行的回归测试清单。"
                + "只输出一个 JSON 对象，不要输出任何其他文字。"
                + "必须遵守：1) 优先纳入与变更内容直接相关的 P0/P1 高风险用例、失败/阻塞用例与历史缺陷；"
                + "2) 结合变更影响模块补充必要的通用回归场景（主流程、异常重放、幂等重复请求、上下游一致性、边界值）；"
                + "3) 禁止编造用例编号或 Bug 编号，参考材料中给出的编号必须原样引用；"
                + "4) 证据不足时在 need_more_info 数组中明确标注缺项，不要猜测；"
                + "5) AI 不批准上线，清单需人工复核后执行。"
                + "输出 JSON 必须包含 risk_level（P0/P1/P2）、human_review_points（数组）、need_more_info（数组）、"
                + "regression_items（数组，每项含 title（不超过 60 字）、priority（P0/P1/P2）、reason（纳入理由））。";
    }

    private String regressionUserPrompt(String title, String changeContent, AnalysisContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("清单标题：").append(title).append("\n");
        sb.append("变更内容与影响模块：\n").append(changeContent).append("\n");
        if (!ctx.getHighRiskCases().isEmpty()) {
            sb.append("\n高风险用例参考（优先纳入与变更相关的项）：\n");
            for (com.testpilot.entity.TestCase c : ctx.getHighRiskCases()) {
                sb.append("- ").append(c.getTitle()).append("（").append(c.getCaseNo())
                        .append("，").append(c.getPriority()).append("）\n");
            }
        }
        boolean hasBug = false;
        boolean hasRule = false;
        for (KbItem kb : ctx.getKbRefs()) {
            if ("历史 Bug 库".equals(kb.getCategory()) && !hasBug) {
                hasBug = true;
                sb.append("\n历史 Bug 参考：\n");
            }
            if ("回归规则库".equals(kb.getCategory()) && !hasRule) {
                hasRule = true;
                sb.append("\n回归规则参考：\n");
            }
            if ("历史 Bug 库".equals(kb.getCategory()) || "回归规则库".equals(kb.getCategory())) {
                sb.append("- ").append(kb.getTitle()).append("（").append(kb.getKbNo()).append("）\n");
            }
        }
        sb.append("\n请输出结构化 JSON 回归清单。");
        return sb.toString();
    }

    private String buildSystemPrompt(String taskType) {
        return "你是资深测试工程师，正在 TestPilot 个人 QA 工作台执行「"
                + TaskTypes.nameOf(taskType) + "」任务。只输出一个 JSON 对象，不要输出任何其他文字。"
                + "必须遵守全局安全规则：1) 不跳步骤；2) 证据不足时在 need_more_info 数组中明确标注缺项，不要猜测根因；"
                + "3) 不编造知识标题或 Bug 编号，只能引用用户材料中给出的真实知识条目；"
                + "4) AI 不批准上线；5) P0/P1 与 Prompt 通过需人工复核。"
                + "输出 JSON 必须包含 risk_level（P0/P1/P2）、human_review_points（数组）、need_more_info（数组）字段，"
                + "并根据任务类型输出结构化分析字段。";
    }

    @SuppressWarnings("unchecked")
    private String buildUserPrompt(String taskType, AnalysisContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("任务类型：").append(taskType)
                .append("（").append(TaskTypes.nameOf(taskType)).append("）\n");
        sb.append("模块：").append(ctx.modulePath()).append("\n");
        sb.append("风险等级：").append(ctx.getRisk()).append("\n");
        sb.append("输入完整度：").append(ctx.getCompleteness()).append("%，缺项：")
                .append(ctx.getMissing()).append("\n");
        if (!ctx.getKbRefs().isEmpty()) {
            sb.append("\n已发布知识条目（可引用，不可编造）：\n");
            for (KbItem kb : ctx.getKbRefs()) {
                String body = kb.getBody() != null && kb.getBody().length() > 100
                        ? kb.getBody().substring(0, 100) + "…" : kb.getBody();
                sb.append("- ").append(kb.getTitle()).append("（").append(kb.getKbNo())
                        .append("）：").append(body).append("\n");
            }
        }
        sb.append("\n输入材料：\n").append(ctx.getInputText() == null ? "" : ctx.getInputText());
        sb.append("\n\n请输出结构化 JSON 分析结果。");
        return sb.toString();
    }

    /** 从模型回复中提取 JSON 对象（兼容 ```json 代码块） */
    static Map<String, Object> extractJson(String content) {
        if (content == null) {
            return null;
        }
        String text = content.trim();
        if (text.startsWith("```")) {
            int end = text.lastIndexOf("```");
            int start = text.indexOf("\n");
            if (start > 0 && end > start) {
                text = text.substring(start + 1, end).trim();
            }
        }
        int braceStart = text.indexOf('{');
        int braceEnd = text.lastIndexOf('}');
        if (braceStart < 0 || braceEnd <= braceStart) {
            return null;
        }
        Map<String, Object> map = JsonUtil.readMap(text.substring(braceStart, braceEnd + 1));
        return map.isEmpty() ? null : map;
    }
}
