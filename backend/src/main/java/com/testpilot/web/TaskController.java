package com.testpilot.web;

import com.testpilot.common.ApiResponse;
import com.testpilot.entity.TaskRecord;
import com.testpilot.service.TaskRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 分析任务（Bug 分析 / 日志排查 / SQL 分析等单页任务共用）。
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskRecordService taskService;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(taskService.list(type, keyword));
    }

    /** 发起分析：走引擎（LLM 优先，规则降级） */
    @PostMapping
    public ApiResponse<TaskRecord> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(taskService.create(body));
    }

    /** 输入完整度实时检查（新建分析页右侧面板） */
    @PostMapping("/completeness")
    public ApiResponse<Map<String, Object>> completeness(@RequestBody Map<String, Object> body) {
        String taskType = body.get("taskType") == null ? "" : body.get("taskType").toString();
        String context = body.get("context") == null ? "" : body.get("context").toString();
        return ApiResponse.ok(taskService.completeness(taskType, context));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(taskService.detail(id));
    }

    /** 人工复核：确认通过 / 驳回补充 */
    @PostMapping("/{id}/review")
    public ApiResponse<TaskRecord> review(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String action = body.get("action") == null ? "" : body.get("action").toString();
        String note = body.get("note") == null ? "" : body.get("note").toString();
        return ApiResponse.ok(taskService.review(id, action, note));
    }

    /** 申请入知识库：复核通过的分析结论沉淀为待审核知识 */
    @PostMapping("/{id}/to-knowledge")
    public ApiResponse<TaskRecord> toKnowledge(@PathVariable Long id) {
        return ApiResponse.ok(taskService.toKnowledge(id));
    }
}
