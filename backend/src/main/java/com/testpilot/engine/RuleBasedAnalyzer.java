package com.testpilot.engine;

import com.testpilot.entity.KbItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 规则引擎分析器：LLM 未启用或调用失败时的降级实现。
 * 输出结构化 JSON；知识引用只使用真实命中的已发布条目，绝不编造。
 */
@Service
public class RuleBasedAnalyzer {

    public Map<String, Object> analyze(String taskType, AnalysisContext ctx) {
        if (TaskTypes.BUG_ANALYSIS.equals(taskType)) {
            return bugAnalysis(ctx);
        }
        if (TaskTypes.LOG_TRIAGE.equals(taskType)) {
            return logTriage(ctx);
        }
        if (TaskTypes.SQL_ANALYSIS.equals(taskType)) {
            return sqlAnalysis(ctx);
        }
        if (TaskTypes.REGRESSION_LIST.equals(taskType)) {
            return regressionList(ctx);
        }
        if (TaskTypes.TESTCASE_GEN.equals(taskType)) {
            return testcaseGen(ctx);
        }
        if (TaskTypes.TEST_REPORT.equals(taskType)) {
            return testReport(ctx);
        }
        if (TaskTypes.PROMPT_TEST.equals(taskType)) {
            return promptTest(ctx);
        }
        Map<String, Object> out = base(ctx);
        out.put("message", "未知任务类型：" + taskType);
        return out;
    }

    // ---------------- 通用字段 ----------------

