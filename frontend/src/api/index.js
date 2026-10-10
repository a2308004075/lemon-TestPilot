import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '',
  timeout: 60000
})

http.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code !== 0) {
        ElMessage.error(body.message || '请求失败')
        return Promise.reject(new Error(body.message))
      }
      return body.data
    }
    return body
  },
  (err) => {
    const msg = err.response && err.response.data && err.response.data.message
      ? err.response.data.message
      : err.message
    ElMessage.error(msg || '网络错误')
    return Promise.reject(err)
  }
)

export default http

// ---------------- API 封装 ----------------

// 工作台首页
export const apiDashboardStats = () => http.get('/api/dashboard/stats')

// AI 测试助手
export const apiAssistantEntries = () => http.get('/api/assistant/entries')
export const apiAssistantIdentify = (payload) => http.post('/api/assistant/identify', payload)
export const apiAssistantExecute = (payload) => http.post('/api/assistant/execute', payload)

// 执行记录
export const apiRuns = (params) => http.get('/api/runs', { params })
export const apiRunDetail = (id) => http.get(`/api/runs/${id}`)
export const apiRunReview = (id, payload) => http.post(`/api/runs/${id}/review`, payload)

// 用例库
export const apiCases = (params) => http.get('/api/cases', { params })
export const apiCaseTree = () => http.get('/api/cases/tree')
export const apiCaseModules = () => http.get('/api/cases/modules')
export const apiCaseGenerateDrafts = (payload) => http.post('/api/cases/generate-drafts', payload)
export const apiCaseCreate = (payload) => http.post('/api/cases', payload)
export const apiCaseDetail = (id) => http.get(`/api/cases/${id}`)
export const apiCaseUpdate = (id, payload) => http.put(`/api/cases/${id}`, payload)
export const apiCaseCopy = (id) => http.post(`/api/cases/${id}/copy`)

// 分析任务（Bug/日志/SQL 共用）
export const apiTasks = (params) => http.get('/api/tasks', { params })
export const apiTaskCreate = (payload) => http.post('/api/tasks', payload)
export const apiTaskCompleteness = (payload) => http.post('/api/tasks/completeness', payload)
export const apiTaskDetail = (id) => http.get(`/api/tasks/${id}`)
export const apiTaskReview = (id, payload) => http.post(`/api/tasks/${id}/review`, payload)
export const apiTaskToKnowledge = (id) => http.post(`/api/tasks/${id}/to-knowledge`)

// 操作审计（详情抽屉时间线）
export const apiAudit = (params) => http.get('/api/audit', { params })

// 回归测试
export const apiRegressionLists = () => http.get('/api/regression/lists')
export const apiRegressionListDetail = (id) => http.get(`/api/regression/lists/${id}`)
export const apiRegressionGenerate = (payload) => http.post('/api/regression/lists/generate', payload)
export const apiRegressionItemStatus = (id, status) => http.put(`/api/regression/items/${id}/status`, { status })

// 测试报告
export const apiReports = () => http.get('/api/reports')
export const apiReportGenerate = (payload) => http.post('/api/reports/generate', payload)
export const apiReportDetail = (id) => http.get(`/api/reports/${id}`)

// 知识库
export const apiKbList = (params) => http.get('/api/kb', { params })
export const apiKbStats = () => http.get('/api/kb/stats')
export const apiKbCategories = () => http.get('/api/kb/categories')
export const apiKbTaskSourced = (params) => http.get('/api/kb/task-sourced', { params })
export const apiKbDetail = (id) => http.get(`/api/kb/${id}`)
export const apiKbCreate = (payload) => http.post('/api/kb', payload)
export const apiKbUpdate = (id, payload) => http.put(`/api/kb/${id}`, payload)
export const apiKbReview = (id, payload) => http.post(`/api/kb/${id}/review`, payload)
export const apiKbRevoke = (id) => http.post(`/api/kb/${id}/revoke`)

// 工作台编排
export const apiWorkbenchEntries = () => http.get('/api/workbench/entries')
export const apiWorkbenchPublish = (taskType, payload) => http.put(`/api/workbench/entries/${taskType}`, payload)

// 案例验证
export const apiValidationCases = () => http.get('/api/validation/cases')
export const apiValidationRun = (id) => http.post(`/api/validation/cases/${id}/run`)

// 配置中心
export const apiConfigModules = () => http.get('/api/config/modules')
export const apiConfigModuleCreate = (payload) => http.post('/api/config/modules', payload)
export const apiConfigModuleUpdate = (id, payload) => http.put(`/api/config/modules/${id}`, payload)
export const apiConfigModuleToggle = (id) => http.put(`/api/config/modules/${id}/toggle`)
export const apiConfigLlm = () => http.get('/api/config/llm')
export const apiConfigLlmUpdate = (id, payload) => http.put(`/api/config/llm/${id}`, payload)
export const apiConfigLlmTest = (id) => http.post(`/api/config/llm/${id}/test-connection`)
export const apiConfigTemplates = (params) => http.get('/api/config/templates', { params })
export const apiConfigTemplateUpdate = (taskType, fields) => http.put(`/api/config/templates/${taskType}`, { fields })
export const apiConfigKbRules = () => http.get('/api/config/kb-rules')
export const apiConfigKbRuleSave = (mainTaskType, categories) => http.put(`/api/config/kb-rules/${mainTaskType}`, { categories })
