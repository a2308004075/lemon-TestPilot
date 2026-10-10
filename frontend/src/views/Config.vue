<template>
  <div class="page">
    <!-- 页头 -->
    <div class="page-intro">
      <div>
        <h2>配置中心</h2>
        <p>配置三级模块、模型厂商、输出模板和任务关联规则。</p>
      </div>
    </div>

    <div class="tabs">
      <button :class="{ active: tab === 'modules' }" @click="tab = 'modules'">项目与模块</button>
      <button :class="{ active: tab === 'llm' }" @click="tab = 'llm'">大模型</button>
      <button :class="{ active: tab === 'templates' }" @click="tab = 'templates'">格式模板</button>
      <button :class="{ active: tab === 'rules' }" @click="tab = 'rules'">关联规则</button>
    </div>

    <!-- Tab 1: 项目与模块 -->
    <div v-if="tab === 'modules'" class="two-col wide-left" style="margin-top: 18px">
      <div class="panel">
        <div class="panel-title">
          <h3>三级模块结构</h3>
          <p>停用后不再出现在新建表单的模块下拉中</p>
        </div>
        <div class="module-admin">
          <div v-for="p in moduleTree" :key="p.project" class="project-group">
            <h3><Layers /> {{ p.project }} <span class="badge blue">项目</span></h3>
            <div v-for="m in p.modules" :key="m.module" class="module-group">
              <strong>{{ m.module }}</strong>
              <div v-for="s in m.submodules" :key="s.id" class="submodule-row">
                <span>{{ s.submodule }}</span>
                <small>更新 {{ fmtDate(s.updatedAt) }}</small>
                <span class="badge" :class="s.enabled ? 'green' : 'red'">{{ s.enabled ? '启用' : '停用' }}</span>
                <button class="text-button" @click="toggleModule(s)">{{ s.enabled ? '停用' : '恢复' }}</button>
              </div>
            </div>
          </div>
          <Empty v-if="!moduleTree.length" title="暂无模块配置" desc="在右侧添加第一个三级模块" />
        </div>
      </div>

      <div class="panel">
        <div class="panel-title">
          <h3>新增模块</h3>
          <p>项目 → 模块 → 子模块</p>
        </div>
        <div class="field">
          <span>项目</span>
          <input v-model="moduleForm.project" />
        </div>
        <div class="field">
          <span>模块</span>
          <input v-model="moduleForm.module" />
        </div>
        <div class="field">
          <span>子模块</span>
          <input v-model="moduleForm.submodule" />
        </div>
        <button class="button primary full" style="margin-top: 8px" @click="saveModule"><Plus /> 添加三级模块</button>
      </div>
    </div>

    <!-- Tab 2: 大模型 -->
    <div v-else-if="tab === 'llm'" class="settings-layout" style="margin-top: 18px">
      <div class="panel">
        <div class="provider-list">
          <button v-for="row in llms" :key="row.id" :class="{ active: current && current.id === row.id }"
                  @click="selectLlm(row)">
            <span class="wb-mini-icon"><Bot /></span>
            <span>
              <strong style="font-size: 12px">{{ row.vendorName }}</strong>
              <small>{{ row.modelName || '未配置' }}</small>
            </span>
            <i :class="{ on: row.enabled }"></i>
          </button>
        </div>
      </div>

      <div v-if="current" class="panel">
        <div class="panel-title">
          <div>
            <h3>{{ current.vendorName }} 配置</h3>
            <p>API Key 加密保存至本机密钥库，不写入数据库</p>
          </div>
        </div>
        <div class="field">
          <span>Base URL</span>
          <input v-model="llmForm.baseUrl" placeholder="https://api.example.com/v1" />
        </div>
        <div class="inline-grid">
          <div class="field">
            <span>模型名</span>
            <input v-model="llmForm.modelName" placeholder="如 gpt-4o-mini / deepseek-chat" />
          </div>
          <div class="field">
            <span>API Key</span>
            <input v-model="llmForm.apiKey" placeholder="输入后安全保存" />
          </div>
        </div>
        <div class="inline-grid">
          <div class="field">
            <span>温度</span>
            <input v-model="llmForm.temperature" placeholder="0 ~ 1，默认 0.2" />
          </div>
          <div class="field">
            <span>超时（秒）</span>
            <input v-model="llmForm.timeoutSeconds" placeholder="默认 60" />
          </div>
        </div>
        <div class="inline-grid">
          <div class="field">
            <span>最大输出</span>
            <input v-model="llmForm.maxOutput" placeholder="默认 4096" />
          </div>
          <div class="field">
            <span>启用厂商</span>
            <label class="switch" style="margin-top: 5px">
              <input v-model="llmForm.enabled" type="checkbox" />
              <i></i>
              <span>{{ llmForm.enabled ? '启用中' : '未启用' }}</span>
            </label>
          </div>
        </div>
        <div class="page-actions">
          <button class="button primary" @click="saveLlm"><Check /> 保存配置</button>
          <button class="button" :disabled="testing" @click="testLlm"><RefreshCw /> {{ testing ? '测试中…' : '测试连接' }}</button>
        </div>
      </div>
    </div>

    <!-- Tab 3: 格式模板 -->
    <div v-else-if="tab === 'templates'" class="two-col" style="margin-top: 18px; grid-template-columns: .8fr 1.65fr">
      <div class="panel">
        <div class="panel-title"><h3>模板类型</h3></div>
        <div class="template-list">
          <button v-for="t in taskTypes" :key="t.value" class="template-item"
                  :class="{ active: tplTaskType === t.value }" @click="switchTemplate(t.value)">
            <component :is="taskIcon(t.value)" />
            <strong style="font-size: 12px">{{ t.label }}</strong>
            <ChevronRight />
          </button>
        </div>
      </div>

      <div class="panel">
        <div class="panel-title">
          <h3>{{ typeName(tplTaskType) }}格式</h3>
          <p>保存会全量同步历史展示</p>
        </div>
        <div class="field-list" style="margin-top: 16px">
          <div v-for="(f, i) in tplFields" :key="i">
            <GripVertical />
            <input v-model="f.fieldLabel" :placeholder="f.fieldName ? `字段显示名（当前字段名 ${f.fieldName}）` : '字段显示名（保存时自动生成字段名）'" />
            <button class="badge gray" style="border: 0; cursor: pointer"
                    @click="f.required = !f.required">{{ f.required ? '必填' : '可选' }}</button>
            <button class="icon-button" @click="removeField(i)"><X /></button>
          </div>
        </div>
        <button class="button" @click="addField"><Plus /> 添加字段</button>
        <div class="warning" style="margin-top: 14px">
          <AlertTriangle />
          <p><strong>全量同步提醒</strong><br />新增字段会在历史记录中显示「待补齐」；删除字段从当前页面隐藏，但原始快照仍保留。</p>
        </div>
        <button class="button primary" style="margin-top: 14px" @click="saveTemplates"><Check /> 确认并全量同步</button>
      </div>
    </div>

    <!-- Tab 4: 关联规则 -->
    <div v-else style="margin-top: 18px">
      <div class="panel">
        <div class="panel-title">
          <h3>任务与知识关联</h3>
          <p>发布知识后自动进入对应任务的检索范围</p>
        </div>
        <div class="relation-grid">
          <div v-for="g in kbRules" :key="g.mainTaskType" class="relation-card">
            <div class="relation-head">
              <span class="task-icon"><component :is="taskIcon(g.mainTaskType)" /></span>
              <strong>{{ typeName(g.mainTaskType) }}</strong>
            </div>
            <div class="cat-pills">
              <button v-for="c in KB_CATEGORIES" :key="c" class="cat-pill"
                      :class="{ on: g.categories.includes(c) }"
                      @click="toggleKbCategory(g, c)">{{ c }}</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Layers, Plus, Bot, Check, RefreshCw, AlertTriangle, X, GripVertical, ChevronRight,
  ListChecks, SquareTerminal, Database, ClipboardCheck, FileBarChart, FlaskConical, TestTube2
} from 'lucide-vue-next'
import {
  apiConfigModules, apiConfigModuleCreate, apiConfigModuleToggle,
  apiConfigLlm, apiConfigLlmUpdate, apiConfigLlmTest,
  apiConfigTemplates, apiConfigTemplateUpdate,
  apiConfigKbRules, apiConfigKbRuleSave
} from '../api'
import { fmtDate } from '../utils/format'
import Empty from '../components/ui/Empty.vue'

