<template>
  <AnalysisPage :cfg="cfg" />
</template>

<script setup>
import AnalysisPage from './components/AnalysisPage.vue'

const cfg = {
  taskType: 'sql_analysis',
  title: 'SQL 分析',
  desc: '检查正确性、性能和一致性',
  formTitle: 'SQL 分析 · 智能输入',
  shortName: 'SQL',
  titlePlaceholder: '例如：订单与流水一致性检查',
  placeholder: '粘贴 SQL、表结构、执行计划、实际结果和预期结果…',
  requiredHints: ['SQL 语句', '分析目标'],
  outputFields: ['SQL 正确性', '性能风险', '影响范围', '优化建议', '审核结论']
}
</script>
