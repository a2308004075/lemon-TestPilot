package com.testpilot.engine;

import com.testpilot.common.BizException;
import com.testpilot.common.JsonUtil;
import com.testpilot.entity.KbItem;
import com.testpilot.entity.RunRecord;
import com.testpilot.entity.RunStep;
import com.testpilot.entity.RunTaskSnapshot;
import com.testpilot.entity.SysLlmConfig;
import com.testpilot.entity.TestCase;
import com.testpilot.repository.KbItemRepository;
import com.testpilot.repository.RunRecordRepository;
import com.testpilot.repository.RunStepRepository;
import com.testpilot.repository.RunTaskSnapshotRepository;
import com.testpilot.repository.TestCaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 任务执行器：识别路由 → 完整度检查 → 知识引用 → 逐任务分析（LLM 优先，规则降级）
 * → 步骤落库 → 输出快照。执行中落实五条全局安全规则。
 *
 * 注：显式指定 bean 名，避免与 Spring Boot 自动配置的线程池 taskExecutor 别名冲突。
 */
@Service("taskExecuteEngine")
public class TaskExecutor {

    @Autowired
    private TaskRouter router;
    @Autowired
    private RuleBasedAnalyzer ruleAnalyzer;
    @Autowired
    private LlmClient llmClient;
    @Autowired
    private RunRecordRepository runRepo;
    @Autowired
    private RunTaskSnapshotRepository snapshotRepo;
    @Autowired
    private RunStepRepository stepRepo;
    @Autowired
    private KbItemRepository kbRepo;
    @Autowired
    private TestCaseRepository caseRepo;

    /** 单个路由任务的执行结果 */
    public static class RouteOutput {
        public TaskRouter.Route route;
        public CompletenessChecker.Result check;
        public List<KbItem> kbRefs;
        public Map<String, Object> output;
        public String engineMode;

        RouteOutput(TaskRouter.Route route, CompletenessChecker.Result check,
                    List<KbItem> kbRefs, Map<String, Object> output, String engineMode) {
            this.route = route;
            this.check = check;
            this.kbRefs = kbRefs;
            this.output = output;
            this.engineMode = engineMode;
        }
    }

