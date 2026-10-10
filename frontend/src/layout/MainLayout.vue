<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Home, Sparkles, GitBranch, ListChecks, Bug, SquareTerminal, Database,
  ClipboardCheck, FileBarChart, BookOpen, Settings, Settings2, TestTube2,
  PanelLeftClose, Menu, Plus
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const collapsed = ref(false)

const nav = [
  { path: '/dashboard', label: '工作台首页', icon: Home },
  { path: '/assistant', label: 'AI 测试助手', icon: Sparkles },
  { path: '/runs', label: '执行记录', icon: GitBranch },
  { path: '/cases', label: '用例库', icon: ListChecks },
  { path: '/bug-analysis', label: 'Bug 分析', icon: Bug },
  { path: '/log-analysis', label: '日志分析', icon: SquareTerminal },
  { path: '/sql-analysis', label: 'SQL 分析', icon: Database },
  { path: '/regression', label: '回归测试', icon: ClipboardCheck },
  { path: '/reports', label: '测试报告', icon: FileBarChart },
  { path: '/knowledge', label: '知识库', icon: BookOpen },
  { path: '/workbench', label: '工作台编排', icon: Settings },
  { path: '/validation', label: '案例验证', icon: TestTube2 },
  { path: '/config', label: '配置中心', icon: Settings2 }
]

const pageTitle = computed(() => route.meta.title || '')
const isActive = (path) => route.path === path

const go = (path) => {
  if (route.path !== path) router.push(path)
}
</script>

<template>
  <div class="shell" :class="{ 'is-collapsed': collapsed }">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">T</div>
        <div class="brand-copy">
          <strong>TestPilot</strong>
          <small>PERSONAL QA WORKSPACE</small>
        </div>
      </div>
      <button class="collapse" :aria-label="collapsed ? '展开侧栏' : '收起侧栏'" @click="collapsed = !collapsed">
        <Menu v-if="collapsed" />
        <PanelLeftClose v-else />
      </button>
      <nav>
        <button
          v-for="(item, index) in nav"
          :key="item.path"
          :class="{ active: isActive(item.path) }"
          :title="item.label"
          @click="go(item.path)"
        >
          <span class="nav-icon"><component :is="item.icon" /></span>
          <span class="nav-num">{{ String(index + 1).padStart(2, '0') }}</span>
          <span class="nav-text">{{ item.label }}</span>
        </button>
      </nav>
      <div class="side-status">
        <i />
        <span>本机数据库已连接<br /><small>127.0.0.1:3344</small></span>
      </div>
    </aside>
    <main class="main">
      <header class="topbar">
        <div>
          <span class="eyebrow">PERSONAL WORKSPACE</span>
          <h1>{{ pageTitle }}</h1>
        </div>
        <div class="top-actions">
          <button class="button subtle" @click="go('/knowledge')"><BookOpen />维护知识</button>
          <button class="button primary" @click="go('/assistant')"><Plus />新建任务</button>
        </div>
      </header>
      <div class="content">
        <router-view />
      </div>
    </main>
  </div>
</template>
