package com.testpilot.web;

import com.testpilot.common.ApiResponse;
import com.testpilot.service.WorkbenchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 工作台编排：7 个标准入口配置，保存并发布新版本。
 */
@RestController
@RequestMapping("/api/workbench")
public class WorkbenchController {

    @Autowired
    private WorkbenchService workbenchService;

    @GetMapping("/entries")
    public ApiResponse<List<Map<String, Object>>> entries() {
        return ApiResponse.ok(workbenchService.entries());
    }

    /** 保存并发布新版本：版本号递增，历史执行保留当时快照 */
    @PutMapping("/entries/{taskType}")
    public ApiResponse<Map<String, Object>> publish(
            @PathVariable String taskType, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(workbenchService.publish(taskType, body));
    }
}
