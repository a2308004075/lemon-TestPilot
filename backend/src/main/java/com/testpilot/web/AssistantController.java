package com.testpilot.web;

import com.testpilot.common.ApiResponse;
import com.testpilot.service.AssistantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 测试助手：任务识别、完整执行。
 */
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    @Autowired
    private AssistantService assistantService;

    @GetMapping("/entries")
    public ApiResponse<List<Map<String, Object>>> entries() {
        return ApiResponse.ok(assistantService.entries());
    }

    /** 识别任务与缺项（执行前预检，不落库） */
    @PostMapping("/identify")
    public ApiResponse<Map<String, Object>> identify(@RequestBody Map<String, Object> body) {
        String inputText = body.get("inputText") == null ? "" : body.get("inputText").toString();
        return ApiResponse.ok(assistantService.identify(inputText, strList(body.get("selectedTypes"))));
    }

    /** 按流程完整执行：生成执行记录（路由快照 + 步骤明细） */
    @PostMapping("/execute")
    public ApiResponse<Map<String, Object>> execute(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(assistantService.execute(body));
    }

    @SuppressWarnings("unchecked")
    private static List<String> strList(Object value) {
        List<String> result = new ArrayList<String>();
        if (value instanceof List) {
            for (Object o : (List<Object>) value) {
                if (o != null && !o.toString().trim().isEmpty()) {
                    result.add(o.toString().trim());
                }
            }
        }
        return result;
    }
}
