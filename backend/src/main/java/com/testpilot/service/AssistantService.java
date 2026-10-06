package com.testpilot.service;

import com.testpilot.common.BizException;
import com.testpilot.engine.CompletenessChecker;
import com.testpilot.engine.TaskExecutor;
import com.testpilot.engine.TaskRouter;
import com.testpilot.engine.TaskTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI 测试助手：任务识别（路由+完整度预检）、完整执行。
 */
@Service
public class AssistantService {

    @Autowired
    private TaskRouter router;
    @Autowired
    private TaskExecutor executor;

    /** 7 个任务入口（卡片勾选区数据源） */
    public List<Map<String, Object>> entries() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (String type : TaskTypes.ORDER) {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("taskType", type);
            m.put("name", TaskTypes.nameOf(type));
            m.put("triggerWords", router.triggerWords(type));
            m.put("requiredFields", router.requiredFields(type));
            m.put("workflowSteps", router.workflowSteps(type));
            result.add(m);
        }
        return result;
    }

    /**
     * 识别任务与缺项：路由（主任务+辅助任务）+ 各任务完整度预检，不落库。
     */
    public Map<String, Object> identify(String inputText, List<String> selectedTypes) {
        if ((inputText == null || inputText.trim().isEmpty())
                && (selectedTypes == null || selectedTypes.isEmpty())) {
            throw new BizException("请输入材料描述，或手动勾选任务入口");
        }
        List<TaskRouter.Route> routes = router.route(inputText, selectedTypes);
        if (routes.isEmpty()) {
            throw new BizException("未识别到任何任务入口：请补充材料中的现象/日志/SQL 等关键词，或手动勾选任务入口");
        }

        List<Map<String, Object>> routeList = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> checks = new ArrayList<Map<String, Object>>();
        Set<String> missingAll = new LinkedHashSet<String>();
        int sum = 0;
        for (TaskRouter.Route r : routes) {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("taskType", r.taskType);
            m.put("taskName", r.taskName);
            m.put("role", r.role);
            m.put("seq", r.seq);
            m.put("score", r.score);
            m.put("matchedWords", r.matchedWords);
            m.put("reason", r.reason);
            routeList.add(m);

            List<String> required = router.requiredFields(r.taskType);
            CompletenessChecker.Result check =
                    CompletenessChecker.check(r.taskType, inputText, required);
            Map<String, Object> c = new LinkedHashMap<String, Object>();
            c.put("taskType", r.taskType);
            c.put("taskName", r.taskName);
            c.put("completeness", check.completeness);
            c.put("missing", check.missing);
            c.put("requiredFields", required);
            checks.add(c);
            missingAll.addAll(check.missing);
            sum += check.completeness;
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("routes", routeList);
        result.put("mainTask", routeList.isEmpty() ? null : routeList.get(0).get("taskType"));
        result.put("mainTaskName", routeList.isEmpty() ? null : routeList.get(0).get("taskName"));
        result.put("checks", checks);
        result.put("overallCompleteness", sum / Math.max(1, routes.size()));
        result.put("missingAll", new ArrayList<String>(missingAll));
        result.put("securityRules", TaskTypes.SECURITY_RULES);
        result.put("executed", false);
        return result;
    }

    /** 完整执行：识别路由 → 逐步分析 → 生成执行记录（快照+步骤） */
    public Map<String, Object> execute(Map<String, Object> body) {
        String inputText = str(body.get("inputText"));
        List<String> selectedTypes = strList(body.get("selectedTypes"));
        if (inputText.trim().isEmpty() && selectedTypes.isEmpty()) {
            throw new BizException("请输入材料描述，或手动勾选任务入口");
        }
        return executor.executeRun(inputText, str(body.get("projectName")), str(body.get("moduleName")),
                str(body.get("submoduleName")), selectedTypes, str(body.get("title")));
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    @SuppressWarnings("unchecked")
    private static List<String> strList(Object value) {
        List<String> result = new ArrayList<String>();
        if (value instanceof List) {
            for (Object o : (List<Object>) value) {
                if (o != null && !o.toString().trim().isEmpty()) {
                    result.add(o.toString().trim());
                }
            }
        }
        return result;
    }
}
