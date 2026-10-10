<template>
  <AnalysisPage :cfg="cfg" />
</template>

<script setup>
import AnalysisPage from './components/AnalysisPage.vue'

const cfg = {
  taskType: 'log_triage',
  title: '日志分析',
  desc: '还原调用链与异常节点',
  formTitle: '日志分析 · 智能输入',
  shortName: '日志',
  titlePlaceholder: '例如：支付回调消费超时',
  placeholder: '粘贴发生时间、环境、traceId、上下文日志和预期行为…',
  requiredHints: ['发生时间或 traceId', '日志上下文'],
  outputFields: ['异常摘要', '关键时间线', '错误片段', '可能原因', '排查建议']
}
</script>
