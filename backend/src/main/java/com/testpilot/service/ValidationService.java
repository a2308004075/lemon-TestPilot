package com.testpilot.service;

import com.testpilot.common.BizException;
import com.testpilot.engine.TaskExecutor;
import com.testpilot.engine.TaskRouter;
import com.testpilot.entity.ValidationCase;
import com.testpilot.repository.ValidationCaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 案例验证：用固定 walkthrough 验证路由、流程与安全规则，发布工作台资产前先回归。
 */
@Service
public class ValidationService {

    @Autowired
    private ValidationCaseRepository caseRepo;
    @Autowired
    private TaskRouter router;
    @Autowired
    private TaskExecutor executor;

    public List<ValidationCase> cases() {
        return caseRepo.findAllByOrderByIdAsc();
    }

    /** 运行验证案例：校验路由顺序与安全规则 */
    public Map<String, Object> run(Long id) {
        ValidationCase vc = caseRepo.findById(id)
                .orElseThrow(() -> new BizException("验证案例不存在：" + id));

        List<TaskRouter.Route> routes = router.route(vc.getMaterial(), null);
        List<String> actual = new ArrayList<String>();
        for (TaskRouter.Route r : routes) {
            actual.add(r.taskType);
        }
        List<String> expected = splitComma(vc.getExpectedRoutes());

        // 逐路由分析（不累加引用计数、不落库）
        List<Map<String, Object>> outputs = new ArrayList<Map<String, Object>>();
        for (TaskRouter.Route r : routes) {
            TaskExecutor.RouteOutput ro = executor.analyzeForValidation(
                    r.taskType, vc.getMaterial(), "", "", "", "P1");
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("taskType", r.taskType);
            m.put("taskName", r.taskName);
            m.put("role", r.role);
            m.put("output", ro.output);
            outputs.add(m);
        }

        List<Map<String, Object>> checks = new ArrayList<Map<String, Object>>();
        boolean allPassed = true;

        // 1. 路由顺序符合预期
        boolean routeOk = actual.equals(expected);
        allPassed &= routeOk;
        checks.add(check("路由顺序符合预期", routeOk,
                "预期：" + joinArrow(expected) + "；实际：" + joinArrow(actual)));

        // 2. 不跳结论：每个输出都带人工复核点
        boolean noJump = true;
        for (Map<String, Object> o : outputs) {
            Map<String, Object> output = (Map<String, Object>) o.get("output");
            Object points = output.get("human_review_points");
            if (!(points instanceof List) || ((List<?>) points).isEmpty()) {
                noJump = false;
            }
        }
        allPassed &= noJump;
        checks.add(check("不跳结论（输出含人工复核点）", noJump, ""));

        // 3. 根因需证据：输出包含 need_more_info 且缺项被明确标记
        boolean evidenceOk = true;
        for (Map<String, Object> o : outputs) {
            Map<String, Object> output = (Map<String, Object>) o.get("output");
            if (!output.containsKey("need_more_info")) {
                evidenceOk = false;
            }
        }
        allPassed &= evidenceOk;
        checks.add(check("根因需证据（缺项明确标记）", evidenceOk, ""));

        // 4. 至少 6 个必回归项
        int regCount = 0;
        for (Map<String, Object> o : outputs) {
            if ("regression_list".equals(o.get("taskType"))) {
                Map<String, Object> output = (Map<String, Object>) o.get("output");
                Object items = output.get("regression_items");
                if (items instanceof List) {
                    regCount = ((List<?>) items).size();
                }
            }
        }
        boolean regOk = regCount >= 6;
        allPassed &= regOk;
        checks.add(check("至少 6 个必回归项", regOk, "实际生成 " + regCount + " 项"));

        // 5. P0/P1 人工复核
        boolean reviewOk = true;
        for (Map<String, Object> o : outputs) {
            Map<String, Object> output = (Map<String, Object>) o.get("output");
            Object points = output.get("human_review_points");
            if (!(points instanceof List) || !points.toString().contains("P0/P1")) {
                reviewOk = false;
            }
        }
        allPassed &= reviewOk;
        checks.add(check("P0/P1 人工复核", reviewOk, ""));

        vc.setLastRunAt(LocalDateTime.now());
        vc.setLastResult(allPassed ? "通过" : "未通过");
        vc.setLastRoutes(joinArrow(actual));
        caseRepo.save(vc);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("caseId", vc.getId());
        result.put("caseNo", vc.getCaseNo());
        result.put("title", vc.getTitle());
        result.put("material", vc.getMaterial());
        result.put("expectedRoutes", expected);
        result.put("expectedChecks", splitLines(vc.getExpectedChecks()));
        result.put("actualRoutes", actual);
        result.put("checks", checks);
        result.put("outputs", outputs);
        result.put("passed", allPassed);
        result.put("lastRunAt", vc.getLastRunAt());
        return result;
    }

    private Map<String, Object> check(String name, boolean passed, String detail) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("name", name);
        m.put("passed", passed);
        m.put("detail", detail == null ? "" : detail);
        return m;
    }

    private static List<String> splitComma(String text) {
        List<String> result = new ArrayList<String>();
        if (text != null) {
            for (String part : text.split(",")) {
                String t = part.trim();
                if (!t.isEmpty()) {
                    result.add(t);
                }
            }
        }
        return result;
    }

    private static List<String> splitLines(String text) {
        List<String> result = new ArrayList<String>();
        if (text != null) {
            for (String line : text.split("\n")) {
                String t = line.trim();
                if (!t.isEmpty()) {
                    result.add(t);
                }
            }
        }
        return result;
    }

    private static String joinArrow(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(" → ");
            }
            sb.append(items.get(i));
        }
        return sb.toString();
    }
}
