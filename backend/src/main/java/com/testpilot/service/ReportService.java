package com.testpilot.service;

import com.testpilot.common.BizException;
import com.testpilot.common.JsonUtil;
import com.testpilot.entity.RegressionItem;
import com.testpilot.entity.RegressionList;
import com.testpilot.entity.TaskRecord;
import com.testpilot.entity.TestCase;
import com.testpilot.entity.TestReport;
import com.testpilot.repository.RegressionItemRepository;
import com.testpilot.repository.RegressionListRepository;
import com.testpilot.repository.TaskRecordRepository;
import com.testpilot.repository.TestCaseRepository;
import com.testpilot.repository.TestReportRepository;
import com.testpilot.engine.TaskTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 测试报告：勾选多来源（分析任务 + 回归清单 + 用例）聚合统一质量结论。
 * AI 不批准上线：结论建议仅供人工复核参考。
 */
@Service
public class ReportService {

    @Autowired
    private TestReportRepository reportRepo;
    @Autowired
    private TaskRecordRepository taskRepo;
    @Autowired
    private RegressionListRepository regListRepo;
    @Autowired
    private RegressionItemRepository regItemRepo;
    @Autowired
    private TestCaseRepository caseRepo;

    public List<TestReport> list() {
        return reportRepo.findAllByOrderByIdDesc();
    }

    public Map<String, Object> detail(Long id) {
        TestReport report = reportRepo.findById(id)
                .orElseThrow(() -> new BizException("报告不存在：" + id));
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("report", report);
        result.put("content", JsonUtil.readMap(report.getContentJson()));
        return result;
    }

    /** 生成统一报告 */
    public Map<String, Object> generate(Map<String, Object> body) {
        String title = str(body.get("title"));
        if (title.isEmpty()) {
            throw new BizException("报告标题不能为空");
        }
        String version = str(body.get("version"));
        if (version.isEmpty()) {
            throw new BizException("发布版本不能为空");
        }

        List<Long> taskIds = longList(body.get("sourceTaskIds"));
        List<Long> regListIds = longList(body.get("regressionListIds"));
        List<Long> caseIds = longList(body.get("caseIds"));

        int pass = 0;
        int fail = 0;
        int block = 0;
        Set<String> modules = new LinkedHashSet<String>();
        List<Map<String, Object>> sources = new ArrayList<Map<String, Object>>();
        List<String> riskNotes = new ArrayList<String>();

        for (Long taskId : taskIds) {
            TaskRecord task = taskRepo.findById(taskId).orElse(null);
            if (task == null) {
                continue;
            }
            Map<String, Object> s = new LinkedHashMap<String, Object>();
            s.put("type", TaskTypes.nameOf(task.getTaskType()));
            s.put("title", task.getTitle());
            s.put("risk", task.getRisk());
            s.put("taskNo", task.getTaskNo());
            sources.add(s);
            if (!task.getModuleName().isEmpty()) {
                modules.add(task.getModuleName());
            }
            if ("驳回补充".equals(task.getReviewStatus())) {
                riskNotes.add("任务「" + task.getTitle() + "」被驳回补充，证据不完整");
            }
        }
        for (Long listId : regListIds) {
            RegressionList list = regListRepo.findById(listId).orElse(null);
            if (list == null) {
                continue;
            }
            Map<String, Object> s = new LinkedHashMap<String, Object>();
            s.put("type", "回归清单");
            s.put("title", list.getTitle());
            s.put("risk", "P1");
            s.put("listId", list.getId());
            sources.add(s);
            for (RegressionItem item : regItemRepo.findByListIdOrderBySeqAsc(listId)) {
                if ("失败".equals(item.getStatus())) {
                    fail++;
                    riskNotes.add("回归项「" + item.getTitle() + "」执行失败");
                } else if ("阻塞".equals(item.getStatus())) {
                    block++;
                    riskNotes.add("回归项「" + item.getTitle() + "」阻塞");
                } else if ("通过".equals(item.getStatus())) {
                    pass++;
                }
            }
        }
        for (Long caseId : caseIds) {
            TestCase c = caseRepo.findById(caseId).orElse(null);
            if (c == null) {
                continue;
            }
            if ("通过".equals(c.getStatus())) {
                pass++;
            } else if ("失败".equals(c.getStatus())) {
                fail++;
                riskNotes.add("用例「" + c.getTitle() + "」执行失败");
            } else if ("阻塞".equals(c.getStatus())) {
                block++;
                riskNotes.add("用例「" + c.getTitle() + "」阻塞");
            }
        }

        String suggestion;
        if (block > 0) {
            suggestion = "不可上线";
        } else if (fail > 0) {
            suggestion = "有条件上线";
        } else {
            suggestion = "可上线";
        }
        String conclusion = body.get("conclusion") == null || str(body.get("conclusion")).isEmpty()
                ? suggestion : str(body.get("conclusion"));

        Map<String, Object> content = new LinkedHashMap<String, Object>();
        content.put("summary", title + "（" + version + "）：汇总 "
                + sources.size() + " 项来源，通过 " + pass + "、失败 " + fail + "、阻塞 " + block + "。");
        content.put("sources", sources);
        content.put("riskNotes", riskNotes);
        content.put("suggestion", suggestion);
        content.put("manualReview", "上线结论需测试人最终确认，AI 不批准上线");

        TestReport report = new TestReport();
        report.setReportNo(nextReportNo());
        report.setTitle(title);
        report.setProjectName(strOr(body.get("projectName"), "电商平台"));
        report.setVersion(version);
        report.setModules(String.join("、", modules));
        report.setConclusion(conclusion);
        report.setPassCount(pass);
        report.setFailCount(fail);
        report.setBlockCount(block);
        report.setReportDate(LocalDate.now());
        report.setContentJson(JsonUtil.write(content));
        report.setSourceTaskIds(joinIds(taskIds));
        reportRepo.save(report);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("report", report);
        result.put("content", content);
        return result;
    }

    private String nextReportNo() {
        long max = reportRepo.findAll().stream().mapToLong(TestReport::getId).max().orElse(0L);
        return "report_" + String.format("%03d", max + 1);
    }

    private static String joinIds(List<Long> ids) {
        StringBuilder sb = new StringBuilder();
        for (Long id : ids) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(id);
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static List<Long> longList(Object value) {
        List<Long> result = new ArrayList<Long>();
        if (value instanceof List) {
            for (Object o : (List<Object>) value) {
                try {
                    result.add(Long.parseLong(o.toString()));
                } catch (NumberFormatException ignore) {
                    // 跳过非法 ID
                }
            }
        }
        return result;
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static String strOr(Object o, String fallback) {
        String v = str(o);
        return v.isEmpty() ? fallback : v;
    }
}
