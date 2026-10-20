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

    /** 人侧语义/关键词搜索（mode=semantic|keyword，默认 semantic 走三级降级） */
    @GetMapping("/search")
    public ApiResponse<Map<String, Object>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String mode) {
        return ApiResponse.ok(kbService.search(q, mode));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(kbService.detail(id));
    }

    /** 影响分析：硬引用（ref_count）+ 软引用命中（总数 + 最近 5 条） */
    @GetMapping("/{id}/impact")
    public ApiResponse<Map<String, Object>> impact(@PathVariable Long id) {
        return ApiResponse.ok(kbService.impact(id));
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

    /** 向量状态监控（知识库页状态条） */
    @GetMapping("/embeddings/status")
    public ApiResponse<Map<String, Object>> embeddingStatus() {
        return ApiResponse.ok(kbService.embeddingStatus());
    }

    /** 手动全量重嵌（换嵌入模型后；未配置 embedding 厂商时报错提示） */
    @PostMapping("/embeddings/reembed")
    public ApiResponse<Map<String, Object>> reembed() {
        return ApiResponse.ok(kbService.reembed());
    }
}
