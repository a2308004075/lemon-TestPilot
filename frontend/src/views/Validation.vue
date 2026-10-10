<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Play, LoaderCircle, ShieldCheck, AlertTriangle } from 'lucide-vue-next'
import Badge from '../components/ui/Badge.vue'
import TDrawer from '../components/ui/TDrawer.vue'
import { apiValidationCases, apiValidationRun } from '../api'
import { fmtSlash } from '../utils/format'

const cases = ref([])
const runningId = ref(null)
const resultVisible = ref(false)
const result = ref(null)

const splitLines = (text) => (text || '').split('\n').map((s) => s.trim()).filter(Boolean)
const splitRoutes = (text) => (text || '').split(/[→,，]/).map((s) => s.trim()).filter(Boolean)

const load = async () => {
  cases.value = await apiValidationCases()
}

const run = async (c) => {
  runningId.value = c.id
  try {
    result.value = await apiValidationRun(c.id)
    resultVisible.value = true
    if (result.value.passed) {
      ElMessage.success('验证通过：路由与安全规则均符合预期')
    } else {
      ElMessage.warning('验证未通过，请查看检查项详情')
    }
    await load()
  } finally {
    runningId.value = null
  }
}

onMounted(load)
</script>

<template>
  <div class="page stack wb-page">
    <div class="page-intro">
      <div>
        <span class="wb-kicker">VALIDATION SANDBOX</span>
        <h2>案例验证</h2>
        <p>用固定 walkthrough 验证路由、流程与安全规则，发布工作台资产前先回归。</p>
      </div>
    </div>

    <section v-for="c in cases" :key="c.id" class="panel wb-validation">
      <div class="wb-validation-head">
        <div>
          <span class="wb-id">{{ c.caseNo }}</span>
          <h3>{{ c.title }}</h3>
        </div>
        <Badge :value="c.lastResult || '未运行'" />
      </div>
      <div class="two-col">
        <div>
          <h4>完整材料</h4>
          <pre>{{ c.material }}</pre>
        </div>
        <div>
          <h4>预期检查</h4>
          <div v-if="splitRoutes(c.expectedRoutes).length" class="route-chips">
            <span v-for="r in splitRoutes(c.expectedRoutes)" :key="r">{{ r }}</span>
          </div>
          <ul style="margin: 0; padding-left: 18px">
            <li v-for="(chk, i) in splitLines(c.expectedChecks)" :key="i">{{ chk }}</li>
          </ul>
          <p v-if="c.lastRunAt" class="wb-last">
            最近验证：{{ fmtSlash(c.lastRunAt) }}<template v-if="c.lastRoutes"> · 实际路由 {{ c.lastRoutes }}</template>
          </p>
        </div>
      </div>
      <button class="button primary" :disabled="runningId === c.id" @click="run(c)">
        <Play v-if="runningId !== c.id" /><LoaderCircle v-else class="spin" />运行验证案例
      </button>
    </section>

    <TDrawer
      v-if="resultVisible && result" eyebrow="VALIDATION RESULT"
      :title="result.passed ? '验证通过' : '验证未通过'" :subtitle="result.caseNo" @close="resultVisible = false"
    >
      <div v-if="result.passed" class="wb-ok" style="margin: 0 0 12px">
        <ShieldCheck />
        <span>
          <strong>路由与安全规则均符合预期</strong>
          <small>实际路由：{{ (result.actualRoutes || []).join(' → ') }}</small>
        </span>
      </div>
      <div v-else class="missing-box" style="margin: 0 0 12px">
        <AlertTriangle />
        <div>
          <strong>验证未通过</strong>
          <p>实际路由：{{ (result.actualRoutes || []).join(' → ') || '无' }}。请检查触发词与路由顺序后重试。</p>
        </div>
      </div>

      <section class="detail-section">
        <h3>检查项</h3>
        <div v-for="(chk, i) in result.checks || []" :key="i" class="inspect-row">
          <Badge :value="chk.passed ? '通过' : '失败'" />
          <div>
            <strong>{{ chk.name }}</strong>
            <small>{{ chk.detail }}</small>
          </div>
        </div>
      </section>

      <section class="detail-section" v-for="o in result.outputs || []" :key="o.taskType">
        <h3>{{ o.taskName }} · {{ o.role === 'main' ? '主任务' : '辅助任务' }}</h3>
        <pre>{{ JSON.stringify(o.output, null, 2) }}</pre>
      </section>
    </TDrawer>
  </div>
</template>