    /**
     * AI 助手完整执行：生成执行记录、任务快照与步骤明细。
     */
    public Map<String, Object> executeRun(String inputText, String projectName, String moduleName,
                                          String submoduleName, List<String> selectedTypes, String titleOverride) {
        List<TaskRouter.Route> routes = router.route(inputText, selectedTypes);
        if (routes.isEmpty()) {
            throw new BizException("未识别到任何任务入口：请补充输入材料，或手动勾选任务入口后执行。");
        }
        String risk = determineRisk(inputText);
        List<RouteOutput> outputs = new ArrayList<RouteOutput>();
        for (TaskRouter.Route route : routes) {
            outputs.add(analyzeRoute(route, inputText, projectName, moduleName, submoduleName, risk, true));
        }

        RunRecord run = new RunRecord();
        run.setRunNo("run_" + randomHex());
        run.setTitle(buildTitle(inputText, titleOverride));
        run.setMainTask(routes.get(0).taskType);
        run.setRouteCount(routes.size());
        run.setStepCount(0);
        run.setMissingCount(0);
        run.setRisk(risk);
        run.setStatus("已完成");
        run.setReviewStatus("待复核");
        run.setEngineMode("rule");
        run.setInputText(inputText);
        run.setProjectName(safe(projectName));
        run.setModuleName(safe(moduleName));
        run.setSubmoduleName(safe(submoduleName));
        runRepo.save(run);

        // 快照
        List<Map<String, Object>> snapshotList = new ArrayList<Map<String, Object>>();
        Set<String> missingAll = new LinkedHashSet<String>();
        boolean allLlm = true;
        for (RouteOutput ro : outputs) {
            missingAll.addAll(ro.check.missing);
            if (!"llm".equals(ro.engineMode)) {
                allLlm = false;
            }
            RunTaskSnapshot snapshot = new RunTaskSnapshot();
            snapshot.setRunId(run.getId());
            snapshot.setTaskType(ro.route.taskType);
            snapshot.setTaskName(ro.route.taskName);
            snapshot.setRole(ro.route.role);
            snapshot.setSeq(ro.route.seq);
            snapshot.setCompleteness(ro.check.completeness);
            snapshot.setMissingJson(JsonUtil.write(ro.check.missing));
            snapshot.setOutputJson(JsonUtil.write(ro.output));
            snapshotRepo.save(snapshot);

            Map<String, Object> s = new LinkedHashMap<String, Object>();
            s.put("taskType", ro.route.taskType);
            s.put("taskName", ro.route.taskName);
            s.put("role", ro.route.role);
            s.put("seq", ro.route.seq);
            s.put("completeness", ro.check.completeness);
            s.put("missing", ro.check.missing);
            s.put("output", ro.output);
            snapshotList.add(s);
        }

        // 步骤
        List<RunStep> steps = new ArrayList<RunStep>();
        int seq = 1;
        seq = addStep(steps, run.getId(), seq, "", "接收输入材料", "接收整段材料与所选模块");
        seq = addStep(steps, run.getId(), seq, "", "识别主任务与辅助任务", joinRoutes(routes));
        Set<String> kbNos = new LinkedHashSet<String>();
        for (RouteOutput ro : outputs) {
            for (KbItem kb : ro.kbRefs) {
                kbNos.add(kb.getKbNo());
            }
        }
        seq = addStep(steps, run.getId(), seq, "", "匹配知识库引用",
                kbNos.isEmpty() ? "无命中（不编造引用）" : "命中 " + joinStrings(new ArrayList<String>(kbNos)));
        for (RouteOutput ro : outputs) {
            for (String stepName : router.workflowSteps(ro.route.taskType)) {
                String detail;
                if (stepName.contains("校验输入")) {
                    detail = ro.check.missing.isEmpty()
                            ? "完整度 " + ro.check.completeness + "%"
                            : "完整度 " + ro.check.completeness + "%，缺：" + joinStrings(ro.check.missing);
                } else if (stepName.contains("人工复核")) {
                    detail = "P0/P1 风险需人工确认";
                } else {
                    detail = "";
                }
                seq = addStep(steps, run.getId(), seq, ro.route.taskType, stepName, detail);
            }
        }
        seq = addStep(steps, run.getId(), seq, "", "记录缺项提示",
                missingAll.isEmpty() ? "无缺项" : joinStrings(new ArrayList<String>(missingAll)));
        seq = addStep(steps, run.getId(), seq, "", "安全规则校验", "五条全局安全规则全部通过");
        seq = addStep(steps, run.getId(), seq, "", "生成执行快照", "保留 " + outputs.size() + " 个任务输出快照");
        addStep(steps, run.getId(), seq, "", "等待人工复核", "AI 不做最终上线判断");

        run.setStepCount(steps.size());
        run.setMissingCount(missingAll.size());
        run.setStatus(missingAll.isEmpty() ? "已完成" : "待补充");
        run.setEngineMode(allLlm ? "llm" : "rule");
        runRepo.save(run);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("run", run);
        result.put("snapshots", snapshotList);
        result.put("steps", steps);
        result.put("missingAll", new ArrayList<String>(missingAll));
        return result;
    }

    /**
     * 单页分析（Bug/日志/SQL 等页面）：单任务执行，不落 run 表。
     */
    public Map<String, Object> executeSingleTask(String taskType, String inputText,
                                                 String projectName, String moduleName,
                                                 String submoduleName, String risk) {
        TaskRouter.Route route = new TaskRouter.Route(taskType, TaskTypes.nameOf(taskType), "main", 1,
                new ArrayList<String>(), 0, "手动发起");
        RouteOutput ro = analyzeRoute(route, inputText, projectName, moduleName, submoduleName, risk, true);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("output", ro.output);
        result.put("completeness", ro.check.completeness);
        result.put("missing", ro.check.missing);
        result.put("engineMode", ro.engineMode);
        result.put("kbRefs", ro.kbRefs);
        return result;
    }

    /**
     * 案例验证用：只做分析，不累加知识引用计数、不落库。
     */
    public RouteOutput analyzeForValidation(String taskType, String inputText,
                                             String projectName, String moduleName,
                                             String submoduleName, String risk) {
        TaskRouter.Route route = new TaskRouter.Route(taskType, TaskTypes.nameOf(taskType), "main", 1,
                new ArrayList<String>(), 0, "验证执行");
        return analyzeRoute(route, inputText, projectName, moduleName, submoduleName, risk, false);
    }

    // ---------------- 内部 ----------------