// 配置中心任务命名与顺序（与手册一致，Prompt 测试不进配置页）
const CONFIG_TYPES = [
  ['testcase_gen', '生成测试用例'],
  ['bug_analysis', 'Bug 分析'],
  ['log_triage', '日志分析'],
  ['sql_analysis', 'SQL 分析'],
  ['regression_list', '生成回归清单'],
  ['test_report', '生成测试报告']
]

const taskTypes = CONFIG_TYPES.map(([value, label]) => ({ value, label }))
const typeName = (t) => {
  const found = CONFIG_TYPES.find(([v]) => v === t)
  return found ? found[1] : t
}

const ICONS = {
  testcase_gen: ListChecks,
  bug_analysis: AlertTriangle,
  log_triage: SquareTerminal,
  sql_analysis: Database,
  regression_list: ClipboardCheck,
  test_report: FileBarChart,
  prompt_test: FlaskConical
}
const taskIcon = (t) => ICONS[t] || TestTube2

const tab = ref('modules')

// 模块
const moduleTree = ref([])
const moduleForm = ref({ project: '', module: '', submodule: '' })

// 大模型
const llms = ref([])
const current = ref(null)
const llmForm = ref({})
const testing = ref(false)

// 模板（默认选中第一项）
const tplTaskType = ref('testcase_gen')
const tplFields = ref([])

