package com.testpilot.web;

import com.testpilot.common.ApiResponse;
import com.testpilot.entity.TestReport;
import com.testpilot.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 测试报告：列表、生成（勾选来源聚合结论）、详情。
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping
    public ApiResponse<List<TestReport>> list() {
        return ApiResponse.ok(reportService.list());
    }

    /** 生成统一报告：聚合任务/回归清单/用例，给出上线建议（AI 不批准上线） */
    @PostMapping("/generate")
    public ApiResponse<Map<String, Object>> generate(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(reportService.generate(body));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(reportService.detail(id));
    }
}
