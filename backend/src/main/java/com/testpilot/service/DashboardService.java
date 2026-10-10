package com.testpilot.service;

import com.testpilot.engine.TaskTypes;
import com.testpilot.entity.RunRecord;
import com.testpilot.entity.TaskRecord;
import com.testpilot.repository.KbItemRepository;
import com.testpilot.repository.RunRecordRepository;
import com.testpilot.repository.TaskRecordRepository;
import com.testpilot.repository.TestCaseRepository;
import com.testpilot.repository.TestReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作台首页：状态卡、数据资产、最近任务。
 */
@Service
public class DashboardService {

    @Autowired
    private RunRecordRepository runRepo;
    @Autowired
    private TaskRecordRepository taskRepo;
    @Autowired
    private TestCaseRepository caseRepo;
    @Autowired
    private KbItemRepository kbRepo;
    @Autowired
    private TestReportRepository reportRepo;

    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();

        // 6 状态卡
        List<Map<String, Object>> statusCards = new ArrayList<Map<String, Object>>();
        statusCards.add(card("runCount", "执行记录", runRepo.count(), "次"));
        statusCards.add(card("pendingReview",
                "待复核", runRepo.countByReviewStatus("待复核") + taskRepo.countByReviewStatus("待复核"), "条"));
        statusCards.add(card("pendingSupplement",
                "待补充", runRepo.countByStatus("待补充") + taskRepo.countByStatus("待补充"), "条"));
        statusCards.add(card("taskCount", "分析任务", taskRepo.count(), "个"));
        statusCards.add(card("kbEffective", "有效知识", kbRepo.countByStatus("已发布"), "条"));
        statusCards.add(card("p0Cases", "P0 用例", caseRepo.countByPriority("P0"), "条"));
        result.put("statusCards", statusCards);

        // 数据资产 4 卡
        List<Map<String, Object>> assets = new ArrayList<Map<String, Object>>();
        assets.add(card("cases", "测试用例", caseRepo.count(), "条"));
        assets.add(card("tasks", "任务记录", taskRepo.count(), "条"));
        assets.add(card("kb", "知识条目", kbRepo.count(), "条"));
        assets.add(card("reports", "测试报告", reportRepo.count(), "份"));
        result.put("assets", assets);

        // 最近任务：分析任务 + 执行记录（合并展示）
        List<Map<String, Object>> recentTasks = new ArrayList<Map<String, Object>>();
        for (TaskRecord t : taskRepo.findTop5ByOrderByUpdatedAtDescIdDesc()) {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", t.getId());
            m.put("no", t.getTaskNo());
            m.put("kind", "task");
            m.put("title", t.getTitle());
            m.put("type", t.getTaskType());
            m.put("typeName", TaskTypes.nameOf(t.getTaskType()));
            m.put("status", t.getStatus());
            m.put("reviewStatus", t.getReviewStatus());
            m.put("updatedAt", t.getUpdatedAt());
            recentTasks.add(m);
        }
        result.put("recentTasks", recentTasks);

        List<Map<String, Object>> recentRuns = new ArrayList<Map<String, Object>>();
        for (RunRecord r : runRepo.findTop5ByOrderByUpdatedAtDescIdDesc()) {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", r.getId());
            m.put("no", r.getRunNo());
            m.put("kind", "run");
            m.put("title", r.getTitle());
            m.put("mainTask", r.getMainTask());
            m.put("mainTaskName", TaskTypes.nameOf(r.getMainTask()));
            m.put("status", r.getStatus());
            m.put("reviewStatus", r.getReviewStatus());
            m.put("routeCount", r.getRouteCount());
            m.put("risk", r.getRisk());
            m.put("updatedAt", r.getUpdatedAt());
            recentRuns.add(m);
        }
        result.put("recentRuns", recentRuns);

        return result;
    }

    private static Map<String, Object> card(String key, String name, long value, String unit) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("key", key);
        m.put("name", name);
        m.put("value", value);
        m.put("unit", unit);
        return m;
    }
}
