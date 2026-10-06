<template>
  <div class="page">
    <h2 class="page-title">配置中心</h2>
    <p class="page-desc">项目与模块、大模型厂商、输出格式模板与任务关联规则的集中配置。</p>

    <el-card shadow="never">
      <el-tabs v-model="tab">
        <!-- Tab 1: 项目与模块 -->
        <el-tab-pane label="项目与模块" name="modules">
          <div class="tab-toolbar">
            <span class="tip">停用后不再出现在新建表单的模块下拉中</span>
            <div class="spacer" />
            <el-button type="warning" plain size="small" @click="moduleFormVisible = true">新增模块</el-button>
          </div>
          <div v-for="p in moduleTree" :key="p.project" class="tree-block">
            <div class="tree-project">
              <el-icon color="#e6a23c"><Folder /></el-icon>{{ p.project }}
            </div>
            <div v-for="m in p.modules" :key="m.module" class="tree-module">
              <span class="module-name">{{ m.module }}</span>
              <div class="sub-list">
                <div v-for="s in m.submodules" :key="s.id" class="sub-item">
                  <span>{{ s.submodule }}</span>
                  <span class="sub-right">
                    <el-tag size="small" :type="s.enabled ? 'success' : 'info'" effect="plain">
                      {{ s.enabled ? '启用' : '停用' }}
                    </el-tag>
                    <el-button link :type="s.enabled ? 'danger' : 'success'" size="small" @click="toggleModule(s)">
                      {{ s.enabled ? '停用' : '启用' }}
                    </el-button>
                  </span>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- Tab 2: 大模型 -->
        <el-tab-pane label="大模型" name="llm">
          <el-alert type="info" :closable="false" class="mb12"
                    title="启用一家厂商后引擎将优先调用 LLM；未启用或调用失败自动降级规则引擎，功能不受影响。API Key 仅本地保存，列表仅显示掩码。" />
          <el-table :data="llms" stripe>
            <el-table-column prop="vendorName" label="厂商" width="120" />
            <el-table-column prop="modelName" label="模型" min-width="160">
              <template #default="{ row }">{{ row.modelName || '未配置' }}</template>
            </el-table-column>
            <el-table-column prop="baseUrl" label="Base URL" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">{{ row.baseUrl || '未配置' }}</template>
            </el-table-column>
            <el-table-column prop="apiKey" label="API Key" width="170">
              <template #default="{ row }">{{ row.apiKey || '未配置' }}</template>
            </el-table-column>
            <el-table-column label="温度" width="70" align="center">
              <template #default="{ row }">{{ row.temperature }}</template>
            </el-table-column>
            <el-table-column label="启用" width="80" align="center">
              <template #default="{ row }">
                <el-switch :model-value="row.enabled" @change="(v) => toggleLlm(row, v)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openLlmForm(row)">编辑</el-button>
                <el-button link type="warning" size="small" :loading="testingId === row.id" @click="testLlm(row)">测试连接</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab 3: 格式模板 -->
        <el-tab-pane label="格式模板" name="templates">
          <div class="tab-toolbar">
            <el-select v-model="tplTaskType" style="width: 200px" @change="loadTemplates">
              <el-option v-for="t in taskTypes" :key="t.value" :value="t.value" :label="t.label" />
            </el-select>
            <div class="spacer" />
            <el-button type="warning" plain size="small" @click="addField">添加字段</el-button>
            <el-button type="warning" class="tp-btn-primary" size="small" @click="saveTemplates">保存模板</el-button>
          </div>
          <el-alert type="warning" :closable="false" class="mb12"
                    title="全量同步警示：保存将按当前列表全量同步；被删除的字段仅从模板中隐藏（visible=0），历史执行快照不受影响。" />
          <el-table :data="tplFields" stripe>
            <el-table-column label="字段名" min-width="200">
              <template #default="{ row }">
                <el-input v-model="row.fieldName" size="small" placeholder="英文下划线命名" />
              </template>
            </el-table-column>
            <el-table-column label="显示名" min-width="160">
              <template #default="{ row }">
                <el-input v-model="row.fieldLabel" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="必填" width="80" align="center">
              <template #default="{ row }">
                <el-switch v-model="row.required" />
              </template>
            </el-table-column>
            <el-table-column label="排序" width="90" align="center">
              <template #default="{ row }">{{ row.sortOrder }}</template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ $index }">
                <el-button link type="danger" size="small" @click="removeField($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab 4: 关联规则 -->
        <el-tab-pane label="关联规则" name="rules">
          <div v-for="g in rules" :key="g.mainTaskType" class="rule-block">
            <div class="rule-main">
              <el-icon color="#e6a23c"><Connection /></el-icon>
              {{ g.mainTaskName }} 的辅助任务
            </div>
            <el-table :data="g.rules" size="small" stripe>
              <el-table-column label="辅助任务" min-width="160">
                <template #default="{ row }">{{ typeName(row.auxTaskType) }}</template>
              </el-table-column>
              <el-table-column label="顺序" width="120">
                <template #default="{ row }">
                  <el-input-number v-model="row.sortOrder" size="small" :min="1" :max="9"
                                   @change="saveRule(row)" />
                </template>
              </el-table-column>
              <el-table-column label="启用" width="90" align="center">
                <template #default="{ row }">
                  <el-switch v-model="row.enabled" @change="saveRule(row)" />
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 新增模块弹窗 -->
    <el-dialog v-model="moduleFormVisible" title="新增三级模块" width="480px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="项目" required>
          <el-input v-model="moduleForm.project" placeholder="如：电商平台" />
        </el-form-item>
        <el-form-item label="模块" required>
          <el-input v-model="moduleForm.module" placeholder="如：支付中心" />
        </el-form-item>
        <el-form-item label="子模块" required>
          <el-input v-model="moduleForm.submodule" placeholder="如：订单同步" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="moduleFormVisible = false">取消</el-button>
        <el-button type="warning" class="tp-btn-primary" @click="createModule">保存</el-button>
      </template>
    </el-dialog>

    <!-- 大模型编辑弹窗 -->
    <el-dialog v-model="llmFormVisible" title="编辑大模型配置" width="520px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="Base URL">
          <el-input v-model="llmForm.baseUrl" placeholder="https://api.example.com/v1" />
        </el-form-item>
        <el-form-item label="模型名">
          <el-input v-model="llmForm.modelName" placeholder="如 gpt-4o-mini / deepseek-chat" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="llmForm.apiKey" placeholder="留空或掩码则保持原值" show-password />
        </el-form-item>
        <el-form-item label="温度">
          <el-input v-model="llmForm.temperature" placeholder="0 ~ 1，默认 0.2" />
        </el-form-item>
        <el-form-item label="超时（秒）">
          <el-input v-model="llmForm.timeoutSeconds" placeholder="默认 60" />
        </el-form-item>
        <el-form-item label="最大输出">
          <el-input v-model="llmForm.maxOutput" placeholder="默认 4096" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="llmFormVisible = false">取消</el-button>
        <el-button type="warning" class="tp-btn-primary" @click="saveLlm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  apiConfigModules, apiConfigModuleCreate, apiConfigModuleToggle,
  apiConfigLlm, apiConfigLlmUpdate, apiConfigLlmTest,
  apiConfigTemplates, apiConfigTemplateUpdate,
  apiConfigRules, apiConfigRuleUpdate
} from '../api'

