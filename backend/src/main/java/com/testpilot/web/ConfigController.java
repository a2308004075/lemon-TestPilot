package com.testpilot.web;

import com.testpilot.common.ApiResponse;
import com.testpilot.entity.SysLlmConfig;
import com.testpilot.entity.SysModule;
import com.testpilot.entity.SysOutputTemplate;
import com.testpilot.entity.SysTaskKbRule;
import com.testpilot.entity.SysTaskRule;
import com.testpilot.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 配置中心：项目与模块、大模型、输出格式模板、任务关联规则。
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Autowired
    private ConfigService configService;

    // ---------------- 项目与模块 ----------------

    @GetMapping("/modules")
    public ApiResponse<List<Map<String, Object>>> moduleTree() {
        return ApiResponse.ok(configService.moduleTree());
    }

    @PostMapping("/modules")
    public ApiResponse<SysModule> createModule(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(configService.createModule(body));
    }

    /** 编辑三级模块名称 */
    @PutMapping("/modules/{id}")
    public ApiResponse<SysModule> updateModule(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(configService.updateModule(id, body));
    }

    /** 启用 / 停用 */
    @PutMapping("/modules/{id}/toggle")
    public ApiResponse<SysModule> toggleModule(@PathVariable Long id) {
        return ApiResponse.ok(configService.toggleModule(id));
    }

    // ---------------- 大模型 ----------------

    @GetMapping("/llm")
    public ApiResponse<List<SysLlmConfig>> llmList() {
        return ApiResponse.ok(configService.llmList());
    }

    @PutMapping("/llm/{id}")
    public ApiResponse<SysLlmConfig> llmUpdate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(configService.llmUpdate(id, body));
    }

    @PostMapping("/llm/{id}/test-connection")
    public ApiResponse<Map<String, Object>> llmTest(@PathVariable Long id) {
        return ApiResponse.ok(configService.llmTest(id));
    }

    // ---------------- 输出格式模板 ----------------

    @GetMapping("/templates")
    public ApiResponse<List<SysOutputTemplate>> templates(
            @RequestParam(required = false) String taskType) {
        return ApiResponse.ok(configService.templates(taskType));
    }

    /** 模板字段全量同步（删除字段仅隐藏，原始快照保留） */
    @PutMapping("/templates/{taskType}")
    public ApiResponse<List<SysOutputTemplate>> templateUpdate(
            @PathVariable String taskType, @RequestBody Map<String, Object> body) {
        Object fields = body.get("fields");
        if (!(fields instanceof List)) {
            return ApiResponse.error("fields 必须为数组");
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fieldList = (List<Map<String, Object>>) fields;
        return ApiResponse.ok(configService.templateUpdate(taskType, fieldList));
    }

    // ---------------- 任务关联规则 ----------------

    @GetMapping("/rules")
    public ApiResponse<List<Map<String, Object>>> rules() {
        return ApiResponse.ok(configService.rules());
    }

    @PutMapping("/rules/{id}")
    public ApiResponse<SysTaskRule> ruleUpdate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(configService.ruleUpdate(id, body));
    }

    // ---------------- 任务与知识关联 ----------------

    @GetMapping("/kb-rules")
    public ApiResponse<List<Map<String, Object>>> kbRules() {
        return ApiResponse.ok(configService.kbRules());
    }

    /** 保存任务的知识分类关联（全量覆盖） */
    @PutMapping("/kb-rules/{mainTaskType}")
    public ApiResponse<List<SysTaskKbRule>> kbRuleSave(
            @PathVariable String mainTaskType, @RequestBody Map<String, Object> body) {
        Object categories = body.get("categories");
        if (!(categories instanceof List)) {
            return ApiResponse.error("categories 必须为数组");
        }
        @SuppressWarnings("unchecked")
        List<String> list = (List<String>) categories;
        return ApiResponse.ok(configService.saveKbRule(mainTaskType, list));
    }
}
