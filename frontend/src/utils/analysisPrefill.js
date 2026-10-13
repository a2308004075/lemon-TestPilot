import { ref } from 'vue'

/**
 * 联合分析预填态（6.11 W4）：用例库批量「联合分析」写入，分析页挂载时消费并清空。
 * 模块级 ref 即"App 层预填状态"的轻量等价（项目未引入 pinia）。
 */
export const analysisPrefill = ref(null)

export const setAnalysisPrefill = (payload) => {
  analysisPrefill.value = payload
}

export const consumeAnalysisPrefill = () => {
  const payload = analysisPrefill.value
  analysisPrefill.value = null
  return payload
}
