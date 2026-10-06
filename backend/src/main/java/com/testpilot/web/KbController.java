package com.testpilot.web;

import com.testpilot.common.ApiResponse;
import com.testpilot.entity.KbItem;
import com.testpilot.service.KbService;
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
 * 知识库：列表、统计、已入库经验、详情、新增、升版、审核、撤销入库。
 */
@RestController
@RequestMapping("/api/kb")
public class KbController {

    @Autowired
    private KbService kbService;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String moduleName) {
        return ApiResponse.ok(kbService.list(keyword, category, status, moduleName));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.ok(kbService.stats());
    }

    @GetMapping("/categories")
    public ApiResponse<List<String>> categories() {
        return ApiResponse.ok(kbService.categories());
    }

    /** 已入库经验：来源于分析任务的知识 */
    @GetMapping("/task-sourced")
    public ApiResponse<List<KbItem>> taskSourced(@RequestParam(required = false) String category) {
        return ApiResponse.ok(kbService.taskSourced(category));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(kbService.detail(id));
    }

    @PostMapping
    public ApiResponse<KbItem> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(kbService.create(body));
    }

    /** 编辑内容（升版） */
    @PutMapping("/{id}")
    public ApiResponse<KbItem> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(kbService.update(id, body));
    }

    /** 审核：通过 / 驳回 */
    @PostMapping("/{id}/review")
    public ApiResponse<KbItem> review(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String action = body.get("action") == null ? "" : body.get("action").toString();
        String note = body.get("note") == null ? "" : body.get("note").toString();
        return ApiResponse.ok(kbService.review(id, action, note));
    }

    /** 撤销入库（被引用时阻止） */
    @PostMapping("/{id}/revoke")
    public ApiResponse<KbItem> revoke(@PathVariable Long id) {
        return ApiResponse.ok(kbService.revoke(id));
    }
}
