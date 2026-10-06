package com.testpilot.web;

import com.testpilot.common.ApiResponse;
import com.testpilot.entity.RunRecord;
import com.testpilot.service.RunService;
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
 * 执行记录：列表、详情（快照+步骤）、人工复核。
 */
@RestController
@RequestMapping("/api/runs")
public class RunController {

    @Autowired
    private RunService runService;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(runService.list(keyword, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(runService.detail(id));
    }

    /** 人工复核：确认通过 / 驳回补充 */
    @PostMapping("/{id}/review")
    public ApiResponse<RunRecord> review(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String action = body.get("action") == null ? "" : body.get("action").toString();
        String note = body.get("note") == null ? "" : body.get("note").toString();
        return ApiResponse.ok(runService.review(id, action, note));
    }
}
