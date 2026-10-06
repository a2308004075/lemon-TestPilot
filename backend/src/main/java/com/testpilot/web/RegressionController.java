package com.testpilot.web;

import com.testpilot.common.ApiResponse;
import com.testpilot.service.RegressionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 回归测试：清单列表、生成回归、条目状态流转。
 */
@RestController
@RequestMapping("/api/regression")
public class RegressionController {

    @Autowired
    private RegressionService regressionService;

    @GetMapping("/lists")
    public ApiResponse<List<Map<String, Object>>> lists() {
        return ApiResponse.ok(regressionService.lists());
    }

    @GetMapping("/lists/{id}")
    public ApiResponse<Map<String, Object>> listDetail(@PathVariable Long id) {
        return ApiResponse.ok(regressionService.listDetail(id));
    }

    /** 生成回归清单：从高风险用例 + 历史Bug 汇总 */
    @PostMapping("/lists/generate")
    public ApiResponse<Map<String, Object>> generate(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(regressionService.generate(body));
    }

    /** 更新回归项状态（未执行/通过/失败/阻塞），重算清单进度 */
    @PutMapping("/items/{id}/status")
    public ApiResponse<Map<String, Object>> updateItemStatus(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        String status = body.get("status") == null ? "" : body.get("status").toString();
        return ApiResponse.ok(regressionService.updateItem(id, status));
    }
}
