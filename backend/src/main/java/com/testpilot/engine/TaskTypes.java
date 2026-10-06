package com.testpilot.engine;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务类型常量与默认配置（工作台未发布配置时的兜底）。
 */
public final class TaskTypes {

    public static final String TESTCASE_GEN = "testcase_gen";
    public static final String BUG_ANALYSIS = "bug_analysis";
    public static final String LOG_TRIAGE = "log_triage";
    public static final String SQL_ANALYSIS = "sql_analysis";
    public static final String REGRESSION_LIST = "regression_list";
    public static final String TEST_REPORT = "test_report";
    public static final String PROMPT_TEST = "prompt_test";

    /** 主任务优先顺序（同分时靠前者为主任务） */
    public static final List<String> ORDER = Collections.unmodifiableList(Arrays.asList(
            BUG_ANALYSIS, LOG_TRIAGE, SQL_ANALYSIS, REGRESSION_LIST,
            TESTCASE_GEN, TEST_REPORT, PROMPT_TEST));

    private static final Map<String, String> NAMES;
    private static final Map<String, List<String>> TRIGGERS;
    private static final Map<String, List<String>> REQUIRED_FIELDS;
    private static final Map<String, List<String>> WORKFLOW_STEPS;
    private static final Map<String, List<String>> KB_MAPPINGS;

    /** 知识库全部分类（分类覆盖统计用） */
    public static final List<String> ALL_CATEGORIES = Collections.unmodifiableList(Arrays.asList(
            "业务规则库", "接口异常库", "历史 Bug 库", "日志规律库", "SQL 经验库", "回归规则库", "测试数据库"));

    /** 全局安全规则（五条，输出中必须体现） */
    public static final List<String> SECURITY_RULES = Collections.unmodifiableList(Arrays.asList(
            "不跳步骤",
            "证据不足时明确标记",
            "不编造知识标题或 Bug 编号",
            "AI 不批准上线",
            "P0/P1 与 Prompt 通过需人工复核"));

    public static final List<String> HUMAN_REVIEW_POINTS = Collections.unmodifiableList(Arrays.asList(
            "确认事实与引用依据",
            "P0/P1 风险和上线结论必须由测试人最终确认"));

