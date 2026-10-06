<template>
  <AnalysisPage :cfg="cfg" />
</template>

<script setup>
import AnalysisPage from './components/AnalysisPage.vue'

const cfg = {
  taskType: 'sql_analysis',
  title: 'SQL 分析',
  desc: '语法与正确性检查、性能风险识别、影响范围评估与优化建议；SQL 文本是必要输入。',
  formTitle: 'SQL 分析表单',
  shortName: 'SQL',
  placeholder: 'SQL 文本（完整语句，多条请分行）\n分析目标（一致性核对 / 慢查询定位 / 索引评估）\n相关表结构与数据量（可选）\n执行计划（可选）',
  exampleTitle: '订单与支付流水一致性 SQL 排查',
  example: '分析目标：核对订单状态与支付流水一致性。\n场景：支付成功后订单仍为待支付，怀疑状态同步链路丢失。\n待排查 SQL 方向：\nSELECT o.order_no, o.status, p.pay_status, p.pay_time\nFROM t_order o LEFT JOIN t_payment p ON o.order_no = p.order_no\nWHERE o.status = \'待支付\' AND p.pay_status = \'成功\'\n  AND p.pay_time < DATE_SUB(NOW(), INTERVAL 30 SECOND);\n环境：staging v2.8.3。',
  outputFields: [
    { name: 'risk_level', label: '风险等级评估' },
    { name: 'sql_correctness', label: 'SQL 正确性检查' },
    { name: 'performance_risk', label: '性能风险（索引/锁）' },
    { name: 'impact_scope', label: '影响范围评估' },
    { name: 'kb_refs', label: '引用的知识库条目（真实检索）' },
    { name: 'optimization_suggestions', label: '优化建议' },
    { name: 'review_conclusion', label: '审核结论（需人工复核）' },
    { name: 'need_more_info', label: '待补充信息（证据不足时标记）' }
  ]
}
</script>
