package com.testpilot.service;

import com.testpilot.common.BizException;
import com.testpilot.common.CryptoService;
import com.testpilot.entity.SysLlmConfig;
import com.testpilot.entity.SysModule;
import com.testpilot.entity.SysOutputTemplate;
import com.testpilot.entity.SysTaskKbRule;
import com.testpilot.entity.SysTaskRule;
import com.testpilot.engine.LlmClient;
import com.testpilot.engine.TaskTypes;
import com.testpilot.repository.SysLlmConfigRepository;
import com.testpilot.repository.SysModuleRepository;
import com.testpilot.repository.SysOutputTemplateRepository;
import com.testpilot.repository.SysTaskKbRuleRepository;
import com.testpilot.repository.SysTaskRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置中心：三级模块、大模型厂商、输出格式模板、任务关联规则。
 */
@Service
public class ConfigService {

    @Autowired
    private SysModuleRepository moduleRepo;
    @Autowired
    private SysLlmConfigRepository llmRepo;
    @Autowired
    private SysOutputTemplateRepository templateRepo;
    @Autowired
    private SysTaskRuleRepository ruleRepo;
    @Autowired
    private SysTaskKbRuleRepository kbRuleRepo;
    @Autowired
    private LlmClient llmClient;
    @Autowired
    private CryptoService cryptoService;
    @Autowired
    private AuditService auditService;

    // ---------------- 项目与模块 ----------------

    public List<Map<String, Object>> moduleTree() {
        Map<String, Map<String, Object>> projectMap = new LinkedHashMap<String, Map<String, Object>>();
        for (SysModule m : moduleRepo.findAllByOrderByProjectNameAscModuleNameAscSubmoduleNameAsc()) {
            Map<String, Object> pNode = projectMap.get(m.getProjectName());
            if (pNode == null) {
                pNode = new LinkedHashMap<String, Object>();
                pNode.put("project", m.getProjectName());
                pNode.put("modules", new ArrayList<Map<String, Object>>());
                projectMap.put(m.getProjectName(), pNode);
            }
            List<Map<String, Object>> modules = (List<Map<String, Object>>) pNode.get("modules");
            Map<String, Object> mNode = findByKey(modules, "module", m.getModuleName());
            if (mNode == null) {
                mNode = new LinkedHashMap<String, Object>();
                mNode.put("module", m.getModuleName());
                mNode.put("submodules", new ArrayList<Map<String, Object>>());
                modules.add(mNode);
            }
            List<Map<String, Object>> subs = (List<Map<String, Object>>) mNode.get("submodules");
            Map<String, Object> sNode = new LinkedHashMap<String, Object>();
            sNode.put("id", m.getId());
            sNode.put("submodule", m.getSubmoduleName());
            sNode.put("enabled", m.getEnabled());
            sNode.put("updatedAt", m.getUpdatedAt());
            subs.add(sNode);
        }
        return new ArrayList<Map<String, Object>>(projectMap.values());
    }

    private Map<String, Object> findByKey(List<Map<String, Object>> list, String key, String value) {
        for (Map<String, Object> m : list) {
            if (value.equals(m.get(key))) {
                return m;
            }
        }
        return null;
    }

    public SysModule createModule(Map<String, Object> body) {
        String project = str(body.get("project"));
        String module = str(body.get("module"));
        String submodule = str(body.get("submodule"));
        if (project.isEmpty() || module.isEmpty() || submodule.isEmpty()) {
            throw new BizException("项目、模块、子模块均不能为空");
        }
        if (moduleRepo.existsByProjectNameAndModuleNameAndSubmoduleName(project, module, submodule)) {
            throw new BizException("该三级模块已存在：" + project + " / " + module + " / " + submodule);
        }
        SysModule m = new SysModule();
        m.setProjectName(project);
        m.setModuleName(module);
        m.setSubmoduleName(submodule);
        m.setEnabled(true);
        m.setUpdatedAt(LocalDateTime.now());
        SysModule saved = moduleRepo.save(m);
        auditService.record("module", "module_" + saved.getId(), "新增模块",
                project + " / " + module + " / " + submodule);
        return saved;
    }

    /** 编辑三级模块名称（同组合已存在时报友好错误） */
    public SysModule updateModule(Long id, Map<String, Object> body) {
        SysModule m = moduleRepo.findById(id)
                .orElseThrow(() -> new BizException("模块不存在：" + id));
        if (body.containsKey("project") && !str(body.get("project")).isEmpty()) {
            m.setProjectName(str(body.get("project")));
        }
        if (body.containsKey("module") && !str(body.get("module")).isEmpty()) {
            m.setModuleName(str(body.get("module")));
        }
        if (body.containsKey("submodule") && !str(body.get("submodule")).isEmpty()) {
            m.setSubmoduleName(str(body.get("submodule")));
        }
        if (m.getProjectName().isEmpty() || m.getModuleName().isEmpty() || m.getSubmoduleName().isEmpty()) {
            throw new BizException("项目、模块、子模块均不能为空");
        }
        m.setUpdatedAt(LocalDateTime.now());
        try {
            SysModule saved = moduleRepo.save(m);
            auditService.record("module", "module_" + saved.getId(), "编辑模块",
                    m.getProjectName() + " / " + m.getModuleName() + " / " + m.getSubmoduleName());
            return saved;
        } catch (DataIntegrityViolationException e) {
            throw new BizException("该三级模块已存在：" + m.getProjectName() + " / "
                    + m.getModuleName() + " / " + m.getSubmoduleName());
        }
    }

