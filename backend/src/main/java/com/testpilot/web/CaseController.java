package com.testpilot.web;

import com.testpilot.common.ApiResponse;
import com.testpilot.entity.TestCase;
import com.testpilot.service.CaseService;
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
 * 用例库：模块树、列表、新增、智能生成、编辑升版、复制。
 */
@RestController
@RequestMapping("/api/cases")
public class CaseController {

    @Autowired
    private CaseService caseService;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String moduleName,
            @RequestParam(required = false) String submoduleName,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(caseService.list(keyword, moduleName, submoduleName,
                priority, status, page, size));
    }

    @GetMapping("/tree")
    public ApiResponse<List<Map<String, Object>>> tree() {
        return ApiResponse.ok(caseService.tree());
    }

    /** 启用的三级模块（新建表单下拉数据源） */
    @GetMapping("/modules")
    public ApiResponse<List<Map<String, String>>> modules() {
        return ApiResponse.ok(caseService.modules());
    }

    /** 智能生成用例草稿（不落库，逐条确认后保存） */
    @PostMapping("/generate-drafts")
    public ApiResponse<List<Map<String, Object>>> generateDrafts(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(caseService.generateDrafts(body));
    }

    @PostMapping
    public ApiResponse<TestCase> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(caseService.create(body));
    }

    @GetMapping("/{id}")
    public ApiResponse<TestCase> detail(@PathVariable Long id) {
        return ApiResponse.ok(caseService.detail(id));
    }

    /** 编辑并升版 */
    @PutMapping("/{id}")
    public ApiResponse<TestCase> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(caseService.update(id, body));
    }

    @PostMapping("/{id}/copy")
    public ApiResponse<TestCase> copy(@PathVariable Long id) {
        return ApiResponse.ok(caseService.copy(id));
    }
}
