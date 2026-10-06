package com.testpilot.service;

import com.testpilot.common.BizException;
import com.testpilot.common.JsonUtil;
import com.testpilot.entity.RunRecord;
import com.testpilot.entity.RunStep;
import com.testpilot.entity.RunTaskSnapshot;
import com.testpilot.repository.RunRecordRepository;
import com.testpilot.repository.RunStepRepository;
import com.testpilot.repository.RunTaskSnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 执行记录：列表、详情（路由快照 + 步骤）、人工复核。
 */
@Service
public class RunService {

    @Autowired
    private RunRecordRepository runRepo;
    @Autowired
    private RunTaskSnapshotRepository snapshotRepo;
    @Autowired
    private RunStepRepository stepRepo;

    public Map<String, Object> list(String keyword, String status, int page, int size) {
        List<RunRecord> all = runRepo.findAllByOrderByUpdatedAtDescIdDesc();
        List<RunRecord> filtered = new ArrayList<RunRecord>();
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        for (RunRecord r : all) {
            if (!kw.isEmpty() && r.getTitle() != null && !r.getTitle().toLowerCase().contains(kw)
                    && r.getInputText() != null && !r.getInputText().toLowerCase().contains(kw)) {
                continue;
            }
            if (status != null && !status.isEmpty() && !status.equals(r.getStatus())) {
                continue;
            }
            filtered.add(r);
        }
        int total = filtered.size();
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(total, from + size);
        List<RunRecord> pageList = from < to ? filtered.subList(from, to) : new ArrayList<RunRecord>();

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("list", pageList);
        return result;
    }

    public Map<String, Object> detail(Long id) {
        RunRecord run = runRepo.findById(id)
                .orElseThrow(() -> new BizException("执行记录不存在：" + id));
        List<Map<String, Object>> snapshots = new ArrayList<Map<String, Object>>();
        for (RunTaskSnapshot s : snapshotRepo.findByRunIdOrderBySeqAsc(id)) {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("taskType", s.getTaskType());
            m.put("taskName", s.getTaskName());
            m.put("role", s.getRole());
            m.put("seq", s.getSeq());
            m.put("completeness", s.getCompleteness());
            m.put("missing", JsonUtil.readStringList(s.getMissingJson()));
            m.put("output", JsonUtil.readMap(s.getOutputJson()));
            snapshots.add(m);
        }
        List<RunStep> steps = stepRepo.findByRunIdOrderBySeqAsc(id);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("run", run);
        result.put("snapshots", snapshots);
        result.put("steps", steps);
        return result;
    }

    /** 人工复核：确认通过 / 驳回补充 */
    public RunRecord review(Long id, String action, String note) {
        RunRecord run = runRepo.findById(id)
                .orElseThrow(() -> new BizException("执行记录不存在：" + id));
        if ("approve".equals(action)) {
            run.setReviewStatus("通过");
        } else if ("reject".equals(action)) {
            run.setReviewStatus("驳回补充");
            run.setStatus("待补充");
        } else {
            throw new BizException("无效的复核动作：" + action);
        }
        return runRepo.save(run);
    }
}
