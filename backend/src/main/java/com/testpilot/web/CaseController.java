package com.testpilot.web;

import com.testpilot.common.ApiResponse;
import com.testpilot.entity.TestCase;
import com.testpilot.service.CaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

    /** XLSX 批量导入（前端解析行，后端查重落库，重复/无效行跳过） */
    @PostMapping("/import")
    public ApiResponse<Map<String, Object>> importCases(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(caseService.importCases(body));
    }

    @GetMapping("/{id}")
    public ApiResponse<TestCase> detail(@PathVariable Long id) {
        return ApiResponse.ok(caseService.detail(id));
    }

    /** 编辑用例（版本由用户手动维护，不再自动升版） */
    @PutMapping("/{id}")
    public ApiResponse<TestCase> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(caseService.update(id, body));
    }

    @PostMapping("/{id}/copy")
    public ApiResponse<TestCase> copy(@PathVariable Long id) {
        return ApiResponse.ok(caseService.copy(id));
    }

    /** 删除用例（仅废弃状态可删，物理删除） */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        caseService.delete(id);
        return ApiResponse.ok(null);
    }

    /** 用例执行批次（6.11 W3）：创建 / 列表 / 详情 / 逐条登记 */
    @GetMapping("/batches")
    public ApiResponse<Map<String, Object>> batchList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(caseService.listBatches(page, size));
    }

    @PostMapping("/batches")
    public ApiResponse<Map<String, Object>> batchCreate(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(caseService.createBatch(body));
    }

    @GetMapping("/batches/{id}")
    public ApiResponse<Map<String, Object>> batchDetail(@PathVariable Long id) {
        return ApiResponse.ok(caseService.batchDetail(id));
    }

    @PatchMapping("/batches/{id}")
    public ApiResponse<Map<String, Object>> batchUpdate(@PathVariable Long id,
                                                        @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(caseService.updateBatch(id, body));
    }

    @DeleteMapping("/batches/{id}")
    public ApiResponse<Void> batchDelete(@PathVariable Long id) {
        caseService.deleteBatch(id);
        return ApiResponse.ok(null);
    }
}
