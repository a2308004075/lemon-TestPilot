package com.testpilot.engine;

import com.testpilot.entity.WorkbenchEntry;
import com.testpilot.entity.WorkbenchEntryConfig;
import com.testpilot.repository.WorkbenchEntryConfigRepository;
import com.testpilot.repository.WorkbenchEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务路由器：输入材料 → 匹配触发词 → 主任务 + 辅助任务。
 * 主任务只保留一个，其他命中项作为辅助任务依次执行。
 */
@Service
public class TaskRouter {

    public static class Route {
        public String taskType;
        public String taskName;
        /** main / aux */
        public String role;
        public int seq;
        public List<String> matchedWords = new ArrayList<String>();
        public int score;
        public String reason;

        public Route(String taskType, String taskName, String role, int seq,
                     List<String> matchedWords, int score, String reason) {
            this.taskType = taskType;
            this.taskName = taskName;
            this.role = role;
            this.seq = seq;
            this.matchedWords = matchedWords;
            this.score = score;
            this.reason = reason;
        }
    }

    @Autowired
    private WorkbenchEntryRepository entryRepo;

    @Autowired
    private WorkbenchEntryConfigRepository configRepo;

    /**
     * 路由识别：selectedTypes 非空时按手动勾选；否则按触发词匹配。
     */
    public List<Route> route(String inputText, List<String> selectedTypes) {
        String text = inputText == null ? "" : inputText.toLowerCase();
        List<Route> routes = new ArrayList<Route>();

        if (selectedTypes != null && !selectedTypes.isEmpty()) {
            int seq = 1;
            for (String type : selectedTypes) {
                if (!TaskTypes.isValid(type)) {
                    continue;
                }
                List<String> matched = matchWords(type, text);
                String role = seq == 1 ? "main" : "aux";
                routes.add(new Route(type, TaskTypes.nameOf(type), role, seq, matched,
                        matched.size(), "手动勾选"));
                seq++;
            }
            return routes;
        }

        Map<String, Integer> scores = new LinkedHashMap<String, Integer>();
        Map<String, List<String>> words = new LinkedHashMap<String, List<String>>();
        for (String type : TaskTypes.ORDER) {
            List<String> matched = matchWords(type, text);
            words.put(type, matched);
            scores.put(type, matched.size());
        }

        List<String> matchedTypes = new ArrayList<String>();
        for (String type : TaskTypes.ORDER) {
            if (scores.get(type) > 0) {
                matchedTypes.add(type);
            }
        }
        if (matchedTypes.isEmpty()) {
            return routes;
        }

        // 主任务：得分最高，同分按预置优先顺序
        String main = null;
        for (String type : TaskTypes.ORDER) {
            if (matchedTypes.contains(type)) {
                if (main == null || scores.get(type) > scores.get(main)) {
                    main = type;
                }
            }
        }

        routes.add(new Route(main, TaskTypes.nameOf(main), "main", 1, words.get(main),
                scores.get(main), "触发词命中：" + join(words.get(main))));

        // 辅助任务：得分降序、同分按预置顺序
        List<String> auxTypes = new ArrayList<String>();
        for (String type : TaskTypes.ORDER) {
            if (!type.equals(main) && matchedTypes.contains(type)) {
                auxTypes.add(type);
            }
        }
        auxTypes.sort(new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                int byScore = scores.get(b) - scores.get(a);
                return byScore != 0 ? byScore
                        : TaskTypes.ORDER.indexOf(a) - TaskTypes.ORDER.indexOf(b);
            }
        });
        int seq = 2;
        for (String type : auxTypes) {
            routes.add(new Route(type, TaskTypes.nameOf(type), "aux", seq, words.get(type),
                    scores.get(type), "触发词命中：" + join(words.get(type))));
            seq++;
        }
        return routes;
    }

    private List<String> matchWords(String taskType, String lowerText) {
        List<String> matched = new ArrayList<String>();
        for (String word : triggerWords(taskType)) {
            String w = word.toLowerCase();
            if (!w.isEmpty() && lowerText.contains(w)) {
                matched.add(word);
            }
        }
        return matched;
    }

    private static String join(List<String> words) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            if (i > 0) {
                sb.append("、");
            }
            sb.append(words.get(i));
        }
        return sb.toString();
    }

    // ---------------- 工作台配置读取（带默认兜底） ----------------

    private WorkbenchEntryConfig latestConfig(String taskType) {
        return configRepo.findTopByTaskTypeOrderByVersionDesc(taskType).orElse(null);
    }

    private List<String> splitLines(String text) {
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

    public List<String> triggerWords(String taskType) {
        WorkbenchEntryConfig cfg = latestConfig(taskType);
        if (cfg != null && cfg.getTriggerWords() != null && !cfg.getTriggerWords().trim().isEmpty()) {
            return splitLines(cfg.getTriggerWords());
        }
        return TaskTypes.defaultTriggers(taskType);
    }

    public List<String> requiredFields(String taskType) {
        WorkbenchEntryConfig cfg = latestConfig(taskType);
        if (cfg != null && cfg.getRequiredFields() != null && !cfg.getRequiredFields().trim().isEmpty()) {
            return splitLines(cfg.getRequiredFields());
        }
        return TaskTypes.defaultRequiredFields(taskType);
    }

    public List<String> workflowSteps(String taskType) {
        WorkbenchEntryConfig cfg = latestConfig(taskType);
        if (cfg != null && cfg.getWorkflowSteps() != null && !cfg.getWorkflowSteps().trim().isEmpty()) {
            return splitLines(cfg.getWorkflowSteps());
        }
        return TaskTypes.defaultWorkflowSteps(taskType);
    }

    public List<String> kbMappings(String taskType) {
        WorkbenchEntryConfig cfg = latestConfig(taskType);
        if (cfg != null && cfg.getKbMappings() != null && !cfg.getKbMappings().trim().isEmpty()) {
            return splitLines(cfg.getKbMappings());
        }
        return TaskTypes.defaultKbMappings(taskType);
    }

    public Map<String, String> entryNames() {
        Map<String, String> names = new LinkedHashMap<String, String>();
        for (WorkbenchEntry entry : entryRepo.findAllByOrderByIdAsc()) {
            names.put(entry.getTaskType(), entry.getName());
        }
        return names;
    }
}
