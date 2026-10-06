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
        return kbRepo.save(kb);
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

    /** 审核：通过 / 驳回 */
    public KbItem review(Long id, String action, String note) {
        KbItem kb = kbRepo.findById(id)
                .orElseThrow(() -> new BizException("知识不存在：" + id));
        if ("approve".equals(action)) {
            kb.setStatus("已发布");
        } else if ("reject".equals(action)) {
            kb.setStatus("已驳回");
        } else {
            throw new BizException("无效的审核动作：" + action);
        }
        kb.setReviewNote(note == null ? "" : note);
        return kbRepo.save(kb);
    }

    /** 撤销入库：被引用时阻止 */
    public KbItem revoke(Long id) {
        KbItem kb = kbRepo.findById(id)
                .orElseThrow(() -> new BizException("知识不存在：" + id));
        if (kb.getRefCount() != null && kb.getRefCount() > 0) {
            throw new BizException("该知识已被引用 " + kb.getRefCount() + " 次，撤销将被阻止");
        }
        kb.setStatus("已撤销");
        return kbRepo.save(kb);
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
