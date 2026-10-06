<template>
  <AnalysisPage :cfg="cfg" />
</template>

<script setup>
import AnalysisPage from './components/AnalysisPage.vue'

const cfg = {
  taskType: 'log_triage',
  title: '日志分析',
  desc: '提取时间线、定位异常片段、扩展调用链排查方向；时间范围与日志上下文是关键输入。',
  formTitle: '日志排查表单',
  shortName: '日志',
  placeholder: '时间范围（如 2026-10-02 13:00 ~ 13:30）\ntraceId / 服务名\n异常关键词（超时/空指针/Lock wait…）\n关键日志片段（含时间戳）',
  exampleTitle: '支付回调消费超时异常排查',
  example: '时间范围：2026-10-02 13:00 ~ 13:30，环境 staging v2.8.3。\n服务：payment-callback-consumer。\ntraceId=demo-1024。\n关键日志：\n13:20:01 WARN [pool-3] Lock wait timeout exceeded\n13:20:31 ERROR PaymentCallbackHandler#handle timeout, retry=3\n异常关键词：Lock wait timeout、消费超时。',
  outputFields: [
    { name: 'risk_level', label: '风险等级评估' },
    { name: 'timeline', label: '时间线还原' },
    { name: 'abnormal_fragments', label: '异常片段定位' },
    { name: 'call_chain', label: '调用链扩展方向' },
    { name: 'kb_refs', label: '引用的知识库条目（真实检索）' },
    { name: 'triage_suggestions', label: '排查建议' },
    { name: 'human_review_points', label: '人工复核点' },
    { name: 'need_more_info', label: '待补充信息（证据不足时标记）' }
  ]
}
</script>