    static {
        NAMES = new LinkedHashMap<String, String>();
        NAMES.put(TESTCASE_GEN, "测试用例生成");
        NAMES.put(BUG_ANALYSIS, "Bug 分析");
        NAMES.put(LOG_TRIAGE, "日志排查");
        NAMES.put(SQL_ANALYSIS, "SQL 分析");
        NAMES.put(REGRESSION_LIST, "回归清单");
        NAMES.put(TEST_REPORT, "测试报告");
        NAMES.put(PROMPT_TEST, "Prompt 测试");

        TRIGGERS = new LinkedHashMap<String, List<String>>();
        TRIGGERS.put(TESTCASE_GEN, list("需求", "验收", "用例", "场景", "测试点"));
        TRIGGERS.put(BUG_ANALYSIS, list("bug", "缺陷", "现象", "复现", "实际结果", "预期"));
        TRIGGERS.put(LOG_TRIAGE, list("日志", "traceId", "超时", "timeout", "异常堆栈"));
        TRIGGERS.put(SQL_ANALYSIS, list("sql", "慢查询", "索引", "一致性", "执行计划", "排查"));
        TRIGGERS.put(REGRESSION_LIST, list("回归", "回归范围", "上线前", "发布"));
        TRIGGERS.put(TEST_REPORT, list("测试报告", "质量结论", "上线结论"));
        TRIGGERS.put(PROMPT_TEST, list("prompt", "提示词", "模板"));

        REQUIRED_FIELDS = new LinkedHashMap<String, List<String>>();
        REQUIRED_FIELDS.put(TESTCASE_GEN, list("需求背景", "验收标准"));
        REQUIRED_FIELDS.put(BUG_ANALYSIS, list("问题现象", "复现步骤", "预期结果"));
        REQUIRED_FIELDS.put(LOG_TRIAGE, list("时间范围", "日志上下文", "异常关键词"));
        REQUIRED_FIELDS.put(SQL_ANALYSIS, list("SQL 文本", "分析目标"));
        REQUIRED_FIELDS.put(REGRESSION_LIST, list("版本信息", "回归范围"));
        REQUIRED_FIELDS.put(TEST_REPORT, list("版本信息", "结论来源"));
        REQUIRED_FIELDS.put(PROMPT_TEST, list("Prompt 文本", "评估标准"));

        WORKFLOW_STEPS = new LinkedHashMap<String, List<String>>();
        WORKFLOW_STEPS.put(TESTCASE_GEN, list("校验输入", "拆解测试点", "覆盖正常/异常/边界", "生成结构化用例", "标记人工复核"));
        WORKFLOW_STEPS.put(BUG_ANALYSIS, list("校验输入", "还原现象", "匹配历史Bug", "定位排查路径", "输出回归范围", "标记人工复核"));
        WORKFLOW_STEPS.put(LOG_TRIAGE, list("校验输入", "提取时间线", "定位异常片段", "扩展调用链", "输出排查建议", "标记人工复核"));
        WORKFLOW_STEPS.put(SQL_ANALYSIS, list("校验输入", "语法与正确性检查", "性能风险识别", "影响范围评估", "输出优化建议", "标记人工复核"));
        WORKFLOW_STEPS.put(REGRESSION_LIST, list("校验输入", "收集高风险用例", "收集历史Bug", "合并去重排序", "生成清单", "标记人工复核"));
        WORKFLOW_STEPS.put(TEST_REPORT, list("校验输入", "汇总用例与回归", "聚合分析结论", "生成风险与遗留", "输出统一结论", "标记人工复核"));
        WORKFLOW_STEPS.put(PROMPT_TEST, list("校验输入", "执行Prompt", "对比预期", "输出评估", "标记人工复核"));

        KB_MAPPINGS = new LinkedHashMap<String, List<String>>();
        KB_MAPPINGS.put(TESTCASE_GEN, list("业务规则库", "接口异常库", "历史 Bug 库"));
        KB_MAPPINGS.put(BUG_ANALYSIS, list("历史 Bug 库", "日志规律库", "SQL 经验库", "业务规则库"));
        KB_MAPPINGS.put(LOG_TRIAGE, list("日志规律库", "历史 Bug 库"));
        KB_MAPPINGS.put(SQL_ANALYSIS, list("SQL 经验库", "业务规则库"));
        KB_MAPPINGS.put(REGRESSION_LIST, list("回归规则库", "历史 Bug 库"));
        KB_MAPPINGS.put(TEST_REPORT, list("回归规则库", "业务规则库"));
        KB_MAPPINGS.put(PROMPT_TEST, list("业务规则库"));
    }

    private TaskTypes() {
    }

    private static List<String> list(String... items) {
        return Collections.unmodifiableList(Arrays.asList(items));
    }

    public static String nameOf(String taskType) {
        String name = NAMES.get(taskType);
        return name != null ? name : taskType;
    }

    public static boolean isValid(String taskType) {
        return NAMES.containsKey(taskType);
    }

    public static List<String> defaultTriggers(String taskType) {
        List<String> v = TRIGGERS.get(taskType);
        return v != null ? v : Collections.<String>emptyList();
    }

    public static List<String> defaultRequiredFields(String taskType) {
        List<String> v = REQUIRED_FIELDS.get(taskType);
        return v != null ? v : Collections.<String>emptyList();
    }

    public static List<String> defaultWorkflowSteps(String taskType) {
        List<String> v = WORKFLOW_STEPS.get(taskType);
        return v != null ? v : Collections.<String>emptyList();
    }

    public static List<String> defaultKbMappings(String taskType) {
        List<String> v = KB_MAPPINGS.get(taskType);
        return v != null ? v : Collections.<String>emptyList();
    }
}