// 知识分类常量（与知识库分类一致）
const KB_CATEGORIES = ['业务规则库', '回归规则库', '历史 Bug 库', '日志规律库', '接口异常库', 'SQL 经验库']

// 关联规则
const kbRules = ref([])

// ---------------- 模块 ----------------
const loadModules = async () => {
  moduleTree.value = await apiConfigModules()
}

const saveModule = async () => {
  const f = moduleForm.value
  if (!f.project.trim() || !f.module.trim() || !f.submodule.trim()) {
    ElMessage.warning('项目、模块、子模块均不能为空')
    return
  }
  await apiConfigModuleCreate(f)
  ElMessage.success('模块已新增')
  moduleForm.value = { project: '', module: '', submodule: '' }
  await loadModules()
}

const toggleModule = async (s) => {
  await apiConfigModuleToggle(s.id)
  ElMessage.success(s.enabled ? '已停用' : '已启用')
  await loadModules()
}

// ---------------- 大模型 ----------------
const loadLlms = async () => {
  llms.value = await apiConfigLlm()
  if (current.value) {
    const updated = llms.value.find((l) => l.id === current.value.id)
    if (updated) {
      selectLlm(updated)
    }
  } else if (llms.value.length) {
    selectLlm(llms.value[0])
  }
}

const selectLlm = (row) => {
  current.value = row
  llmForm.value = {
    baseUrl: row.baseUrl || '',
    modelName: row.modelName || '',
    apiKey: '',
    temperature: String(row.temperature ?? ''),
    timeoutSeconds: String(row.timeoutSeconds ?? ''),
    maxOutput: String(row.maxOutput ?? ''),
    enabled: !!row.enabled
  }
}

const saveLlm = async () => {
  await apiConfigLlmUpdate(current.value.id, llmForm.value)
  ElMessage.success('已保存（API Key 加密存储，数据库仅记录掩码）')
  await loadLlms()
}

const testLlm = async () => {
  testing.value = true
  try {
    const res = await apiConfigLlmTest(current.value.id)
    if (res.ok) {
      ElMessage.success(`连接成功：${res.message || 'OK'}`)
    } else {
      ElMessage.error(`连接失败：${res.message || '未知错误'}`)
    }
  } finally {
    testing.value = false
  }
}

// ---------------- 模板 ----------------
const loadTemplates = async () => {
  const list = await apiConfigTemplates({ taskType: tplTaskType.value })
  tplFields.value = (list || []).map((t) => ({
    fieldName: t.fieldName,
    fieldLabel: t.fieldLabel,
    required: !!t.requiredFlag,
    sortOrder: t.sortOrder
  }))
}

const switchTemplate = (v) => {
  tplTaskType.value = v
  loadTemplates()
}

const addField = () => {
  tplFields.value.push({ fieldName: '', fieldLabel: '', required: false, sortOrder: tplFields.value.length + 1 })
}

const removeField = (idx) => {
  tplFields.value.splice(idx, 1)
}

const saveTemplates = async () => {
  const fields = tplFields.value
    .map((f, i) => ({
      fieldName: (f.fieldName || '').trim() || (f.fieldLabel || '').trim(),
      fieldLabel: f.fieldLabel,
      required: f.required,
      sortOrder: i + 1
    }))
    .filter((f) => f.fieldName)
  if (!fields.length) {
    ElMessage.warning('至少保留一个字段')
    return
  }
  await apiConfigTemplateUpdate(tplTaskType.value, fields)
  ElMessage.success('模板已保存（全量同步）')
  await loadTemplates()
}

// ---------------- 关联规则 ----------------
const loadKbRules = async () => {
  kbRules.value = await apiConfigKbRules()
}

const toggleKbCategory = async (g, c) => {
  const has = g.categories.includes(c)
  const categories = has ? g.categories.filter((x) => x !== c) : [...g.categories, c]
  await apiConfigKbRuleSave(g.mainTaskType, categories)
  g.categories = categories
  ElMessage.success(`「${typeName(g.mainTaskType)}」已${has ? '取消关联' : '关联'}${c}`)
}

onMounted(async () => {
  await Promise.all([loadModules(), loadLlms(), loadTemplates(), loadKbRules()])
})
</script>
