<template>
  <AnalysisPage :cfg="cfg" />
</template>

<script setup>
import AnalysisPage from './components/AnalysisPage.vue'

const cfg = {
  taskType: 'bug_analysis',
  title: 'Bug 分析',
  desc: '定位现象、证据和回归范围',
  formTitle: 'Bug 分析 · 智能输入',
  shortName: 'Bug',
  titlePlaceholder: '例如：支付成功订单未同步',
  placeholder: '粘贴 Bug 现象、复现步骤、实际/预期结果、相关改动…',
  requiredHints: ['问题现象', '复现步骤', '预期结果'],
  outputFields: ['影响模块', '证据链', '可能原因', '排查路径', '回归范围']
}
</script>
