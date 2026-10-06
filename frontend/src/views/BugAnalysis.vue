<template>
  <AnalysisPage :cfg="cfg" />
</template>

<script setup>
import AnalysisPage from './components/AnalysisPage.vue'

const cfg = {
  taskType: 'bug_analysis',
  title: 'Bug 分析',
  desc: '还原现象、匹配历史 Bug、定位排查路径并输出回归范围；根因结论需证据支撑，不跳结论。',
  formTitle: 'Bug 分析表单',
  shortName: 'Bug',
  placeholder: '问题现象（实际结果 vs 预期结果）\n复现步骤（每行一步）\n环境与版本信息\n关联 traceId / 日志片段 / SQL',
  exampleTitle: '支付成功后订单仍为待支付',
  example: '支付成功后订单仍为待支付，staging 环境 v2.8.3。\n复现步骤：\n1. 发起支付并完成付款\n2. 等待 30 秒后查询订单详情\n实际结果：订单状态仍为「待支付」\n预期结果：订单状态应为「已支付」\ntraceId=demo-1024，日志出现 Lock wait timeout。',
  outputFields: [
    { name: 'risk_level', label: '风险等级评估' },
    { name: 'phenomenon', label: '现象还原' },
    { name: 'possible_causes', label: '可能原因（按可能性排序）' },
    { name: 'check_path', label: '排查路径建议' },
    { name: 'kb_refs', label: '引用的知识库条目（真实检索）' },
    { name: 'regression_scope', label: '回归范围' },
    { name: 'human_review_points', label: '人工复核点' },
    { name: 'need_more_info', label: '待补充信息（证据不足时标记）' }
  ]
}
</script>
