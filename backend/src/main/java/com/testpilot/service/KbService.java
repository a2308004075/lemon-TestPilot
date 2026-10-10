package com.testpilot.service;

import com.testpilot.common.BizException;
import com.testpilot.entity.KbItem;
import com.testpilot.entity.RunTaskSnapshot;
import com.testpilot.entity.TaskRecord;
import com.testpilot.engine.TaskTypes;
import com.testpilot.repository.KbItemRepository;
import com.testpilot.repository.RunTaskSnapshotRepository;
import com.testpilot.repository.TaskRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库：七分类知识条目、审核、版本、引用计数（撤销阻止）、已入库经验。
 */
@Service
public class KbService {

    @Autowired
    private KbItemRepository kbRepo;
    @Autowired
    private TaskRecordRepository taskRepo;
    @Autowired
    private RunTaskSnapshotRepository snapshotRepo;
    @Autowired
    private AuditService auditService;

    public Map<String, Object> list(String keyword, String category, String status, String moduleName) {
        List<KbItem> all = kbRepo.findAllByOrderByIdAsc();
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        List<KbItem> filtered = new ArrayList<KbItem>();
        for (KbItem kb : all) {
            if (!kw.isEmpty() && !contains(kb.getTitle(), kw) && !contains(kb.getBody(), kw)
                    && !contains(kb.getCategory(), kw)) {
                continue;
            }
            if (category != null && !category.isEmpty() && !category.equals(kb.getCategory())) {
                continue;
            }
            if (status != null && !status.isEmpty() && !status.equals(kb.getStatus())) {
                continue;
            }
            if (moduleName != null && !moduleName.isEmpty() && !moduleName.equals(kb.getModuleName())) {
                continue;
            }
            filtered.add(kb);
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("total", filtered.size());
        result.put("list", filtered);
        return result;
    }

    /** 统计卡：有效知识 / 待审核 / 分类覆盖 / 知识总量 */
    public Map<String, Object> stats() {
        List<KbItem> all = kbRepo.findAllByOrderByIdAsc();
        long effective = 0;
        long pending = 0;
        int covered = 0;
        for (String category : TaskTypes.ALL_CATEGORIES) {
            for (KbItem kb : all) {
                if (category.equals(kb.getCategory())) {
                    covered++;
                    break;
                }
            }
        }
        for (KbItem kb : all) {
            if ("已发布".equals(kb.getStatus())) {
                effective++;
            } else if ("待审核".equals(kb.getStatus())) {
                pending++;
            }
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("effective", effective);
        result.put("pendingReview", pending);
        result.put("categoryCoverage", covered + "/" + TaskTypes.ALL_CATEGORIES.size());
        result.put("total", all.size());
        return result;
    }

    public List<String> categories() {
        return TaskTypes.ALL_CATEGORIES;
    }

    /** 已入库经验：来源于分析任务的知识 */
    public List<KbItem> taskSourced(String category) {
        List<KbItem> result = new ArrayList<KbItem>();
        for (KbItem kb : kbRepo.findTaskSourced()) {
            if (category == null || category.isEmpty() || category.equals(kb.getCategory())) {
                result.add(kb);
            }
        }
        return result;
    }

    public Map<String, Object> detail(Long id) {
        KbItem kb = kbRepo.findById(id)
                .orElseThrow(() -> new BizException("知识不存在：" + id));
        List<String> related = new ArrayList<String>();
        // 关联影响：引用该知识的任务与执行快照（真实检索，不编造）
        for (TaskRecord task : taskRepo.findAllByOrderByUpdatedAtDescIdDesc()) {
            if (task.getOutputJson() != null && task.getOutputJson().contains(kb.getKbNo())) {
                related.add(TaskTypes.nameOf(task.getTaskType()) + "引用该知识：task / " + task.getTaskNo());
            }
        }
        for (RunTaskSnapshot s : snapshotRepo.findAll()) {
            if (s.getOutputJson() != null && s.getOutputJson().contains(kb.getKbNo())) {
                related.add("执行快照引用该知识（" + s.getTaskName() + "）");
            }
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("kb", kb);
        result.put("related", related);
        return result;
    }

    /** 新增知识（保存并提交审核） */
    public KbItem create(Map<String, Object> body) {
        String title = str(body.get("title"));
        if (title.isEmpty()) {
            throw new BizException("知识标题不能为空");
        }
        String kbBody = str(body.get("body"));
        if (kbBody.isEmpty()) {
            throw new BizException("知识正文不能为空");
        }
        KbItem kb = new KbItem();
        kb.setKbNo(nextKbNo());
        kb.setTitle(title);
        kb.setCategory(strOr(body.get("category"), "业务规则库"));
        kb.setProjectName(str(body.get("projectName")));
        kb.setModuleName(str(body.get("moduleName")));
        kb.setSubmoduleName(str(body.get("submoduleName")));
        kb.setRisk(strOr(body.get("risk"), "P1"));
        kb.setVersion(1);
        kb.setStatus("待审核");
        kb.setBody(kbBody);
        kb.setSourceTask("人工维护");
        kb.setReviewNote("");
        kb.setRefCount(0);
        KbItem saved = kbRepo.save(kb);
        auditService.record("kb", saved.getKbNo(), "新增知识",
                saved.getTitle() + " · " + saved.getCategory());
        return saved;
    }

    /** 分析任务结论 → 知识条目（人工复核通过后沉淀经验，状态=待审核） */
    public KbItem createFromTask(TaskRecord task) {
        KbItem kb = new KbItem();
        kb.setKbNo(nextKbNo());
        kb.setTitle(task.getTitle());
        kb.setCategory(categoryOf(task.getTaskType()));
        kb.setProjectName(orDefault(task.getProjectName()));
        kb.setModuleName(orDefault(task.getModuleName()));
        kb.setSubmoduleName(orDefault(task.getSubmoduleName()));
        kb.setRisk(task.getRisk() == null ? "P1" : task.getRisk());
        kb.setVersion(1);
        kb.setStatus("待审核");
        kb.setBody(buildTaskBody(task));
        kb.setSourceTask(task.getTaskNo());
        kb.setReviewNote("");
        kb.setRefCount(0);
        return kbRepo.save(kb);
    }

    /** 任务类型 → 知识分类（仅三类专项分析任务可入库，与详情抽屉入口一致） */
    public static String categoryOf(String taskType) {
        if (TaskTypes.BUG_ANALYSIS.equals(taskType)) {
            return "历史 Bug 库";
        }
        if (TaskTypes.LOG_TRIAGE.equals(taskType)) {
            return "日志规律库";
        }
        if (TaskTypes.SQL_ANALYSIS.equals(taskType)) {
            return "SQL 经验库";
        }
        throw new BizException("该任务类型不支持知识入库：" + TaskTypes.nameOf(taskType));
    }

    private static String buildTaskBody(TaskRecord task) {
        StringBuilder sb = new StringBuilder();
        sb.append("【来源】").append(task.getTaskNo()).append(" · ")
                .append(TaskTypes.nameOf(task.getTaskType())).append("（人工复核通过）\n");
        sb.append("【问题上下文】\n").append(task.getContext() == null ? "" : task.getContext().trim())
                .append("\n");
        sb.append("【分析结论】\n");
        String output = task.getOutputJson() == null ? "" : task.getOutputJson().trim();
        sb.append(output.isEmpty() ? "（无结构化输出）" :
                output.length() > 1500 ? output.substring(0, 1500) + "…" : output);
        return sb.toString();
    }

    private static String orDefault(String value) {
        return value == null ? "" : value;
    }

    /** 编辑内容（升版） */
    public KbItem update(Long id, Map<String, Object> body) {
        KbItem kb = kbRepo.findById(id)
                .orElseThrow(() -> new BizException("知识不存在：" + id));
        if (body.containsKey("title") && !str(body.get("title")).isEmpty()) {
            kb.setTitle(str(body.get("title")));
        }
        if (body.containsKey("category") && !str(body.get("category")).isEmpty()) {
            kb.setCategory(str(body.get("category")));
        }
        if (body.containsKey("risk") && !str(body.get("risk")).isEmpty()) {
            kb.setRisk(str(body.get("risk")));
        }
        if (body.containsKey("body") && !str(body.get("body")).isEmpty()) {
            kb.setBody(str(body.get("body")));
        }
        kb.setVersion(kb.getVersion() + 1);
        return kbRepo.save(kb);
    }

    /** 审核：通过 / 驳回（同步来源任务的知识入库状态） */
    public KbItem review(Long id, String action, String note) {
        KbItem kb = kbRepo.findById(id)
                .orElseThrow(() -> new BizException("知识不存在：" + id));
        if ("approve".equals(action)) {
            kb.setStatus("已发布");
            syncTaskKnowledgeStatus(kb.getSourceTask(), "已入库");
        } else if ("reject".equals(action)) {
            kb.setStatus("已驳回");
            syncTaskKnowledgeStatus(kb.getSourceTask(), "未入库");
        } else {
            throw new BizException("无效的审核动作：" + action);
        }
        kb.setReviewNote(note == null ? "" : note);
        KbItem saved = kbRepo.save(kb);
        auditService.record("kb", saved.getKbNo(),
                "approve".equals(action) ? "审核通过" : "审核驳回",
                saved.getTitle() + (note == null || note.isEmpty() ? "" : " · " + note));
        return saved;
    }

    /** 撤销入库：被引用时阻止（同步来源任务回未入库） */
    public KbItem revoke(Long id) {
        KbItem kb = kbRepo.findById(id)
                .orElseThrow(() -> new BizException("知识不存在：" + id));
        if (kb.getRefCount() != null && kb.getRefCount() > 0) {
            throw new BizException("该知识已被引用 " + kb.getRefCount() + " 次，撤销将被阻止");
        }
        kb.setStatus("已撤销");
        syncTaskKnowledgeStatus(kb.getSourceTask(), "未入库");
        KbItem saved = kbRepo.save(kb);
        auditService.record("kb", saved.getKbNo(), "撤销入库", saved.getTitle());
        return saved;
    }

    /** 知识审核结果反向同步来源任务（人工维护的知识不处理） */
    private void syncTaskKnowledgeStatus(String sourceTask, String status) {
        if (sourceTask == null || sourceTask.isEmpty() || "人工维护".equals(sourceTask)) {
            return;
        }
        TaskRecord task = taskRepo.findByTaskNo(sourceTask).orElse(null);
        if (task != null) {
            task.setKnowledgeStatus(status);
            taskRepo.save(task);
        }
    }

    private String nextKbNo() {
        long max = kbRepo.findAll().stream().mapToLong(KbItem::getId).max().orElse(0L);
        return "kb_" + String.format("%03d", max + 1);
    }

    private static boolean contains(String text, String kw) {
        return text != null && text.toLowerCase().contains(kw);
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static String strOr(Object o, String fallback) {
        String v = str(o);
        return v.isEmpty() ? fallback : v;
    }
}
