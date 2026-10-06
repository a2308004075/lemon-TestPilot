package com.testpilot.engine;

import com.testpilot.entity.KbItem;
import com.testpilot.entity.TestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * 分析上下文：一次结构化分析所需的全部输入。
 */
public class AnalysisContext {

    private String inputText;
    private String projectName = "";
    private String moduleName = "";
    private String submoduleName = "";
    private String risk = "P1";
    private int completeness = 100;
    private List<String> missing = new ArrayList<String>();
    /** 命中的知识库引用（仅已发布，绝不编造） */
    private List<KbItem> kbRefs = new ArrayList<KbItem>();
    /** 高风险用例（回归清单生成用） */
    private List<TestCase> highRiskCases = new ArrayList<TestCase>();

    public String getInputText() {
        return inputText;
    }

    public void setInputText(String inputText) {
        this.inputText = inputText;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getSubmoduleName() {
        return submoduleName;
    }

    public void setSubmoduleName(String submoduleName) {
        this.submoduleName = submoduleName;
    }

    public String getRisk() {
        return risk;
    }

    public void setRisk(String risk) {
        this.risk = risk;
    }

    public int getCompleteness() {
        return completeness;
    }

    public void setCompleteness(int completeness) {
        this.completeness = completeness;
    }

    public List<String> getMissing() {
        return missing;
    }

    public void setMissing(List<String> missing) {
        this.missing = missing;
    }

    public List<KbItem> getKbRefs() {
        return kbRefs;
    }

    public void setKbRefs(List<KbItem> kbRefs) {
        this.kbRefs = kbRefs;
    }

    public List<TestCase> getHighRiskCases() {
        return highRiskCases;
    }

    public void setHighRiskCases(List<TestCase> highRiskCases) {
        this.highRiskCases = highRiskCases;
    }

    /** 模块全路径显示，如：电商平台 / 支付中心 / 支付回调 */
    public String modulePath() {
        StringBuilder sb = new StringBuilder();
        if (projectName != null && !projectName.isEmpty()) {
            sb.append(projectName);
        }
        if (moduleName != null && !moduleName.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" / ");
            }
            sb.append(moduleName);
        }
        if (submoduleName != null && !submoduleName.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" / ");
            }
            sb.append(submoduleName);
        }
        return sb.length() > 0 ? sb.toString() : "未指定模块";
    }

    /** 提取 traceId（如 traceId=demo-1024） */
    public String traceId() {
        if (inputText == null) {
            return null;
        }
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("(?i)trace[_ ]?id\\s*[=:]\\s*([A-Za-z0-9_\\-]+)")
                        .matcher(inputText);
        return m.find() ? m.group(1) : null;
    }
}
