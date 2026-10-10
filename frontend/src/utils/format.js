/**
 * 跨页面共享的格式化工具（收敛各视图重复定义）。
 */

// 任务类型编码 → 中文名
export const TASK_NAMES = {
  testcase_gen: '测试用例生成',
  bug_analysis: 'Bug 分析',
  log_triage: '日志排查',
  sql_analysis: 'SQL 分析',
  regression_list: '回归清单',
  test_report: '测试报告',
  prompt_test: 'Prompt 测试'
}

export const taskName = (type) => TASK_NAMES[type] || type

// ISO 时间 → 本地展示（去掉 T 与毫秒 / 仅日期）
export const fmtTime = (t) => (t || '').replace('T', ' ').slice(0, 19)

export const fmtDate = (t) => (t || '').slice(0, 10)

// ISO 时间 → 斜杠格式（手册样式：2026/10/2 13:16:24）
export const fmtSlash = (t) => {
  if (!t) return ''
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return (t || '').replace('T', ' ').slice(0, 19)
  return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`
}

// 复核状态徽章：通过 / 驳回补充 / 待复核
export const reviewClass = (s) =>
  s === '通过' ? 'tp-status-ok' : s === '驳回补充' ? 'tp-status-block' : 'tp-status-warn'

// 任务/执行状态徽章：已完成 / 待补充
export const taskStatusClass = (s) => (s === '已完成' ? 'tp-status-ok' : 'tp-status-warn')

// 用例与回归项状态徽章
export const caseStatusClass = (s) =>
  s === '通过' ? 'tp-status-ok' : s === '失败' || s === '阻塞' ? 'tp-status-block' : 'tp-status-info'

// 知识状态徽章
export const kbStatusClass = (s) =>
  s === '已发布' ? 'tp-status-ok' : s === '已撤销' || s === '已驳回' ? 'tp-status-block' : 'tp-status-warn'

// 上线结论徽章
export const conclusionClass = (c) =>
  c === '可上线' ? 'tp-status-ok' : c === '不可上线' ? 'tp-status-block' : 'tp-status-warn'
