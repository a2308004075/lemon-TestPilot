package com.testpilot.service;

import com.testpilot.common.BizException;
import com.testpilot.entity.KbItem;
import com.testpilot.entity.RegressionItem;
import com.testpilot.entity.RegressionList;
import com.testpilot.entity.TestCase;
import com.testpilot.repository.KbItemRepository;
import com.testpilot.repository.RegressionItemRepository;
import com.testpilot.repository.RegressionListRepository;
import com.testpilot.repository.TestCaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 回归测试：清单生成（高风险用例 + 历史 Bug + 变更模块）、执行状态流转、进度统计。
 */
@Service
public class RegressionService {

    @Autowired
    private RegressionListRepository listRepo;
    @Autowired
    private RegressionItemRepository itemRepo;
    @Autowired
    private TestCaseRepository caseRepo;
    @Autowired
    private KbItemRepository kbRepo;
    @Autowired
    private AuditService auditService;

    /** 全部清单（含条目与汇总） */
    public List<Map<String, Object>> lists() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (RegressionList list : listRepo.findAllByOrderByIdDesc()) {
            result.add(listDetail(list));
        }
        return result;
    }

    public Map<String, Object> listDetail(Long id) {
        RegressionList list = listRepo.findById(id)
                .orElseThrow(() -> new BizException("回归清单不存在：" + id));
        return listDetail(list);
    }

    private Map<String, Object> listDetail(RegressionList list) {
        List<RegressionItem> items = itemRepo.findByListIdOrderBySeqAsc(list.getId());
        int pass = 0;
        int fail = 0;
        int block = 0;
        int executed = 0;
        for (RegressionItem item : items) {
            if ("通过".equals(item.getStatus())) {
                pass++;
                executed++;
            } else if ("失败".equals(item.getStatus())) {
                fail++;
                executed++;
            } else if ("阻塞".equals(item.getStatus())) {
                block++;
                executed++;
            }
        }
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", list.getId());
        m.put("title", list.getTitle());
        m.put("version", list.getVersion());
        m.put("status", list.getStatus());
        m.put("progress", list.getProgress());
        m.put("createdDate", list.getCreatedDate());
        m.put("updatedAt", list.getUpdatedAt());
        m.put("items", items);
        m.put("summary", Arrays.asList(
                pass + " 通过", fail + " 失败", block + " 阻塞"));
        m.put("passCount", pass);
        m.put("failCount", fail);
        m.put("blockCount", block);
        m.put("executed", executed);
        m.put("total", items.size());
        return m;
    }

    /**
     * 生成回归清单：从高风险用例（P0 或失败/阻塞）、已发布历史 Bug 生成。
     */
    public Map<String, Object> generate(Map<String, Object> body) {
        String title = str(body.get("title"));
        if (title.isEmpty()) {
            throw new BizException("清单标题不能为空");
        }
        String version = str(body.get("version"));

        Set<String> usedTitles = new LinkedHashSet<String>();
        List<RegressionItem> items = new ArrayList<RegressionItem>();

        for (TestCase c : caseRepo.findAllByOrderByCreatedDateDescIdDesc()) {
            boolean highRisk = "P0".equals(c.getPriority())
                    || "失败".equals(c.getStatus()) || "阻塞".equals(c.getStatus());
            if (highRisk && usedTitles.add(c.getTitle())) {
                RegressionItem item = new RegressionItem();
                item.setTitle(c.getTitle());
                item.setPriority(c.getPriority());
                item.setSourceType("case");
                item.setSourceRef(c.getCaseNo());
                item.setModuleLabel(moduleLabel(c.getProjectName(), c.getModuleName(),
                        c.getSubmoduleName()) + " · 关联用例");
                item.setStatus("未执行");
                items.add(item);
            }
        }
        for (KbItem kb : kbRepo.findByStatusAndCategoryOrderByIdAsc("已发布", "历史 Bug 库")) {
            if (usedTitles.add(kb.getTitle())) {
                RegressionItem item = new RegressionItem();
                item.setTitle(kb.getTitle());
                item.setPriority(kb.getRisk());
                item.setSourceType("bug");
                item.setSourceRef(kb.getKbNo());
                item.setModuleLabel(moduleLabel(kb.getProjectName(), kb.getModuleName(),
                        kb.getSubmoduleName()) + " · 关联 Bug");
                item.setStatus("未执行");
                items.add(item);
            }
        }
        // 至少 6 个必回归项
        if (items.size() < 6) {
            String[] base = {"核心链路主流程校验", "异常与失败场景重放", "幂等与重复请求验证",
                    "上下游数据一致性核对", "边界值与极端场景", "历史缺陷回归验证"};
            for (String b : base) {
                if (items.size() >= 6) {
                    break;
                }
                if (usedTitles.add(b)) {
                    RegressionItem item = new RegressionItem();
                    item.setTitle(b);
                    item.setPriority("P1");
                    item.setSourceType("rule");
                    item.setSourceRef("");
                    item.setModuleLabel("通用回归规则");
                    item.setStatus("未执行");
                    items.add(item);
                }
            }
        }

        RegressionList list = new RegressionList();
        list.setTitle(title);
        list.setVersion(version);
        list.setStatus("执行中");
        list.setProgress(0);
        list.setCreatedDate(LocalDate.now());
        listRepo.save(list);

        int seq = 1;
        for (RegressionItem item : items) {
            item.setListId(list.getId());
            item.setSeq(seq++);
            itemRepo.save(item);
        }
        auditService.record("regression", "reg_" + list.getId(), "生成回归清单",
                title + " · " + version + " · " + items.size() + " 项");
        return listDetail(list);
    }

    /** 更新条目状态并重算清单进度 */
    public Map<String, Object> updateItem(Long itemId, String status) {
        if (!Arrays.asList("未执行", "通过", "失败", "阻塞").contains(status)) {
            throw new BizException("无效的执行状态：" + status);
        }
        RegressionItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new BizException("回归项不存在：" + itemId));
        item.setStatus(status);
        itemRepo.save(item);

        List<RegressionItem> items = itemRepo.findByListIdOrderBySeqAsc(item.getListId());
        int executed = 0;
        for (RegressionItem i : items) {
            if (!"未执行".equals(i.getStatus())) {
                executed++;
            }
        }
        RegressionList list = listRepo.findById(item.getListId())
                .orElseThrow(() -> new BizException("回归清单不存在"));
        list.setProgress(items.isEmpty() ? 0 : (int) Math.round(executed * 100.0 / items.size()));
        list.setStatus(executed >= items.size() && !items.isEmpty() ? "已完成" : "执行中");
        listRepo.save(list);
        auditService.record("regression", "reg_" + list.getId(), "回归项状态",
                item.getTitle() + " → " + status);
        return listDetail(list);
    }

    private static String moduleLabel(String project, String module, String sub) {
        StringBuilder sb = new StringBuilder();
        for (String part : new String[]{project, module, sub}) {
            if (part != null && !part.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(" / ");
                }
                sb.append(part);
            }
        }
        return sb.length() > 0 ? sb.toString() : "未指定模块";
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }
}
