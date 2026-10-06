package com.testpilot.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 输入完整度检查：按任务必填信息逐项检测，计算完整度百分比与缺项列表。
 * 证据不足时只提示补充，不猜测根因。
 */
public class CompletenessChecker {

    public static class Result {
        public int completeness;
        public List<String> missing = new ArrayList<String>();

        public Result(int completeness, List<String> missing) {
            this.completeness = completeness;
            this.missing = missing;
        }
    }

    private static final Pattern DATE_PATTERN =
            Pattern.compile("\\d{4}-\\d{2}-\\d{2}|\\d{1,2}:\\d{2}(:\\d{2})?|\\d{4}/\\d{1,2}/\\d{1,2}");
    private static final Pattern SQL_PATTERN =
            Pattern.compile("(?i)\\b(select|insert|update|delete|create|alter|truncate)\\b[^a-z]");
    private static final Pattern VERSION_PATTERN =
            Pattern.compile("(?i)v\\d+(\\.\\d+)+|version");

    private CompletenessChecker() {
    }

    public static Result check(String taskType, String text, List<String> requiredFields) {
        String t = text == null ? "" : text;
        String lower = t.toLowerCase();
        List<String> missing = new ArrayList<String>();
        List<String> fields = requiredFields != null
                ? requiredFields : TaskTypes.defaultRequiredFields(taskType);
        if (fields.isEmpty()) {
            return new Result(100, missing);
        }
        for (String field : fields) {
            if (!detect(field, t, lower)) {
                missing.add(field);
            }
        }
        int completeness = (int) Math.round((fields.size() - missing.size()) * 100.0 / fields.size());
        return new Result(completeness, missing);
    }

    /** 按字段名启发式检测输入材料中是否包含该信息 */
    static boolean detect(String field, String text, String lower) {
        if (field.contains("现象")) {
            return lower.contains("实际结果") || text.contains("现象") || text.contains("异常")
                    || text.contains("报错") || text.contains("失败") || text.contains("不一致");
        }
        if (field.contains("复现")) {
            return text.contains("复现") || text.contains("步骤") || text.contains("操作");
        }
        if (field.contains("预期")) {
            return text.contains("预期") || text.contains("期望");
        }
        if (field.contains("时间范围") || field.contains("发生时间")) {
            return text.contains("时间范围") || text.contains("发生时间") || text.contains("时间窗口")
                    || DATE_PATTERN.matcher(text).find();
        }
        if (field.contains("日志上下文")) {
            return text.contains("日志") || lower.contains("log") || text.contains("堆栈");
        }
        if (field.contains("异常关键词")) {
            return lower.contains("exception") || lower.contains("error") || lower.contains("timeout")
                    || lower.contains("lock wait") || text.contains("超时") || text.contains("异常")
                    || text.contains("失败");
        }
        if (field.contains("SQL")) {
            return SQL_PATTERN.matcher(text).find();
        }
        if (field.contains("分析目标")) {
            return text.contains("排查") || text.contains("检查") || text.contains("分析")
                    || text.contains("一致性") || text.contains("性能") || text.contains("方向")
                    || text.contains("目标") || text.contains("建议");
        }
        if (field.contains("版本")) {
            return VERSION_PATTERN.matcher(text).find() || text.contains("版本");
        }
        if (field.contains("回归范围")) {
            return text.contains("回归");
        }
        if (field.contains("需求背景")) {
            return text.contains("需求") || text.contains("背景");
        }
        if (field.contains("验收")) {
            return text.contains("验收") || text.contains("标准");
        }
        if (field.contains("Prompt")) {
            return lower.contains("prompt") || text.contains("提示词");
        }
        if (field.contains("评估")) {
            return text.contains("评估") || text.contains("标准") || text.contains("预期输出");
        }
        if (field.contains("结论来源")) {
            return text.contains("报告") || text.contains("结论") || text.contains("用例")
                    || text.contains("回归");
        }
        // 自定义字段：材料中出现字段名即视为已补充，否则材料较长时视为可用
        return text.contains(field) || text.length() >= 60;
    }
}