const NAMES = {
  testcase_gen: '测试用例生成', bug_analysis: 'Bug 分析', log_triage: '日志排查',
  sql_analysis: 'SQL 分析', regression_list: '回归清单', test_report: '测试报告', prompt_test: 'Prompt 测试'
}

const taskTypes = Object.entries(NAMES).map(([value, label]) => ({ value, label }))
const typeName = (t) => NAMES[t] || t

const tab = ref('modules')

// 模块
const moduleTree = ref([])
const moduleFormVisible = ref(false)
const moduleForm = ref({ project: '', module: '', submodule: '' })

// 大模型
const llms = ref([])
const llmFormVisible = ref(false)
const llmForm = ref({})
const editingLlmId = ref(null)
const testingId = ref(null)

// 模板
const tplTaskType = ref('bug_analysis')
const tplFields = ref([])

// 规则
const rules = ref([])

// ---------------- 模块 ----------------
const loadModules = async () => {
  moduleTree.value = await apiConfigModules()
}

const createModule = async () => {
  const f = moduleForm.value
  if (!f.project.trim() || !f.module.trim() || !f.submodule.trim()) {
    ElMessage.warning('项目、模块、子模块均不能为空')
    return
  }
  await apiConfigModuleCreate(f)
  ElMessage.success('模块已新增')
  moduleFormVisible.value = false
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
}