    /** 启用 / 停用（停用后不再出现在新建表单中） */
    public SysModule toggleModule(Long id) {
        SysModule m = moduleRepo.findById(id)
                .orElseThrow(() -> new BizException("模块不存在：" + id));
        m.setEnabled(!Boolean.TRUE.equals(m.getEnabled()));
        m.setUpdatedAt(LocalDateTime.now());
        SysModule saved = moduleRepo.save(m);
        auditService.record("module", "module_" + saved.getId(),
                Boolean.TRUE.equals(saved.getEnabled()) ? "启用模块" : "停用模块",
                saved.getProjectName() + " / " + saved.getModuleName() + " / " + saved.getSubmoduleName());
        return saved;
    }

    // ---------------- 大模型 ----------------

    public List<SysLlmConfig> llmList() {
        List<SysLlmConfig> result = llmRepo.findAllByOrderByIdAsc();
        // 不回传完整 API Key，只回传掩码（掩码基于解密原文）
        for (SysLlmConfig cfg : result) {
            if (cfg.getApiKey() != null && !cfg.getApiKey().isEmpty()) {
                cfg.setApiKey(maskKey(cryptoService.decrypt(cfg.getApiKey())));
            }
        }
        return result;
    }

    public SysLlmConfig llmUpdate(Long id, Map<String, Object> body) {
        SysLlmConfig cfg = llmRepo.findById(id)
                .orElseThrow(() -> new BizException("厂商配置不存在：" + id));
        if (body.containsKey("baseUrl")) {
            cfg.setBaseUrl(str(body.get("baseUrl")));
        }
        if (body.containsKey("modelName")) {
            cfg.setModelName(str(body.get("modelName")));
        }
        if (body.containsKey("apiKey")) {
            String key = str(body.get("apiKey"));
            // 掩码值或空值不覆盖已保存的 Key；新 Key 加密后入库
            if (!key.isEmpty() && !key.contains("*")) {
                cfg.setApiKey(cryptoService.encrypt(key));
            } else if (key.isEmpty()) {
                cfg.setApiKey("");
            }
        }
        if (body.containsKey("temperature")) {
            try {
                cfg.setTemperature(new BigDecimal(str(body.get("temperature"))));
            } catch (NumberFormatException ignore) {
                // 保持原值
            }
        }
        if (body.containsKey("timeoutSeconds")) {
            cfg.setTimeoutSeconds(parseInt(body.get("timeoutSeconds"), 60));
        }
        if (body.containsKey("maxOutput")) {
            cfg.setMaxOutput(parseInt(body.get("maxOutput"), 4096));
        }
        if (body.containsKey("enabled")) {
            cfg.setEnabled(Boolean.parseBoolean(body.get("enabled").toString()));
        }
        cfg.setUpdatedAt(LocalDateTime.now());
        SysLlmConfig saved = llmRepo.save(cfg);
        auditService.record("llm", saved.getVendorCode(), "保存模型配置",
                saved.getVendorName() + (Boolean.TRUE.equals(saved.getEnabled()) ? " · 已启用" : " · 未启用"));
        if (saved.getApiKey() != null && !saved.getApiKey().isEmpty()) {
            saved.setApiKey(maskKey(cryptoService.decrypt(saved.getApiKey())));
        }
        return saved;
    }

    public Map<String, Object> llmTest(Long id) {
        SysLlmConfig cfg = llmRepo.findById(id)
                .orElseThrow(() -> new BizException("厂商配置不存在：" + id));
        Map<String, Object> result = llmClient.testConnection(cfg);
        auditService.record("llm", cfg.getVendorCode(), "连接测试",
                cfg.getVendorName() + " · " + (Boolean.TRUE.equals(result.get("ok")) ? "成功" : "失败"));
        return result;
    }

