package com.testpilot.service;

import com.testpilot.common.BizException;
import com.testpilot.entity.SysModule;
import com.testpilot.entity.TestCase;
import com.testpilot.engine.AnalysisContext;
import com.testpilot.engine.RuleBasedAnalyzer;
import com.testpilot.engine.TaskTypes;
import com.testpilot.repository.SysModuleRepository;
import com.testpilot.repository.TestCaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用例库：模块树、CRUD、复制、编辑升版、智能生成草稿。
 */
@Service
public class CaseService {

    @Autowired
    private TestCaseRepository caseRepo;
    @Autowired
    private SysModuleRepository moduleRepo;
    @Autowired
    private RuleBasedAnalyzer ruleAnalyzer;
    @Autowired
    private AuditService auditService;

    public Map<String, Object> list(String keyword, String moduleName, String submoduleName,
                                    String priority, String status, int page, int size) {
        List<TestCase> all = caseRepo.findAllByOrderByCreatedDateDescIdDesc();
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        List<TestCase> filtered = new ArrayList<TestCase>();
        for (TestCase c : all) {
            if (!kw.isEmpty() && !contains(c.getTitle(), kw) && !contains(c.getCaseNo(), kw)
                    && !contains(c.getPreconditions(), kw) && !contains(c.getSteps(), kw)) {
                continue;
            }
            if (moduleName != null && !moduleName.isEmpty() && !moduleName.equals(c.getModuleName())) {
                continue;
            }
            if (submoduleName != null && !submoduleName.isEmpty()
                    && !submoduleName.equals(c.getSubmoduleName())) {
                continue;
            }
            if (priority != null && !priority.isEmpty() && !priority.equals(c.getPriority())) {
                continue;
            }
            if (status != null && !status.isEmpty() && !status.equals(c.getStatus())) {
                continue;
            }
            filtered.add(c);
        }
        int total = filtered.size();
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(total, from + size);
        List<TestCase> pageList = from < to ? filtered.subList(from, to) : new ArrayList<TestCase>();

        int p0 = 0;
        for (TestCase c : filtered) {
            if ("P0".equals(c.getPriority())) {
                p0++;
            }
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("total", total);
        result.put("p0Count", p0);
        result.put("page", page);
        result.put("size", size);
        result.put("list", pageList);
        return result;
    }

    /** 模块树（含用例计数） */
    public List<Map<String, Object>> tree() {
        Map<String, Map<String, Object>> projectMap = new LinkedHashMap<String, Map<String, Object>>();
        for (TestCase c : caseRepo.findAllByOrderByCreatedDateDescIdDesc()) {
            String project = c.getProjectName() == null ? "未分组" : c.getProjectName();
            String module = c.getModuleName() == null ? "未分组" : c.getModuleName();
            String sub = c.getSubmoduleName() == null ? "未分组" : c.getSubmoduleName();

            Map<String, Object> pNode = projectMap.get(project);
            if (pNode == null) {
                pNode = new LinkedHashMap<String, Object>();
                pNode.put("name", project);
                pNode.put("count", 0);
                pNode.put("modules", new ArrayList<Map<String, Object>>());
                projectMap.put(project, pNode);
            }
            List<Map<String, Object>> modules = (List<Map<String, Object>>) pNode.get("modules");
            Map<String, Object> mNode = findByName(modules, module);
            if (mNode == null) {
                mNode = new LinkedHashMap<String, Object>();
                mNode.put("name", module);
                mNode.put("count", 0);
                mNode.put("submodules", new ArrayList<Map<String, Object>>());
                modules.add(mNode);
            }
            List<Map<String, Object>> subs = (List<Map<String, Object>>) mNode.get("submodules");
            Map<String, Object> sNode = findByName(subs, sub);
            if (sNode == null) {
                sNode = new LinkedHashMap<String, Object>();
                sNode.put("name", sub);
                sNode.put("count", 0);
                subs.add(sNode);
            }
            sNode.put("count", ((Integer) sNode.get("count")) + 1);
            mNode.put("count", ((Integer) mNode.get("count")) + 1);
            pNode.put("count", ((Integer) pNode.get("count")) + 1);
        }
        return new ArrayList<Map<String, Object>>(projectMap.values());
    }

    private Map<String, Object> findByName(List<Map<String, Object>> list, String name) {
        for (Map<String, Object> m : list) {
            if (name.equals(m.get("name"))) {
                return m;
            }
        }
        return null;
    }

    /** 启用的三级模块（下拉数据源） */
    public List<Map<String, String>> modules() {
        List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        for (SysModule m : moduleRepo.findByEnabledTrue()) {
            Map<String, String> item = new LinkedHashMap<String, String>();
            item.put("project", m.getProjectName());
            item.put("module", m.getModuleName());
            item.put("submodule", m.getSubmoduleName());
            result.add(item);
        }
        return result;
    }

    public TestCase create(Map<String, Object> body) {
        String title = str(body.get("title"));
        if (title.isEmpty()) {
            throw new BizException("用例标题不能为空");
        }
        TestCase c = new TestCase();
        c.setCaseNo(nextCaseNo());
        applyFields(c, body);
        c.setStatus("未执行");
        c.setVersion(1);
        c.setCreatedDate(LocalDate.now());
        TestCase saved = caseRepo.save(c);
        auditService.record("case", saved.getCaseNo(), "新增用例",
                saved.getTitle() + " · " + saved.getPriority());
        return saved;
    }

    public TestCase detail(Long id) {
        return caseRepo.findById(id)
                .orElseThrow(() -> new BizException("用例不存在：" + id));
    }

    /** 编辑并升版 */
    public TestCase update(Long id, Map<String, Object> body) {
        TestCase c = detail(id);
        applyFields(c, body);
        c.setVersion(c.getVersion() + 1);
        TestCase saved = caseRepo.save(c);
        auditService.record("case", saved.getCaseNo(), "编辑升版",
                "v" + saved.getVersion() + " · " + saved.getTitle());
        return saved;
    }

    /** 复制用例 */
    public TestCase copy(Long id) {
        TestCase src = detail(id);
        TestCase c = new TestCase();
        c.setCaseNo(nextCaseNo());
        c.setTitle(src.getTitle() + "（副本）");
        c.setProjectName(src.getProjectName());
        c.setModuleName(src.getModuleName());
        c.setSubmoduleName(src.getSubmoduleName());
        c.setPriority(src.getPriority());
        c.setType(src.getType());
        c.setPreconditions(src.getPreconditions());
        c.setSteps(src.getSteps());
        c.setExpected(src.getExpected());
        c.setTestData(src.getTestData());
        c.setStatus("未执行");
        c.setVersion(1);
        c.setLinkedBugNo(src.getLinkedBugNo());
        c.setCreatedDate(LocalDate.now());
        TestCase saved = caseRepo.save(c);
        auditService.record("case", saved.getCaseNo(), "复制用例",
                "来自 " + src.getCaseNo() + " · " + saved.getTitle());
        return saved;
    }

    /** 智能生成用例草稿（不落库，逐条确认后保存） */
    public List<Map<String, Object>> generateDrafts(Map<String, Object> body) {
        String context = str(body.get("context"));
        if (context.isEmpty()) {
            throw new BizException("请粘贴需求或问题上下文");
        }
        AnalysisContext ctx = new AnalysisContext();
        ctx.setInputText(context);
        ctx.setProjectName(str(body.get("projectName")));
        ctx.setModuleName(str(body.get("moduleName")));
        ctx.setSubmoduleName(str(body.get("submoduleName")));
        ctx.setRisk("P1");
        Map<String, Object> output = ruleAnalyzer.analyze(TaskTypes.TESTCASE_GEN, ctx);
        List<Map<String, Object>> cases =
                (List<Map<String, Object>>) output.get("cases");
        return cases == null ? new ArrayList<Map<String, Object>>() : cases;
    }

    private void applyFields(TestCase c, Map<String, Object> body) {
        c.setTitle(str(body.get("title")));
        c.setProjectName(str(body.get("projectName")));
        c.setModuleName(str(body.get("moduleName")));
        c.setSubmoduleName(str(body.get("submoduleName")));
        c.setPriority(body.get("priority") == null ? "P1" : str(body.get("priority")));
        c.setType(body.get("type") == null ? "功能" : str(body.get("type")));
        c.setPreconditions(str(body.get("preconditions")));
        c.setSteps(str(body.get("steps")));
        c.setExpected(str(body.get("expected")));
        c.setTestData(str(body.get("testData")));
        c.setLinkedBugNo(str(body.get("linkedBugNo")));
    }

    private String nextCaseNo() {
        long max = caseRepo.findAll().stream().mapToLong(TestCase::getId).max().orElse(0L);
        return "TC-" + String.format("%04d", max + 1);
    }

    private static boolean contains(String text, String kw) {
        return text != null && text.toLowerCase().contains(kw);
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }
}
