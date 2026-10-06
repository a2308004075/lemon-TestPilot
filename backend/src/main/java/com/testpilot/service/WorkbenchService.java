package com.testpilot.service;

import com.testpilot.common.BizException;
import com.testpilot.entity.WorkbenchEntry;
import com.testpilot.entity.WorkbenchEntryConfig;
import com.testpilot.repository.WorkbenchEntryConfigRepository;
import com.testpilot.repository.WorkbenchEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作台编排：7 个标准入口的触发词/必填信息/知识库映射/工作流步骤配置。
 * 事实留在知识库；工作台只维护入口与规则；修改后版本号递增，历史执行保留当时快照。
 */
@Service
public class WorkbenchService {

    @Autowired
    private WorkbenchEntryRepository entryRepo;
    @Autowired
    private WorkbenchEntryConfigRepository configRepo;

    public List<Map<String, Object>> entries() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (WorkbenchEntry entry : entryRepo.findAllByOrderByIdAsc()) {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", entry.getId());
            m.put("taskType", entry.getTaskType());
            m.put("name", entry.getName());
            m.put("icon", entry.getIcon());
            m.put("version", entry.getVersion());
            m.put("published", entry.getPublished());
            WorkbenchEntryConfig cfg =
                    configRepo.findTopByTaskTypeOrderByVersionDesc(entry.getTaskType()).orElse(null);
            if (cfg != null) {
                m.put("configVersion", cfg.getVersion());
                m.put("triggerWords", splitLines(cfg.getTriggerWords()));
                m.put("requiredFields", splitLines(cfg.getRequiredFields()));
                m.put("kbMappings", splitLines(cfg.getKbMappings()));
                m.put("workflowSteps", splitLines(cfg.getWorkflowSteps()));
                m.put("publishedAt", cfg.getPublishedAt());
            }
            result.add(m);
        }
        return result;
    }

    /** 保存并发布新版本：版本号递增，历史执行记录保留当时快照 */
    public Map<String, Object> publish(String taskType, Map<String, Object> body) {
        WorkbenchEntry entry = entryRepo.findAllByOrderByIdAsc().stream()
                .filter(e -> taskType.equals(e.getTaskType()))
                .findFirst()
                .orElseThrow(() -> new BizException("工作台入口不存在：" + taskType));

        int newVersion = configRepo.findTopByTaskTypeOrderByVersionDesc(taskType)
                .map(c -> c.getVersion() + 1).orElse(1);

        WorkbenchEntryConfig cfg = new WorkbenchEntryConfig();
        cfg.setEntryId(entry.getId());
        cfg.setTaskType(taskType);
        cfg.setVersion(newVersion);
        cfg.setTriggerWords(joinLines(body.get("triggerWords")));
        cfg.setRequiredFields(joinLines(body.get("requiredFields")));
        cfg.setKbMappings(joinLines(body.get("kbMappings")));
        cfg.setWorkflowSteps(joinLines(body.get("workflowSteps")));
        cfg.setPublishedAt(LocalDateTime.now());
        configRepo.save(cfg);

        entry.setVersion(newVersion);
        entry.setPublished(true);
        entryRepo.save(entry);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("taskType", taskType);
        result.put("version", newVersion);
        result.put("publishedAt", cfg.getPublishedAt());
        return result;
    }

    private static List<String> splitLines(String text) {
        List<String> result = new ArrayList<String>();
        if (text != null) {
            for (String line : text.split("\n")) {
                String t = line.trim();
                if (!t.isEmpty()) {
                    result.add(t);
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static String joinLines(Object value) {
        if (value instanceof List) {
            StringBuilder sb = new StringBuilder();
            for (Object o : (List<Object>) value) {
                if (o == null || o.toString().trim().isEmpty()) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(o.toString().trim());
            }
            return sb.toString();
        }
        return value == null ? "" : value.toString();
    }
}