    private static String maskKey(String key) {
        if (key.length() <= 8) {
            return "****";
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    // ---------------- 格式模板 ----------------

    public List<SysOutputTemplate> templates(String taskType) {
        if (taskType == null || taskType.isEmpty()) {
            List<SysOutputTemplate> result = new ArrayList<SysOutputTemplate>();
            for (String type : TaskTypes.ORDER) {
                result.addAll(templateRepo.findByTaskTypeAndVisibleTrueOrderBySortOrderAsc(type));
            }
            return result;
        }
        return templateRepo.findByTaskTypeAndVisibleTrueOrderBySortOrderAsc(taskType);
    }

    /**
     * 模板字段全量同步：新增字段插入；删除字段仅隐藏（visible=0），原始快照仍保留。
     */
    public List<SysOutputTemplate> templateUpdate(String taskType, List<Map<String, Object>> fields) {
        if (!TaskTypes.isValid(taskType)) {
            throw new BizException("无效的任务类型：" + taskType);
        }
        List<SysOutputTemplate> existing = templateRepo.findByTaskTypeOrderBySortOrderAsc(taskType);
        // 本次提交的字段名集合
        List<String> submitted = new ArrayList<String>();
        int sort = 1;
        for (Map<String, Object> f : fields) {
            String fieldName = str(f.get("fieldName"));
            if (fieldName.isEmpty()) {
                continue;
            }
            submitted.add(fieldName);
            String label = strOr(f.get("fieldLabel"), fieldName);
            boolean required = !"false".equalsIgnoreCase(str(f.get("required")));
            SysOutputTemplate tpl = templateRepo.findByTaskTypeAndFieldName(taskType, fieldName)
                    .orElse(null);
            if (tpl == null) {
                tpl = new SysOutputTemplate();
                tpl.setTaskType(taskType);
                tpl.setFieldName(fieldName);
            }
            tpl.setFieldLabel(label);
            tpl.setRequiredFlag(required);
            tpl.setSortOrder(sort++);
            tpl.setVisible(true);
            templateRepo.save(tpl);
        }
        // 未提交的已有字段：仅隐藏
        for (SysOutputTemplate tpl : existing) {
            if (!submitted.contains(tpl.getFieldName())) {
                tpl.setVisible(false);
                templateRepo.save(tpl);
            }
        }
        return templateRepo.findByTaskTypeAndVisibleTrueOrderBySortOrderAsc(taskType);
    }

    // ---------------- 关联规则 ----------------

    public List<Map<String, Object>> rules() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (String main : TaskTypes.ORDER) {
            List<SysTaskRule> rulesOf = ruleRepo.findByMainTaskTypeOrderBySortOrderAsc(main);
            if (!rulesOf.isEmpty()) {
                Map<String, Object> group = new LinkedHashMap<String, Object>();
                group.put("mainTaskType", main);
                group.put("mainTaskName", TaskTypes.nameOf(main));
                group.put("rules", rulesOf);
                result.add(group);
            }
        }
        return result;
    }

    public SysTaskRule ruleUpdate(Long id, Map<String, Object> body) {
        SysTaskRule rule = ruleRepo.findById(id)
                .orElseThrow(() -> new BizException("关联规则不存在：" + id));
        if (body.containsKey("enabled")) {
            rule.setEnabled(Boolean.parseBoolean(body.get("enabled").toString()));
        }
        if (body.containsKey("sortOrder")) {
            rule.setSortOrder(parseInt(body.get("sortOrder"), rule.getSortOrder()));
        }
        return ruleRepo.save(rule);
    }

    // ---------------- 任务与知识关联 ----------------

    /** 配置页任务展示顺序（与手册一致） */
    private static final List<String> KB_RULE_ORDER = Arrays.asList(
            TaskTypes.TESTCASE_GEN, TaskTypes.BUG_ANALYSIS, TaskTypes.LOG_TRIAGE,
            TaskTypes.SQL_ANALYSIS, TaskTypes.REGRESSION_LIST, TaskTypes.TEST_REPORT);

    public List<Map<String, Object>> kbRules() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (String main : KB_RULE_ORDER) {
            List<String> categories = new ArrayList<String>();
            for (SysTaskKbRule row : kbRuleRepo.findByMainTaskTypeOrderBySortOrderAsc(main)) {
                if (Boolean.TRUE.equals(row.getEnabled())) {
                    categories.add(row.getCategory());
                }
            }
            Map<String, Object> group = new LinkedHashMap<String, Object>();
            group.put("mainTaskType", main);
            group.put("mainTaskName", TaskTypes.nameOf(main));
            group.put("categories", categories);
            result.add(group);
        }
        return result;
    }

    /** 保存任务的知识分类关联（全量覆盖该任务的配置） */
    @Transactional
    public List<SysTaskKbRule> saveKbRule(String mainTaskType, List<String> categories) {
        if (!TaskTypes.isValid(mainTaskType)) {
            throw new BizException("未知任务类型：" + mainTaskType);
        }
        kbRuleRepo.deleteByMainTaskType(mainTaskType);
        List<SysTaskKbRule> saved = new ArrayList<SysTaskKbRule>();
        int sort = 1;
        for (String category : categories) {
            SysTaskKbRule row = new SysTaskKbRule();
            row.setMainTaskType(mainTaskType);
            row.setCategory(category);
            row.setSortOrder(sort++);
            row.setEnabled(true);
            saved.add(kbRuleRepo.save(row));
        }
        return saved;
    }

    private static int parseInt(Object value, int fallback) {
        try {
            return Integer.parseInt(str(value));
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static String strOr(Object o, String fallback) {
        String v = str(o);
        return v.isEmpty() ? fallback : v;
    }
}
