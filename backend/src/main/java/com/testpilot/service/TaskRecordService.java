package com.testpilot.service;

import com.testpilot.common.BizException;
import com.testpilot.common.JsonUtil;
import com.testpilot.engine.CompletenessChecker;
import com.testpilot.engine.TaskExecutor;
import com.testpilot.engine.TaskTypes;
import com.testpilot.entity.TaskRecord;
import com.testpilot.repository.TaskRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 单页分析任务（Bug 分析 / 日志排查 / SQL 分析 / 用例生成 / 报告 / Prompt 测试）。
 */
@Service
public class TaskRecordService {

    @Autowired
    private TaskRecordRepository taskRepo;
    @Autowired
    private TaskExecutor executor;

    public Map<String, Object> list(String taskType, String keyword) {
        List<TaskRecord> all = taskType == null || taskType.isEmpty()
                ? taskRepo.findAllByOrderByUpdatedAtDescIdDesc()
                : taskRepo.findByTaskTypeOrderByUpdatedAtDescIdDesc(taskType);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        List<TaskRecord> filtered = new ArrayList<TaskRecord>();
        for (TaskRecord t : all) {
            if (!kw.isEmpty() && t.getTitle() != null && !t.getTitle().toLowerCase().contains(kw)
                    && t.getContext() != null && !t.getContext().toLowerCase().contains(kw)) {
                continue;
            }
            filtered.add(t);
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("total", filtered.size());
        result.put("list", filtered);
        return result;
    }

    /** 发起分析：走引擎（LLM 优先，规则降级），落任务记录 */
    public TaskRecord create(Map<String, Object> body) {
        String taskType = str(body.get("taskType"));
        if (!TaskTypes.isValid(taskType)) {
            throw new BizException("无效的任务类型：" + taskType);
        }
        String title = str(body.get("title"));
        if (title.trim().isEmpty()) {
            throw new BizException("任务标题不能为空");
        }
        String context = str(body.get("context"));
        if (context.trim().isEmpty()) {
            throw new BizException("问题上下文不能为空");
        }
        String risk = body.get("risk") == null ? "P1" : str(body.get("risk"));

        Map<String, Object> analyzed = executor.executeSingleTask(taskType, context,
                str(body.get("projectName")), str(body.get("moduleName")),
                str(body.get("submoduleName")), risk);

        TaskRecord task = new TaskRecord();
        task.setTaskNo(nextTaskNo());
        task.setTitle(title);
        task.setTaskType(taskType);
        task.setProjectName(str(body.get("projectName")));
        task.setModuleName(str(body.get("moduleName")));
        task.setSubmoduleName(str(body.get("submoduleName")));
        task.setRisk(risk);
        task.setContext(context);
        List<String> missing = (List<String>) analyzed.get("missing");
        task.setStatus(missing == null || missing.isEmpty() ? "已完成" : "待补充");
        task.setReviewStatus("待复核");
        task.setCompleteness((Integer) analyzed.get("completeness"));
        task.setOutputJson(JsonUtil.write(analyzed.get("output")));
        task.setEngineMode((String) analyzed.get("engineMode"));
        task.setSource("manual");
        return taskRepo.save(task);
    }

    public Map<String, Object> detail(Long id) {
        TaskRecord task = taskRepo.findById(id)
                .orElseThrow(() -> new BizException("任务不存在：" + id));
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("task", task);
        result.put("output", JsonUtil.readMap(task.getOutputJson()));
        return result;
    }

    /** 人工复核 */
    public TaskRecord review(Long id, String action, String note) {
        TaskRecord task = taskRepo.findById(id)
                .orElseThrow(() -> new BizException("任务不存在：" + id));
        if ("approve".equals(action)) {
            task.setReviewStatus("通过");
        } else if ("reject".equals(action)) {
            task.setReviewStatus("驳回补充");
            task.setStatus("待补充");
        } else {
            throw new BizException("无效的复核动作：" + action);
        }
        return taskRepo.save(task);
    }

    /** 输入完整度实时检查（新建分析页右侧面板） */
    public Map<String, Object> completeness(String taskType, String context) {
        if (!TaskTypes.isValid(taskType)) {
            throw new BizException("无效的任务类型：" + taskType);
        }
        CompletenessChecker.Result result = CompletenessChecker.check(taskType, context, null);
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("completeness", result.completeness);
        data.put("missing", result.missing);
        return data;
    }

    private String nextTaskNo() {
        long max = taskRepo.findAll().stream().mapToLong(TaskRecord::getId).max().orElse(0L);
        return "task_" + String.format("%03d", max + 1);
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }
}
