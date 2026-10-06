<template>
  <el-container class="layout">
    <!-- 深色侧边栏 -->
    <el-aside width="216px" class="sidebar">
      <div class="brand">
        <span class="brand-logo">TP</span>
        <div class="brand-text">
          <div class="brand-name">TestPilot</div>
          <div class="brand-sub">个人 QA 工作台</div>
        </div>
      </div>
      <el-menu :default-active="activeMenu" router class="sidebar-menu"
               background-color="#263238" text-color="#b0bec5" active-text-color="#f7c948">
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          <el-icon><component :is="m.icon" /></el-icon>
          <span>{{ m.title }}</span>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-footer">
        <span class="dot" :class="dbOk ? 'dot-ok' : 'dot-err'"></span>
        {{ dbOk ? '数据库已连接' : '数据库未连接' }}
      </div>
    </el-aside>

    <!-- 顶栏 + 内容 -->
    <el-container>
      <el-header class="topbar" height="56px">
        <div class="topbar-title">{{ currentTitle }}</div>
        <div class="topbar-right">
          <el-tag v-if="llmEnabled" type="warning" size="small" effect="light">LLM 已启用</el-tag>
          <el-tag v-else type="info" size="small" effect="plain">规则引擎模式</el-tag>
          <el-divider direction="vertical" />
          <el-icon><User /></el-icon>
          <span class="username">测试同学</span>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import http from '../api'

const menus = [
  { path: '/dashboard', title: '工作台首页', icon: 'HomeFilled' },
  { path: '/assistant', title: 'AI 测试助手', icon: 'MagicStick' },
  { path: '/runs', title: '执行记录', icon: 'VideoPlay' },
  { path: '/cases', title: '用例库', icon: 'Document' },
  { path: '/bug-analysis', title: 'Bug 分析', icon: 'Warning' },
  { path: '/log-analysis', title: '日志分析', icon: 'Tickets' },
  { path: '/sql-analysis', title: 'SQL 分析', icon: 'Coin' },
  { path: '/regression', title: '回归测试', icon: 'RefreshRight' },
  { path: '/reports', title: '测试报告', icon: 'DataAnalysis' },
  { path: '/knowledge', title: '知识库', icon: 'Collection' },
  { path: '/workbench', title: '工作台编排', icon: 'SetUp' },
  { path: '/validation', title: '案例验证', icon: 'CircleCheck' },
  { path: '/config', title: '配置中心', icon: 'Setting' }
]

const route = useRoute()
const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '')

const dbOk = ref(false)
const llmEnabled = ref(false)

onMounted(async () => {
  try {
    await http.get('/api/dashboard/stats')
    dbOk.value = true
  } catch (e) {
    dbOk.value = false
  }
  try {
    const llms = await http.get('/api/config/llm')
    llmEnabled.value = (llms || []).some((c) => c.enabled)
  } catch (e) {
    llmEnabled.value = false
  }
})
</script>

<style scoped>
.layout {
  height: 100vh;
}

.sidebar {
  background: var(--tp-sidebar-bg);
  display: flex;
  flex-direction: column;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-logo {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--tp-primary);
  color: #4a3800;
  font-weight: 700;
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-name {
  color: #fff;
  font-weight: 700;
  font-size: 15px;
  line-height: 1.2;
}

.brand-sub {
  color: #8fa3ad;
  font-size: 11px;
}

.sidebar-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: rgba(247, 201, 72, 0.12) !important;
  border-right: 3px solid var(--tp-sidebar-active);
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.06);
}

.sidebar-footer {
  padding: 12px 16px;
  color: #8fa3ad;
  font-size: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  gap: 6px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.dot-ok {
  background: var(--tp-pass);
  box-shadow: 0 0 6px rgba(103, 194, 58, 0.8);
}

.dot-err {
  background: var(--tp-p0);
}

.topbar {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.topbar-title {
  font-size: 16px;
  font-weight: 600;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 13px;
}

.username {
  margin-left: 2px;
}

.main {
  background: #f5f6f8;
  padding: 0;
  overflow-y: auto;
}
</style>