const openLlmForm = (row) => {
  editingLlmId.value = row.id
  llmForm.value = {
    baseUrl: row.baseUrl || '',
    modelName: row.modelName || '',
    apiKey: '',
    temperature: String(row.temperature ?? ''),
    timeoutSeconds: String(row.timeoutSeconds ?? ''),
    maxOutput: String(row.maxOutput ?? '')
  }
  llmFormVisible.value = true
}

const saveLlm = async () => {
  await apiConfigLlmUpdate(editingLlmId.value, llmForm.value)
  ElMessage.success('已保存')
  llmFormVisible.value = false
  await loadLlms()
}

const toggleLlm = async (row, enabled) => {
  await apiConfigLlmUpdate(row.id, { enabled })
  ElMessage.success(enabled ? `已启用 ${row.vendorName}，引擎将优先调用 LLM` : '已停用，切回规则引擎')
  await loadLlms()
}

const testLlm = async (row) => {
  testingId.value = row.id
  try {
    const res = await apiConfigLlmTest(row.id)
    if (res.ok) {
      ElMessage.success(`连接成功：${res.message || 'OK'}`)
    } else {
      ElMessage.error(`连接失败：${res.message || '未知错误'}`)
    }
  } finally {
    testingId.value = null
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

const addField = () => {
  tplFields.value.push({ fieldName: '', fieldLabel: '', required: false, sortOrder: tplFields.value.length + 1 })
}

const removeField = (idx) => {
  tplFields.value.splice(idx, 1)
}

const saveTemplates = async () => {
  const fields = tplFields.value
    .map((f, i) => ({ ...f, sortOrder: i + 1 }))
    .filter((f) => f.fieldName.trim())
  if (!fields.length) {
    ElMessage.warning('至少保留一个字段')
    return
  }
  await apiConfigTemplateUpdate(tplTaskType.value, fields)
  ElMessage.success('模板已保存（全量同步）')
  await loadTemplates()
}

// ---------------- 规则 ----------------
const loadRules = async () => {
  rules.value = await apiConfigRules()
}

const saveRule = async (row) => {
  await apiConfigRuleUpdate(row.id, { enabled: row.enabled, sortOrder: row.sortOrder })
  ElMessage.success('规则已更新')
  await loadRules()
}

onMounted(async () => {
  await Promise.all([loadModules(), loadLlms(), loadTemplates(), loadRules()])
})
</script>

<style scoped>
.mb12 {
  margin-bottom: 12px;
}

.tab-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.tab-toolbar .tip {
  color: #909399;
  font-size: 12px;
}

.spacer {
  flex: 1;
}

.tree-block {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 12px;
}

.tree-project {
  font-weight: 600;
  font-size: 15px;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
}

.tree-module {
  margin-left: 18px;
  margin-bottom: 8px;
}

.module-name {
  font-weight: 600;
  font-size: 13px;
  color: #606266;
}

.sub-list {
  margin-top: 4px;
}

.sub-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 5px 12px;
  border-left: 2px solid #ebeef5;
  margin-left: 10px;
  font-size: 13px;
  color: #606266;
}

.sub-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rule-block {
  margin-bottom: 16px;
}

.rule-main {
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}
</style>
