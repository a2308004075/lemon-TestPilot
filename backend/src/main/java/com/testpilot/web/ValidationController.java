package com.testpilot.web;

import com.testpilot.common.ApiResponse;
import com.testpilot.entity.ValidationCase;
import com.testpilot.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 案例验证：固定 walkthrough 验证路由与安全规则。
 */
@RestController
@RequestMapping("/api/validation")
public class ValidationController {

    @Autowired
    private ValidationService validationService;

    @GetMapping("/cases")
    public ApiResponse<List<ValidationCase>> cases() {
        return ApiResponse.ok(validationService.cases());
    }

    /** 运行验证：校验路由顺序与五条安全规则 */
    @PostMapping("/cases/{id}/run")
    public ApiResponse<Map<String, Object>> run(@PathVariable Long id) {
        return ApiResponse.ok(validationService.run(id));
    }
}
