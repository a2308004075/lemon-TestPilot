import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layout/MainLayout.vue'

const routes = [
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '工作台首页' } },
      { path: 'assistant', name: 'Assistant', component: () => import('../views/Assistant.vue'), meta: { title: 'AI 测试助手' } },
      { path: 'runs', name: 'Runs', component: () => import('../views/Runs.vue'), meta: { title: '执行记录' } },
      { path: 'cases', name: 'Cases', component: () => import('../views/Cases.vue'), meta: { title: '用例库' } },
      { path: 'bug-analysis', name: 'BugAnalysis', component: () => import('../views/BugAnalysis.vue'), meta: { title: 'Bug 分析' } },
      { path: 'log-analysis', name: 'LogAnalysis', component: () => import('../views/LogAnalysis.vue'), meta: { title: '日志分析' } },
      { path: 'sql-analysis', name: 'SqlAnalysis', component: () => import('../views/SqlAnalysis.vue'), meta: { title: 'SQL 分析' } },
      { path: 'regression', name: 'Regression', component: () => import('../views/Regression.vue'), meta: { title: '回归测试' } },
      { path: 'reports', name: 'Reports', component: () => import('../views/Reports.vue'), meta: { title: '测试报告' } },
      { path: 'knowledge', name: 'Knowledge', component: () => import('../views/Knowledge.vue'), meta: { title: '知识库' } },
      { path: 'workbench', name: 'Workbench', component: () => import('../views/Workbench.vue'), meta: { title: '工作台编排' } },
      { path: 'validation', name: 'Validation', component: () => import('../views/Validation.vue'), meta: { title: '案例验证' } },
      { path: 'config', name: 'Config', component: () => import('../views/Config.vue'), meta: { title: '配置中心' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.afterEach((to) => {
  document.title = (to.meta.title ? to.meta.title + ' · ' : '') + 'TestPilot 个人 QA 工作台'
})

export default router
