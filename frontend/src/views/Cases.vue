<template>
  <div class="page">
    <h2 class="page-title">用例库</h2>
    <p class="page-desc">三级模块组织的用例资产：优先级徽章、版本、复制与编辑升版，支持从需求上下文智能生成草稿。</p>

    <el-row :gutter="12">
      <!-- 左侧模块树 -->
      <el-col :span="6">
        <el-card shadow="never" class="tree-card">
          <div class="card-title"><el-icon color="#e6a23c"><FolderOpened /></el-icon>模块（{{ total }} 条用例）</div>
          <el-tree :data="tree" node-key="name" default-expand-all :expand-on-click-node="false"
                   @node-click="onTreeNodeClick">
            <template #default="{ data }">
              <span class="tree-node">
                <span>{{ data.name }}</span>
                <el-tag size="small" type="info" effect="plain">{{ data.count }}</el-tag>
              </span>
            </template>
          </el-tree>
        </el-card>
      </el-col>

      <!-- 右侧列表 -->
      <el-col :span="18">
        <el-card shadow="never" class="mb12">
          <div class="search-bar">
            <el-input v-model="query.keyword" placeholder="搜索标题 / 步骤" clearable style="width: 220px"
                      @keyup.enter="load" @clear="load" />
            <el-select v-model="query.priority" placeholder="优先级" clearable style="width: 110px" @change="load">
              <el-option v-for="p in ['P0', 'P1', 'P2']" :key="p" :label="p" :value="p" />
            </el-select>
            <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px" @change="load">
              <el-option v-for="s in ['未执行', '通过', '失败', '阻塞']" :key="s" :label="s" :value="s" />
            </el-select>
            <div class="spacer" />
            <el-button type="warning" class="tp-btn-primary" @click="openGenerate">智能生成</el-button>
            <el-button type="warning" plain @click="openCreate">新增用例</el-button>
          </div>
        </el-card>

        <el-card shadow="never">
          <el-table :data="rows" stripe v-loading="loading">
            <el-table-column prop="caseNo" label="编号" width="100" />
            <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
            <el-table-column label="模块" min-width="160">
              <template #default="{ row }">{{ shortModule(row) }}</template>
            </el-table-column>
            <el-table-column label="优先级" width="80" align="center">
              <template #default="{ row }">
                <span class="tp-risk" :class="'tp-risk-' + row.priority">{{ row.priority }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="type" label="类型" width="80" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <span class="tp-status" :class="statusClass(row.status)">{{ row.status }}</span>
              </template>
            </el-table-column>
            <el-table-column label="版本" width="70" align="center">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">v{{ row.version }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="linkedBugNo" label="关联Bug" width="100" />
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
                <el-button link type="primary" size="small" @click="copyCase(row)">复制</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total"
                           layout="total, prev, pager, next" @current-change="load" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="editId ? '编辑用例（保存后升版）' : '新增用例'" width="680px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="用例标题" />
        </el-form-item>
        <el-row :gutter="10">
          <el-col :span="8">
            <el-form-item label="项目">
              <el-input v-model="form.projectName" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="模块">
              <el-input v-model="form.moduleName" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="子模块">
              <el-input v-model="form.submoduleName" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="8">
            <el-form-item label="优先级">
              <el-select v-model="form.priority" style="width: 100%">
                <el-option v-for="p in ['P0', 'P1', 'P2']" :key="p" :label="p" :value="p" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="类型">
              <el-select v-model="form.type" style="width: 100%">
                <el-option v-for="t in ['功能', '异常', '边界', '性能', '安全']" :key="t" :label="t" :value="t" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="关联Bug">
              <el-input v-model="form.linkedBugNo" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="前置条件">
          <el-input v-model="form.preconditions" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="测试步骤">
          <el-input v-model="form.steps" type="textarea" :rows="4" placeholder="每行一步" />
        </el-form-item>
        <el-form-item label="预期结果">
          <el-input v-model="form.expected" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="测试数据">
          <el-input v-model="form.testData" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="warning" class="tp-btn-primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 智能生成弹窗 -->
    <el-dialog v-model="genVisible" title="智能生成用例草稿" width="720px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="需求上下文" required>
          <el-input v-model="genContext" type="textarea" :rows="6"
                    placeholder="粘贴需求描述 / 问题上下文，引擎将拆解测试点并覆盖正常、异常、边界场景生成结构化用例草稿" />
        </el-form-item>
      </el-form>
      <div class="gen-actions">
        <el-button type="warning" class="tp-btn-primary" :loading="generating" @click="generate">生成草稿</el-button>
        <span class="gen-tip">草稿不直接入库，逐条确认后保存</span>
      </div>
      <el-divider v-if="drafts.length" content-position="left">草稿（{{ drafts.length }} 条）</el-divider>
      <div v-for="(d, i) in drafts" :key="i" class="draft-item">
        <div class="draft-head">
          <span class="tp-risk" :class="'tp-risk-' + d.priority">{{ d.priority }}</span>
          <span class="draft-title">{{ d.title }}</span>
          <el-button size="small" type="warning" plain @click="saveDraft(d)">入库</el-button>
        </div>
        <div class="draft-body">
          <div v-if="d.preconditions"><b>前置：</b>{{ d.preconditions }}</div>
          <div><b>步骤：</b><span style="white-space: pre-line">{{ d.steps }}</span></div>
          <div><b>预期：</b>{{ d.expected }}</div>
        </div>
      </div>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="detail ? detail.caseNo + ' · ' + detail.title : ''" size="46%">
      <template v-if="detail">
        <el-descriptions :column="2" border size="small" class="mb12">
          <el-descriptions-item label="优先级">
            <span class="tp-risk" :class="'tp-risk-' + detail.priority">{{ detail.priority }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="类型">{{ detail.type }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <span class="tp-status" :class="statusClass(detail.status)">{{ detail.status }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="版本">v{{ detail.version }}</el-descriptions-item>
          <el-descriptions-item label="模块" :span="2">{{ detail.projectName }} / {{ detail.moduleName }} / {{ detail.submoduleName }}</el-descriptions-item>
          <el-descriptions-item label="关联Bug" :span="2">{{ detail.linkedBugNo || '无' }}</el-descriptions-item>
        </el-descriptions>
        <h4>前置条件</h4>
        <p class="detail-text">{{ detail.preconditions || '无' }}</p>
        <h4>测试步骤</h4>
        <p class="detail-text pre">{{ detail.steps || '无' }}</p>
        <h4>预期结果</h4>
        <p class="detail-text">{{ detail.expected || '无' }}</p>
        <h4 v-if="detail.testData">测试数据</h4>
        <p v-if="detail.testData" class="detail-text">{{ detail.testData }}</p>
        <div class="detail-actions">
          <el-button @click="copyCase(detail)">复制用例</el-button>
          <el-button type="warning" plain @click="openEdit(detail)">编辑（升版）</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  apiCases, apiCaseTree, apiCaseCreate, apiCaseUpdate, apiCaseCopy, apiCaseGenerateDrafts
} from '../api'

const query = ref({ keyword: '', priority: '', status: '', moduleName: '', submoduleName: '', page: 1, size: 10 })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const tree = ref([])

const formVisible = ref(false)
const editId = ref(null)
const form = ref({})

const genVisible = ref(false)
const genContext = ref('')
const generating = ref(false)
const drafts = ref([])

const detailVisible = ref(false)
const detail = ref(null)

const statusClass = (s) => s === '通过' ? 'tp-status-ok' : s === '失败' || s === '阻塞' ? 'tp-status-block' : 'tp-status-info'

const shortModule = (row) => {
  const parts = [row.moduleName, row.submoduleName].filter(Boolean)
  return parts.join(' / ') || '未分组'
}

const load = async () => {
  loading.value = true
  try {
    const data = await apiCases(query.value)
    rows.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const loadTree = async () => {
  tree.value = await apiCaseTree()
}

const onTreeNodeClick = (node) => {
  query.value.moduleName = ''
  query.value.submoduleName = ''
  const level = getNodeLevel(node)
  if (level === 1) {
    // 项目级：不清模块（仅项目分组）
  } else if (level === 2) {
    query.value.moduleName = node.name
  } else if (level === 3) {
    query.value.submoduleName = node.name
    // 找父模块
    for (const p of tree.value) {
      for (const m of p.modules || []) {
        if ((m.submodules || []).some((s) => s.name === node.name)) {
          query.value.moduleName = m.name
        }
      }
    }
  }
  query.value.page = 1
  load()
}

const getNodeLevel = (node) => {
  if (node.submodules) return node.modules ? 1 : 2
  return 3
}

const emptyForm = () => ({
  title: '', projectName: '电商平台', moduleName: '', submoduleName: '',
  priority: 'P1', type: '功能', preconditions: '', steps: '', expected: '', testData: '', linkedBugNo: ''
})

const openCreate = () => {
  editId.value = null
  form.value = emptyForm()
  formVisible.value = true
}

const openEdit = (row) => {
  editId.value = row.id
  form.value = {
    title: row.title, projectName: row.projectName, moduleName: row.moduleName,
    submoduleName: row.submoduleName, priority: row.priority, type: row.type,
    preconditions: row.preconditions, steps: row.steps, expected: row.expected,
    testData: row.testData, linkedBugNo: row.linkedBugNo
  }
  detailVisible.value = false
  formVisible.value = true
}

const save = async () => {
  if (!form.value.title.trim()) {
    ElMessage.warning('请填写用例标题')
    return
  }
  if (editId.value) {
    const saved = await apiCaseUpdate(editId.value, form.value)
    ElMessage.success(`已保存并升版至 v${saved.version}`)
  } else {
    await apiCaseCreate(form.value)
    ElMessage.success('用例已入库')
  }
  formVisible.value = false
  await load()
  await loadTree()
}

const copyCase = async (row) => {
  const c = await apiCaseCopy(row.id)
  ElMessage.success(`已复制为 ${c.caseNo}（${c.title}）`)
  await load()
  await loadTree()
}

const openGenerate = () => {
  genVisible.value = true
  drafts.value = []
}

const generate = async () => {
  if (!genContext.value.trim()) {
    ElMessage.warning('请粘贴需求或问题上下文')
    return
  }
  generating.value = true
  try {
    drafts.value = await apiCaseGenerateDrafts({ context: genContext.value })
    if (!drafts.value.length) {
      ElMessage.info('未生成草稿，请补充上下文')
    }
  } finally {
    generating.value = false
  }
}

const saveDraft = async (d) => {
  await apiCaseCreate({
    title: d.title, projectName: '电商平台', moduleName: d.moduleName || '',
    submoduleName: d.submoduleName || '', priority: d.priority || 'P1', type: d.type || '功能',
    preconditions: d.preconditions || '', steps: d.steps || '',
    expected: d.expected || '', testData: d.testData || '', linkedBugNo: ''
  })
  ElMessage.success('草稿已入库')
  drafts.value = drafts.value.filter((x) => x !== d)
  await load()
  await loadTree()
}

const openDetail = (row) => {
  detail.value = row
  detailVisible.value = true
}

onMounted(() => {
  load()
  loadTree()
})
</script>

<style scoped>
.mb12 {
  margin-bottom: 12px;
}

.tree-card {
  min-height: 400px;
}

.tree-node {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding-right: 8px;
}

.search-bar {
  display: flex;
  gap: 8px;
  align-items: center;
}

.spacer {
  flex: 1;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.gen-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.gen-tip {
  color: #909399;
  font-size: 12px;
}

.draft-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 10px;
}

.draft-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.draft-title {
  font-weight: 600;
  flex: 1;
}

.draft-body {
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
}

.detail-text {
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
  background: #fafafa;
  border-radius: 6px;
  padding: 10px 12px;
}

.detail-text.pre {
  white-space: pre-line;
}

.detail-actions {
  margin-top: 16px;
  display: flex;
  gap: 8px;
}
</style>
