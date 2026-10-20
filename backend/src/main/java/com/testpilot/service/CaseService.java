package com.testpilot.service;

import com.testpilot.common.BizException;
import com.testpilot.entity.CaseExecutionBatch;
import com.testpilot.entity.CaseExecutionItem;
import com.testpilot.entity.SysModule;
import com.testpilot.entity.TestCase;
import com.testpilot.engine.AnalysisContext;
import com.testpilot.engine.RuleBasedAnalyzer;
import com.testpilot.engine.TaskTypes;
import com.testpilot.repository.CaseExecutionBatchRepository;
import com.testpilot.repository.CaseExecutionItemRepository;
import com.testpilot.repository.SysModuleRepository;
import com.testpilot.repository.TestCaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    @Autowired
    private CaseExecutionBatchRepository batchRepo;
    @Autowired
    private CaseExecutionItemRepository itemRepo;

    /** 执行条目的可登记结果（待执行表示撤销登记） */
    private static final List<String> ITEM_STATUSES =
            Arrays.asList("待执行", "通过", "失败", "阻塞", "跳过");

    private static final DateTimeFormatter BATCH_TITLE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 用例编号：TC + 生成时刻时间戳（yyyyMMddHHmmss），如 TC-20261014143135 */
    private static final DateTimeFormatter CASE_NO_FMT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 执行批次号：RT + 创建时刻时间戳（yyyyMMddHHmmss），如 RT-20261014143536 */
    private static final DateTimeFormatter BATCH_NO_FMT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

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
        c.setStatus("使用");
        if (c.getVersion() == null || c.getVersion().isEmpty()) {
            c.setVersion("v1");
        }
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

    /** 编辑用例：版本由用户手动维护，未传则保留原值 */
    public TestCase update(Long id, Map<String, Object> body) {
        TestCase c = detail(id);
        applyFields(c, body);
        TestCase saved = caseRepo.save(c);
        auditService.record("case", saved.getCaseNo(), "编辑用例",
                saved.getVersion() + " · " + saved.getTitle());
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
        c.setStatus("使用");
        c.setVersion("v1");
        c.setLinkedBugNo(src.getLinkedBugNo());
        c.setCreatedDate(LocalDate.now());
        TestCase saved = caseRepo.save(c);
        auditService.record("case", saved.getCaseNo(), "复制用例",
                "来自 " + src.getCaseNo() + " · " + saved.getTitle());
        return saved;
    }

    /** 删除用例：仅废弃状态可删，物理删除并写审计（执行条目为快照引用，不受影响） */
    @Transactional
    public void delete(Long id) {
        TestCase c = detail(id);
        if (!"废弃".equals(c.getStatus())) {
            throw new BizException("仅废弃状态的用例可删除，请先编辑状态为废弃");
        }
        caseRepo.delete(c);
        auditService.record("case", c.getCaseNo(), "删除用例",
                c.getTitle() + " · " + c.getPriority());
    }

    /** XLSX 批量导入：标题+项目/模块/子模块四元组查重，重复或无效行跳过，汇总写一条审计 */
    @Transactional
    public Map<String, Object> importCases(Map<String, Object> body) {
        Object raw = body.get("cases");
        List<Object> rows = raw instanceof List ? (List<Object>) raw : new ArrayList<Object>();
        if (rows.isEmpty()) {
            throw new BizException("没有可导入的用例");
        }
        if (rows.size() > 500) {
            throw new BizException("单次最多导入 500 条用例");
        }
        // 全量载入查重键与已有编号，内存判重（前端预览已过滤无效行，此处为防御性兜底）
        Set<String> dupKeys = new HashSet<String>();
        Set<String> caseNos = new HashSet<String>();
        for (TestCase c : caseRepo.findAll()) {
            dupKeys.add(dupKey(c.getTitle(), c.getProjectName(), c.getModuleName(), c.getSubmoduleName()));
            caseNos.add(c.getCaseNo());
        }
        // 编号时间游标逐秒顺延：批量插入下避免 nextCaseNo 的同秒 sleep 等待
        LocalDateTime slot = LocalDateTime.now();
        int imported = 0;
        int skipped = 0;
        for (Object o : rows) {
            if (!(o instanceof Map)) {
                skipped++;
                continue;
            }
            Map<String, Object> row = (Map<String, Object>) o;
            String title = str(row.get("title"));
            String moduleName = str(row.get("moduleName"));
            if (title.isEmpty() || moduleName.isEmpty()) {
                skipped++;
                continue;
            }
            if (!dupKeys.add(dupKey(title, str(row.get("projectName")), moduleName,
                    str(row.get("submoduleName"))))) {
                skipped++;
                continue;
            }
            String no;
            do {
                no = "TC-" + slot.format(CASE_NO_FMT);
                slot = slot.plusSeconds(1);
            } while (caseNos.contains(no));
            caseNos.add(no);
            TestCase c = new TestCase();
            c.setCaseNo(no);
            applyFields(c, row);
            c.setStatus("使用");
            c.setVersion("v1");
            c.setCreatedDate(LocalDate.now());
            caseRepo.save(c);
            imported++;
        }
        auditService.record("case", "XLSX-IMPORT", "批量导入用例",
                "导入 " + imported + " 条，跳过 " + skipped + " 条");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("total", rows.size());
        result.put("imported", imported);
        result.put("skipped", skipped);
        return result;
    }

    /** 查重键：标题 + 项目/模块/子模块四元组 */
    private static String dupKey(String title, String project, String module, String sub) {
        return title + "|" + project + "|" + module + "|" + sub;
    }

    // ---------- 用例执行批次（6.11 W3） ----------

    /** 勾选用例创建执行批次：快照用例编号/标题/优先级，条目初始"待执行" */
    @Transactional
    public Map<String, Object> createBatch(Map<String, Object> body) {
        Object rawIds = body.get("caseIds");
        List<Object> idList = rawIds instanceof List ? (List<Object>) rawIds : new ArrayList<Object>();
        if (idList.isEmpty()) {
            throw new BizException("请先勾选用例");
        }
        if (idList.size() > 100) {
            throw new BizException("单批最多 100 条用例");
        }
        // 按勾选顺序去重，并滤除不存在的用例
        List<TestCase> cases = new ArrayList<TestCase>();
        Set<Long> seen = new LinkedHashSet<Long>();
        for (Object o : idList) {
            Long cid = toLong(o);
            if (cid == null || !seen.add(cid)) {
                continue;
            }
            caseRepo.findById(cid).ifPresent(cases::add);
        }
        if (cases.isEmpty()) {
            throw new BizException("勾选的用例不存在或已失效");
        }

        String customTitle = str(body.get("title"));
        CaseExecutionBatch batch = new CaseExecutionBatch();
        batch.setBatchNo(nextBatchNo());
        batch.setTitle(customTitle.isEmpty()
                ? "用例执行批次 " + LocalDateTime.now().format(BATCH_TITLE_FMT)
                : customTitle);
        batch.setStatus("执行中");
        batch.setTotal(cases.size());
        batch.setPassed(0);
        batch.setFailed(0);
        batch.setBlocked(0);
        batch.setSkipped(0);
        CaseExecutionBatch saved = batchRepo.save(batch);

        int seq = 0;
        for (TestCase c : cases) {
            CaseExecutionItem item = new CaseExecutionItem();
            item.setBatchId(saved.getId());
            item.setSeqOrder(seq++);
            item.setCaseId(c.getId());
            item.setCaseNoSnapshot(c.getCaseNo());
            item.setTitleSnapshot(c.getTitle());
            item.setPrioritySnapshot(c.getPriority());
            item.setStatus("待执行");
            item.setNote("");
            itemRepo.save(item);
        }
        auditService.record("case_execution_batch", "BATCH-" + saved.getId(),
                "创建执行批次", "共 " + cases.size() + " 条用例");
        return batchDetail(saved.getId());
    }

    /** 批次列表（分页 + 进度计数） */
    public Map<String, Object> listBatches(int page, int size) {
        List<CaseExecutionBatch> all = batchRepo.findAllByOrderByIdDesc();
        int total = all.size();
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(total, from + size);
        List<CaseExecutionBatch> pageList = from < to ? all.subList(from, to) : new ArrayList<CaseExecutionBatch>();
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("list", pageList);
        return result;
    }

    /** 批次详情 + 条目（按勾选顺序） */
    public Map<String, Object> batchDetail(Long id) {
        CaseExecutionBatch batch = batchRepo.findById(id)
                .orElseThrow(() -> new BizException("执行批次不存在：" + id));
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("batch", batch);
        result.put("items", itemRepo.findByBatchIdOrderBySeqOrderAsc(id));
        return result;
    }

    /** 逐条登记执行结果：重算批次计数、自动完成、联动用例状态、写审计 */
    @Transactional
    public Map<String, Object> updateBatch(Long id, Map<String, Object> body) {
        CaseExecutionBatch batch = batchRepo.findById(id)
                .orElseThrow(() -> new BizException("执行批次不存在：" + id));
        Long itemId = toLong(body.get("itemId"));
        if (itemId == null) {
            throw new BizException("缺少条目参数 itemId");
        }
        String status = str(body.get("status"));
        if (!ITEM_STATUSES.contains(status)) {
            throw new BizException("非法的执行结果：" + status);
        }
        CaseExecutionItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new BizException("执行条目不存在：" + itemId));
        if (!id.equals(item.getBatchId())) {
            throw new BizException("该条目不属于此批次");
        }
        item.setStatus(status);
        if (body.containsKey("note")) {
            item.setNote(str(body.get("note")));
        }
        item.setExecutedAt("待执行".equals(status) ? null : LocalDateTime.now());
        itemRepo.save(item);

        List<CaseExecutionItem> items = itemRepo.findByBatchIdOrderBySeqOrderAsc(id);
        int passed = 0;
        int failed = 0;
        int blocked = 0;
        int skipped = 0;
        for (CaseExecutionItem it : items) {
            if ("通过".equals(it.getStatus())) {
                passed++;
            } else if ("失败".equals(it.getStatus())) {
                failed++;
            } else if ("阻塞".equals(it.getStatus())) {
                blocked++;
            } else if ("跳过".equals(it.getStatus())) {
                skipped++;
            }
        }
        batch.setPassed(passed);
        batch.setFailed(failed);
        batch.setBlocked(blocked);
        batch.setSkipped(skipped);
        batch.setStatus(passed + failed + blocked + skipped >= items.size() ? "已完成" : "执行中");
        batchRepo.save(batch);

        auditService.record("case_execution_batch", "BATCH-" + id,
                "登记执行结果", item.getCaseNoSnapshot() + " → " + status);
        return batchDetail(id);
    }

    /** 删除执行批次：先删子项条目，再删批次，最后写审计 */
    @Transactional
    public void deleteBatch(Long id) {
        CaseExecutionBatch batch = batchRepo.findById(id)
                .orElseThrow(() -> new BizException("执行批次不存在：" + id));
        itemRepo.deleteByBatchId(id);
        batchRepo.delete(batch);
        auditService.record("case_execution_batch", "BATCH-" + id,
                "删除执行批次", batch.getTitle());
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
        // 版本为用户手动维护的自由文本：仅在传入非空值时应用，否则保持原值（新增时由 create 兜底为 v1）
        String version = str(body.get("version"));
        if (!version.isEmpty()) {
            c.setVersion(version);
        }
    }

    private String nextCaseNo() {
        String no = "TC-" + LocalDateTime.now().format(CASE_NO_FMT);
        // 同一秒内重复创建时顺延等待，避免 uk_case_no 唯一约束冲突
        int guard = 0;
        while (caseRepo.existsByCaseNo(no) && guard < 20) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            no = "TC-" + LocalDateTime.now().format(CASE_NO_FMT);
            guard++;
        }
        return no;
    }

    /** 生成批次号：RT + 创建时刻时间戳（yyyyMMddHHmmss） */
    private String nextBatchNo() {
        String no = "RT-" + LocalDateTime.now().format(BATCH_NO_FMT);
        // 同一秒内重复创建时顺延等待，避免批次号唯一约束冲突
        int guard = 0;
        while (batchRepo.existsByBatchNo(no) && guard < 20) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            no = "RT-" + LocalDateTime.now().format(BATCH_NO_FMT);
            guard++;
        }
        return no;
    }

    private static boolean contains(String text, String kw) {
        return text != null && text.toLowerCase().contains(kw);
    }

    /** 宽松转换前端传入的数字 id（JSON 数字 / 字符串均可） */
    private static Long toLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        try {
            return Long.parseLong(o.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }
}