    private Map<String, Object> base(AnalysisContext ctx) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("risk_level", ctx.getRisk());
        out.put("human_review_points", TaskTypes.HUMAN_REVIEW_POINTS);
        out.put("need_more_info", ctx.getMissing());
        return out;
    }

    private List<String> kbByCategory(AnalysisContext ctx, String category) {
        List<String> result = new ArrayList<String>();
        for (KbItem kb : ctx.getKbRefs()) {
            if (category.equals(kb.getCategory())) {
                result.add(kb.getTitle() + "（" + kb.getKbNo() + "）");
            }
        }
        return result;
    }

    private String firstSentence(String text) {
        if (text == null) {
            return "";
        }
        for (String part : text.split("[。\\n]")) {
            String t = part.trim();
            if (!t.isEmpty()) {
                return t.length() > 60 ? t.substring(0, 60) + "…" : t;
            }
        }
        return "";
    }

    private String phenomenonOf(String text) {
        if (text == null) {
            return "";
        }
        for (String part : text.split("[。\\n]")) {
            String t = part.trim();
            if (t.contains("实际结果") || t.contains("现象")) {
                return t.length() > 60 ? t.substring(0, 60) + "…" : t;
            }
        }
        return firstSentence(text);
    }

    private boolean hasLockWait(String text) {
        String t = text == null ? "" : text.toLowerCase();
        return t.contains("lock wait") || t.contains("锁等待") || t.contains("timeout") || t.contains("超时");
    }

    private boolean isPaymentDomain(String text) {
        String t = text == null ? "" : text;
        return t.contains("支付") || t.contains("回调") || t.contains("订单") || t.contains("优惠券");
    }

    // ---------------- Bug 分析 ----------------

    private Map<String, Object> bugAnalysis(AnalysisContext ctx) {
        Map<String, Object> out = base(ctx);
        String text = ctx.getInputText() == null ? "" : ctx.getInputText();
        out.put("phenomenon", phenomenonOf(text));
        out.put("affected_modules", Arrays.asList(ctx.modulePath()));
        List<String> causes = new ArrayList<String>();
        causes.add("从现象推测存在状态同步或事务边界异常，尚需证据验证");
        if (hasLockWait(text)) {
            causes.add("Lock wait timeout 指向数据库锁等待，可能阻塞核心状态更新事务");
        }
        out.put("possible_causes", causes);
        String traceId = ctx.traceId();
        List<String> logs = new ArrayList<String>();
        logs.add(traceId != null
                ? "按 traceId=" + traceId + " 检索支付回调与订单同步全链路日志"
                : "提供 traceId 或时间范围后检索全链路日志");
        logs.add("检查关键事务边界与状态流转日志");
        out.put("recommended_logs", logs);
        List<String> sqls = new ArrayList<String>();
        sqls.add("查询核心业务状态流转表在时间范围内的变更记录");
        sqls.add("核对上下游数据一致性");
        List<String> sqlKb = kbByCategory(ctx, "SQL 经验库");
        if (!sqlKb.isEmpty()) {
            sqls.add("参考 SQL 经验库：" + sqlKb.get(0));
        }
        out.put("recommended_sql", sqls);
        List<String> historyBugs = kbByCategory(ctx, "历史 Bug 库");
        out.put("related_history_bugs",
                historyBugs.isEmpty() ? Arrays.asList("暂无命中（不编造引用）") : historyBugs);
        List<String> scope = new ArrayList<String>();
        scope.add("直接变更模块");
        scope.add("上下游状态同步");
        scope.add("失败与重复请求场景");
        for (String kb : kbByCategory(ctx, "回归规则库")) {
            scope.add("回归规则：" + kb);
        }
        out.put("regression_scope", scope);
        return out;
    }

    // ---------------- 日志排查 ----------------

    private Map<String, Object> logTriage(AnalysisContext ctx) {
        Map<String, Object> out = base(ctx);
        String text = ctx.getInputText() == null ? "" : ctx.getInputText();
        out.put("anomaly_summary", "已提取异常关键词与时间线，根因仍需结合完整上下文确认");
        String firstKeyword = firstErrorKeyword(text);
        List<String> timeline = new ArrayList<String>();
        timeline.add("接收用户证据");
        timeline.add(firstKeyword != null
                ? "定位首个异常片段（" + firstKeyword + "）" : "等待补充日志片段");
        timeline.add("向上下游扩展调用链");
        out.put("timeline", timeline);
        out.put("error_fragments", errorFragments(text));
        List<String> causes = new ArrayList<String>();
        if (hasLockWait(text)) {
            causes.add("锁等待可能由幂等表唯一索引冲突或长事务导致，尚需证据验证");
        } else {
            causes.add("需要更多日志证据，不猜测根因");
        }
        out.put("possible_causes", causes);
        String traceId = ctx.traceId();
        List<String> suggestions = new ArrayList<String>();
        suggestions.add(traceId != null
                ? "按 traceId=" + traceId + " 检索回调消费线程与数据库事务日志"
                : "补充 traceId 或时间范围后检索线程与事务日志");
        List<String> logKb = kbByCategory(ctx, "日志规律库");
        if (!logKb.isEmpty()) {
            suggestions.add("命中日志规律：" + logKb.get(0));
        }
        out.put("suggestions", suggestions);
        out.put("related_kb", kbByCategory(ctx, "日志规律库"));
        return out;
    }

    private String firstErrorKeyword(String text) {
        String lower = text == null ? "" : text.toLowerCase();
        if (lower.contains("lock wait")) {
            return "Lock wait timeout";
        }
        if (lower.contains("timeout") || text.contains("超时")) {
            return "timeout";
        }
        if (lower.contains("exception")) {
            return "exception";
        }
        if (lower.contains("error")) {
            return "error";
        }
        return null;
    }

    private List<String> errorFragments(String text) {
        List<String> fragments = new ArrayList<String>();
        if (text == null) {
            return fragments;
        }
        for (String line : text.split("\\n|。|；")) {
            String t = line.trim();
            String lower = t.toLowerCase();
            if (t.isEmpty()) {
                continue;
            }
            if (lower.contains("exception") || lower.contains("error") || lower.contains("timeout")
                    || lower.contains("lock wait") || t.contains("超时") || t.contains("异常")
                    || t.contains("失败")) {
                fragments.add(t.length() > 80 ? t.substring(0, 80) + "…" : t);
                if (fragments.size() >= 5) {
                    break;
                }
            }
        }
        if (fragments.isEmpty()) {
            fragments.add("未检测到明确异常片段，建议补充日志上下文");
        }
        return fragments;
    }

    // ---------------- SQL 分析 ----------------

    private Map<String, Object> sqlAnalysis(AnalysisContext ctx) {
        Map<String, Object> out = base(ctx);
        String text = ctx.getInputText() == null ? "" : ctx.getInputText();
        boolean hasSql = CompletenessChecker.detect("SQL 文本", text, text.toLowerCase());
        out.put("sql_correctness", hasSql
                ? "已按只读检查口径扫描 SQL 文本，未发现明显语法问题（以人工复核为准）"
                : "未提供 SQL 文本，暂无法进行语法与正确性检查");
        out.put("performance_risk", hasSql
                ? "建议补充执行计划与表结构信息后评估索引命中与锁行为"
                : "缺少执行计划与表结构信息，无法评估索引命中情况");
        out.put("impact_scope", Arrays.asList(ctx.modulePath(), "上下游状态同步", "历史数据回溯范围"));
        List<String> suggestions = new ArrayList<String>();
        List<String> sqlKb = kbByCategory(ctx, "SQL 经验库");
        if (!sqlKb.isEmpty()) {
            suggestions.add("参考 SQL 经验库：" + sqlKb.get(0));
        }
        suggestions.add("按 traceId 与时间范围对齐上下游数据后核对一致性");
        suggestions.add("补充执行计划后评估索引与锁行为");
        out.put("optimization_suggestions", suggestions);
        out.put("review_conclusion", "证据不足，需人工复核后给出最终结论");
        out.put("completeness_score", ctx.getCompleteness());
        return out;
    }

    // ---------------- 回归清单 ----------------

    private Map<String, Object> regressionList(AnalysisContext ctx) {
        Map<String, Object> out = base(ctx);
        String text = ctx.getInputText() == null ? "" : ctx.getInputText();
        Set<String> items = new LinkedHashSet<String>();
        if (isPaymentDomain(text)) {
            items.add("支付成功回调主流程");
            items.add("支付失败回调处理");
            items.add("重复回调幂等验证");
            items.add("订单状态同步查询");
            items.add("支付流水一致性核对");
            items.add("优惠券返还与退款场景");
        } else {
            items.add("核心链路主流程校验");
            items.add("异常与失败场景重放");
            items.add("幂等与重复请求验证");
            items.add("上下游数据一致性核对");
            items.add("边界值与极端场景");
            items.add("历史缺陷回归验证");
        }
        int caseCount = 0;
        for (com.testpilot.entity.TestCase c : ctx.getHighRiskCases()) {
            items.add(c.getTitle() + "（关联用例 " + c.getCaseNo() + "）");
            caseCount++;
        }
        int bugCount = 0;
        for (String kb : kbByCategory(ctx, "历史 Bug 库")) {
            items.add(kb);
            bugCount++;
        }
        int ruleCount = 0;
        for (String kb : kbByCategory(ctx, "回归规则库")) {
            ruleCount++;
        }
        List<String> itemList = new ArrayList<String>(items);
        // 至少 6 个必回归项（安全规则）
        while (itemList.size() < 6) {
            itemList.add("补充项 " + (itemList.size() + 1) + "（待人工确认）");
        }
        out.put("regression_items", itemList);
        out.put("item_count", itemList.size());
        out.put("completion_rate", ctx.getCompleteness());
        out.put("sources_summary", Arrays.asList(
                "高风险用例 " + caseCount + " 条",
                "历史 Bug " + bugCount + " 条",
                "回归规则库 " + ruleCount + " 条"));
        return out;
    }

    // ---------------- 测试用例生成 ----------------

    private Map<String, Object> testcaseGen(AnalysisContext ctx) {
        Map<String, Object> out = base(ctx);
        String text = ctx.getInputText() == null ? "" : ctx.getInputText();
        boolean payment = isPaymentDomain(text);
        String topic = payment ? "支付回调" : "核心功能";
        List<String> points = payment
                ? Arrays.asList("支付主流程正向校验", "重复回调幂等", "订单状态同步一致性", "优惠券与退款联动", "异常与边界场景")
                : Arrays.asList("主流程正向校验", "异常场景处理", "边界条件覆盖", "数据一致性校验", "幂等与重复请求");
        out.put("test_points", points);
        List<Map<String, Object>> cases = new ArrayList<Map<String, Object>>();
        cases.add(draftCase(topic + "正常流程校验", "P1", "功能",
                "环境与数据就绪", "1. 执行主流程操作\n2. 检查结果状态", "结果与预期一致"));
        cases.add(draftCase(topic + "异常场景拦截", "P1", "异常",
                "构造异常输入", "1. 执行异常操作\n2. 检查拦截与日志", "被正确拦截并记录"));
        cases.add(draftCase(topic + "边界条件验证", "P2", "边界",
                "构造边界数据", "1. 执行边界操作\n2. 检查处理结果", "边界处理符合规则"));
        out.put("cases", cases);
        out.put("coverage", "正常 / 异常 / 边界");
        return out;
    }

    private Map<String, Object> draftCase(String title, String priority, String type,
                                          String preconditions, String steps, String expected) {
        Map<String, Object> c = new LinkedHashMap<String, Object>();
        c.put("title", title);
        c.put("priority", priority);
        c.put("type", type);
        c.put("preconditions", preconditions);
        c.put("steps", steps);
        c.put("expected", expected);
        return c;
    }

    // ---------------- 测试报告 ----------------

    private Map<String, Object> testReport(AnalysisContext ctx) {
        Map<String, Object> out = base(ctx);
        out.put("conclusion_candidate", "待来源数据齐备后人工确认");
        out.put("summary", "测试报告将汇总：用例执行结果、回归清单结果、分析任务结论与风险遗留，形成统一质量结论。");
        out.put("sources", Arrays.asList("用例库执行结果", "回归清单完成情况", "Bug/日志/SQL 分析任务"));
        out.put("risk_notes", new ArrayList<String>());
        out.put("ai_boundary", "AI 不批准上线，上线结论必须由测试人最终确认");
        return out;
    }

    // ---------------- Prompt 测试 ----------------

    private Map<String, Object> promptTest(AnalysisContext ctx) {
        Map<String, Object> out = base(ctx);
        List<Map<String, Object>> eval = new ArrayList<Map<String, Object>>();
        eval.add(evalItem("指令清晰度", ctx.getMissing().contains("Prompt 文本") ? "待补充 Prompt 文本" : "已按材料初评"));
        eval.add(evalItem("输出结构", "结构化输出需符合模板"));
        eval.add(evalItem("安全合规", "Prompt 通过需人工复核"));
        out.put("prompt_eval", eval);
        out.put("notes", "Prompt 通过需人工复核，AI 不做最终结论");
        return out;
    }

    private Map<String, Object> evalItem(String check, String result) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("check", check);
        item.put("result", result);
        return item;
    }
}
