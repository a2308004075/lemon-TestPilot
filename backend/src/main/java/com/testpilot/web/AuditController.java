package com.testpilot.web;

import com.testpilot.common.ApiResponse;
import com.testpilot.entity.AuditLog;
import com.testpilot.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 操作审计查询（详情抽屉时间线）。
 */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    @Autowired
    private AuditService auditService;

    @GetMapping
    public ApiResponse<List<AuditLog>> list(@RequestParam String entityType,
                                            @RequestParam String entityNo) {
        return ApiResponse.ok(auditService.list(entityType, entityNo));
    }
}