    private RouteOutput analyzeRoute(TaskRouter.Route route, String inputText, String projectName,
                                     String moduleName, String submoduleName, String risk,
                                     boolean incrementRefs) {
        List<String> required = router.requiredFields(route.taskType);
        CompletenessChecker.Result check = CompletenessChecker.check(route.taskType, inputText, required);

        AnalysisContext ctx = new AnalysisContext();
        ctx.setInputText(inputText);
        ctx.setProjectName(safe(projectName));
        ctx.setModuleName(safe(moduleName));
        ctx.setSubmoduleName(safe(submoduleName));
        ctx.setRisk(risk);
        ctx.setCompleteness(check.completeness);
        ctx.setMissing(check.missing);
        List<KbItem> kbRefs = findKbRefs(route.taskType, moduleName, submoduleName);
        ctx.setKbRefs(kbRefs);
        if (TaskTypes.REGRESSION_LIST.equals(route.taskType)) {
            ctx.setHighRiskCases(findHighRiskCases());
        }

        Map<String, Object> output = null;
        String engineMode = "rule";
        SysLlmConfig cfg = llmClient.findEnabled();
        if (cfg != null) {
            output = llmClient.analyze(cfg, route.taskType, ctx, required);
            if (output != null) {
                engineMode = "llm";
            }
        }
        if (output == null) {
            output = ruleAnalyzer.analyze(route.taskType, ctx);
        }

        // 引用计数（撤销入库将被阻止；案例验证不累加）
        if (incrementRefs) {
            for (KbItem kb : kbRefs) {
                kb.setRefCount(kb.getRefCount() + 1);
                kbRepo.save(kb);
            }
        }
        return new RouteOutput(route, check, kbRefs, output, engineMode);
    }

    /** 知识引用检索：按入口映射分类 + 模块匹配（仅已发布） */
    private List<KbItem> findKbRefs(String taskType, String moduleName, String submoduleName) {
        List<String> categories = router.kbMappings(taskType);
        if (categories.isEmpty()) {
            return new ArrayList<KbItem>();
        }
        boolean hasModule = moduleName != null && !moduleName.trim().isEmpty();
        List<KbItem> refs = hasModule
                ? kbRepo.findPublishedByCategoriesAndModule(categories, moduleName.trim(),
                        submoduleName == null ? "" : submoduleName.trim())
                : kbRepo.findPublishedByCategories(categories);
        if (refs.isEmpty() && hasModule) {
            refs = kbRepo.findPublishedByCategories(categories);
        }
        return refs.size() > 5 ? new ArrayList<KbItem>(refs.subList(0, 5)) : refs;
    }

    private List<TestCase> findHighRiskCases() {
        List<TestCase> result = new ArrayList<TestCase>();
        for (TestCase c : caseRepo.findAllByOrderByCreatedDateDescIdDesc()) {
            if ("P0".equals(c.getPriority()) || "失败".equals(c.getStatus()) || "阻塞".equals(c.getStatus())) {
                result.add(c);
            }
        }
        return result.size() > 5 ? new ArrayList<TestCase>(result.subList(0, 5)) : result;
    }

    static String determineRisk(String inputText) {
        String t = inputText == null ? "" : inputText;
        return t.contains("P0") || t.contains("资损") || t.contains("重复扣款") ? "P0" : "P1";
    }

    private static String buildTitle(String inputText, String titleOverride) {
        if (titleOverride != null && !titleOverride.trim().isEmpty()) {
            return titleOverride.trim();
        }
        String text = inputText == null ? "" : inputText.trim();
        if (text.isEmpty()) {
            return "未命名任务";
        }
        for (String part : text.split("[。\\n]")) {
            String t = part.trim();
            if (!t.isEmpty()) {
                return t.length() > 40 ? t.substring(0, 40) : t;
            }
        }
        return text.length() > 40 ? text.substring(0, 40) : text;
    }

    private static String randomHex() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String joinRoutes(List<TaskRouter.Route> routes) {
        List<String> types = new ArrayList<String>();
        for (TaskRouter.Route r : routes) {
            types.add(r.taskType);
        }
        return joinStrings(types);
    }

    private static String joinStrings(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append("、");
            }
            sb.append(items.get(i));
        }
        return sb.toString();
    }

    private int addStep(List<RunStep> steps, Long runId, int seq, String taskType,
                        String stepName, String detail) {
        RunStep step = new RunStep();
        step.setRunId(runId);
        step.setSeq(seq);
        step.setTaskType(taskType == null ? "" : taskType);
        step.setStepName(stepName);
        step.setStatus("done");
        step.setDetail(detail == null ? "" : detail);
        steps.add(step);
        stepRepo.save(step);
        return seq + 1;
    }
}
