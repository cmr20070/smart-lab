<template>
  <div class="page-container">
    <div class="page-hero">
      <div class="hero-left">
        <div class="hero-icon">
          <el-icon :size="26"><Bell /></el-icon>
        </div>
        <div>
          <h2 class="hero-title">{{ isAdmin || isLabAdmin ? '公告管理' : '公告通知' }}</h2>
          <p class="hero-sub">{{ isAdmin || isLabAdmin ? '管理公告通知，向全体师生发布信息' : '查看最新公告通知' }}</p>
        </div>
      </div>
      <div class="hero-actions" v-if="isAdmin || isLabAdmin">
        <el-button type="primary" :icon="Plus" @click="openPublishDialog">发布公告</el-button>
      </div>
    </div>

    <div v-if="!loading && tableData.length > 0" class="type-filter-bar">
      <el-button v-for="t in typeOptions" :key="t.value"
        :type="filterType === t.value ? 'primary' : ''"
        size="small" plain round
        @click="filterType = filterType === t.value ? '' : t.value"
      >{{ t.label }}</el-button>
    </div>

    <div v-if="loading" class="grid-skeleton">
      <div v-for="i in 6" :key="i" class="sk-item">
        <div class="sk-line w60"></div>
        <div class="sk-line w80"></div>
        <div class="sk-line w40"></div>
      </div>
    </div>

    <div v-else-if="filteredTableData.length === 0" class="empty-hint">
      <el-icon :size="48"><Bell /></el-icon>
      <p>暂无公告通知</p>
    </div>

    <template v-else>
      <div v-if="isAdmin || isLabAdmin" class="table-toolbar">
        <div class="tt-left">
          <el-checkbox v-model="selectAll" :indeterminate="isIndeterminate" @change="handleSelectAllChange">全选</el-checkbox>
          <el-button size="small" @click="handleInvertSelect">反选</el-button>
          <span v-if="selectedIds.length > 0" class="selected-count">已选 {{ selectedIds.length }} 项</span>
        </div>
        <div class="tt-right">
          <el-button v-if="isAdmin && selectedIds.length > 0" size="small" type="danger" plain @click="handleBatchDelete">批量删除</el-button>
        </div>
      </div>

      <div v-if="isAdmin || isLabAdmin" class="table-wrap">
        <el-table
          :data="pagedFilteredData"
          stripe
          border
          style="width:100%"
          @selection-change="handleSelectionChange"
          ref="annTableRef"
          row-key="id"
        >
          <el-table-column type="selection" width="45" />
          <el-table-column label="序号" width="60" align="center">
            <template #default="{ $index }">
              {{ (tablePage - 1) * tablePageSize + $index + 1 }}
            </template>
          </el-table-column>
          <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
          <el-table-column label="类型" width="90" align="center">
            <template #default="{ row }">
              <span class="ac-type-tag" :style="{ background: typeColor(row.type) }">{{ typeLabel(row.type) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="content-ellipsis">{{ row.content }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="publisherName" label="发布人" width="100" align="center" />
          <el-table-column label="发布时间" width="160" align="center">
            <template #default="{ row }">
              <span class="time-text">{{ formatTime(row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="plain">
                {{ row.status === 1 ? '已发布' : '已撤回' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link size="small" @click="showDetail(row)">查看</el-button>
              <el-button v-if="canEditRow(row)" link type="primary" size="small" :icon="Edit" @click="handleEdit(row)" :disabled="row.status === 0">编辑</el-button>
              <el-button v-if="row.status === 1 && row.publisherId === userStore.userInfo?.id" link type="warning" size="small" @click="handleWithdraw(row)">撤回</el-button>
              <el-button v-if="isAdmin" link type="danger" size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-else class="announce-card-list">
        <div
          v-for="item in filteredTableData"
          :key="item.id"
          class="announce-card"
          @click="showDetail(item)"
          :class="{ withdrawn: item.status === 0 }"
        >
          <div class="ac-left">
            <div class="ac-badge" :class="item.status === 1 ? 'active' : 'inactive'">
              <el-icon :size="18"><Bell /></el-icon>
            </div>
          </div>
          <div class="ac-body">
            <div class="ac-header">
              <h3 class="ac-title">{{ item.title }}</h3>
              <div class="ac-tags">
                <span class="ac-type-tag" :style="{ background: typeColor(item.type) }">{{ typeLabel(item.type) }}</span>
                <el-tag :type="item.status === 1 ? 'success' : 'info'" size="small" effect="plain">
                  {{ item.status === 1 ? '已发布' : '已撤回' }}
                </el-tag>
              </div>
            </div>
            <p class="ac-content">{{ item.content }}</p>
            <div class="ac-footer">
              <div class="ac-meta">
                <span class="ac-author">
                  <el-icon :size="13"><User /></el-icon> {{ item.publisherName }}
                </span>
                <span class="ac-time">
                  <el-icon :size="13"><Clock /></el-icon> {{ formatTime(item.createTime) }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <div class="pagination-wrap" v-if="(isAdmin || isLabAdmin ? filteredTableData.length : total) > 0">
      <el-pagination
        v-if="isAdmin || isLabAdmin"
        v-model:current-page="tablePage"
        v-model:page-size="tablePageSize"
        :total="filteredTableData.length"
        :page-sizes="[10, 20, 30, 50]"
        layout="total, sizes, prev, pager, next"
        background
      />
      <el-pagination
        v-else
        v-model:current-page="page"
        v-model:page-size="size"
        :page-sizes="[6, 12, 18]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchData"
        @current-change="fetchData"
        background
      />
    </div>

    <el-dialog v-model="publishVisible" :title="isEditing ? '编辑公告' : '发布公告'" width="640px" :close-on-click-modal="false" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="公告类型" prop="type">
          <el-select v-model="form.type" placeholder="选择公告类型" style="width: 100%">
            <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="请输入公告内容" maxlength="2000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishVisible = false; cancelEdit()">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEditing ? '保存修改' : '立即发布' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="公告详情" width="600px" :close-on-click-modal="false">
      <div v-if="detailRow" class="detail-wrap">
        <h3 class="detail-title">{{ detailRow.title }}</h3>
        <div class="detail-meta">
          <span class="ac-type-tag" :style="{ background: typeColor(detailRow.type) }">{{ typeLabel(detailRow.type) }}</span>
          <span><el-icon :size="14"><User /></el-icon> 发布人：{{ detailRow.publisherName }}</span>
          <span><el-icon :size="14"><Clock /></el-icon> 时间：{{ formatTime(detailRow.createTime) }}</span>
          <el-tag :type="detailRow.status === 1 ? 'success' : 'info'" size="small">
            {{ detailRow.status === 1 ? '已发布' : '已撤回' }}
          </el-tag>
        </div>
        <div class="detail-divider"></div>
        <div class="detail-content">{{ detailRow.content }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { getAnnouncementList, getAllAnnouncementList, publishAnnouncement, updateAnnouncement, withdrawAnnouncement, deleteAnnouncement } from '@/api/announcement'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, Plus, Edit, Delete, User, Clock } from '@element-plus/icons-vue'

const userStore = useUserStore()
const role = computed(() => userStore.userInfo?.role || 'STUDENT')
const isAdmin = computed(() => role.value === 'ADMIN')
const isLabAdmin = computed(() => role.value === 'LAB_ADMIN')

const publishVisible = ref(false)
const formRef = ref(null)
const form = ref({ title: '', content: '', type: 'GENERAL' })
const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
  type: [{ required: true, message: '请选择公告类型', trigger: 'change' }]
}
const isEditing = ref(false)
const editingId = ref(null)
const submitting = ref(false)

const tableData = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(6)
const total = ref(0)
const detailVisible = ref(false)
const detailRow = ref(null)
const filterType = ref('')

const selectedIds = ref([])
const selectAll = ref(false)
const isIndeterminate = ref(false)
const annTableRef = ref(null)
const tablePage = ref(1)
const tablePageSize = ref(10)

const typeOptions = [
  { value: 'GENERAL', label: '通知' },
  { value: 'NOTICE', label: '提醒' },
  { value: 'POLICY', label: '制度' },
  { value: 'MAINTENANCE', label: '维护' },
  { value: 'HOLIDAY', label: '假期' },
]

const typeColor = (t) => {
  const map = { GENERAL: '#8b5cf6', NOTICE: '#ef4444', POLICY: '#2563eb', MAINTENANCE: '#f59e0b', HOLIDAY: '#10b981' }
  return map[t] || '#8b5cf6'
}
const typeLabel = (t) => {
  const map = { GENERAL: '通知', NOTICE: '提醒', POLICY: '制度', MAINTENANCE: '维护', HOLIDAY: '假期' }
  return map[t] || '通知'
}

const filteredTableData = computed(() => {
  if (!filterType.value) return tableData.value
  return tableData.value.filter(d => d.type === filterType.value)
})

const pagedFilteredData = computed(() => {
  const start = (tablePage.value - 1) * tablePageSize.value
  return filteredTableData.value.slice(start, start + tablePageSize.value)
})

const canEditRow = (row) => {
  if (isAdmin.value || isLabAdmin.value) return true
  return row.publisherId === userStore.userInfo?.id
}

const openPublishDialog = () => {
  isEditing.value = false
  editingId.value = null
  form.value = { title: '', content: '', type: 'GENERAL' }
  publishVisible.value = true
}

const fetchData = async () => {
  loading.value = true
  try {
    if (isAdmin.value || isLabAdmin.value) {
      const res = await getAllAnnouncementList({ page: 1, size: 999 })
      tableData.value = res.data?.records || []
      total.value = res.data?.total || 0
    } else {
      const res = await getAnnouncementList({ page: page.value, size: size.value })
      tableData.value = res.data?.records || []
      total.value = res.data?.total || 0
    }
  } catch (e) {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEditing.value) {
      await updateAnnouncement(editingId.value, { title: form.value.title, content: form.value.content, type: form.value.type })
      ElMessage.success('公告已更新')
    } else {
      await publishAnnouncement({ title: form.value.title, content: form.value.content, type: form.value.type })
      ElMessage.success('公告已发布')
    }
    form.value = { title: '', content: '', type: 'GENERAL' }
    isEditing.value = false
    editingId.value = null
    publishVisible.value = false
    fetchData()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleEdit = (row) => {
  isEditing.value = true
  editingId.value = row.id
  form.value = { title: row.title, content: row.content, type: row.type || 'GENERAL' }
  publishVisible.value = true
}

const cancelEdit = () => {
  isEditing.value = false
  editingId.value = null
  form.value = { title: '', content: '', type: 'GENERAL' }
}

const handleWithdraw = async (row) => {
  try {
    await ElMessageBox.confirm('确定撤回该公告？', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    await withdrawAnnouncement(row.id)
    ElMessage.success('公告已撤回')
    fetchData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该公告？', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    await deleteAnnouncement(row.id)
    ElMessage.success('公告已删除')
    selectedIds.value = []
    fetchData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(r => r.id)
  const totalCount = filteredTableData.value.length
  selectAll.value = totalCount > 0 && selection.length === totalCount
  isIndeterminate.value = selection.length > 0 && selection.length < totalCount
}

const handleSelectAllChange = (val) => {
  if (val) {
    filteredTableData.value.forEach(row => annTableRef.value?.toggleRowSelection(row, true))
  } else {
    annTableRef.value?.clearSelection()
  }
}

const handleInvertSelect = () => {
  filteredTableData.value.forEach(row => {
    if (row.status === 0) return
    annTableRef.value?.toggleRowSelection(row)
  })
}

const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择公告')
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 条公告？`, '批量删除', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    for (const id of selectedIds.value) {
      await deleteAnnouncement(id)
    }
    ElMessage.success(`已删除 ${selectedIds.value.length} 条公告`)
    selectedIds.value = []
    fetchData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

const showDetail = (row) => {
  detailRow.value = row
  detailVisible.value = true
}

const formatTime = (t) => {
  if (!t) return '-'
  return t.replace('T', ' ')
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.page-hero { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.hero-left { display: flex; align-items: center; gap: 16px; }
.hero-icon { width: 52px; height: 52px; border-radius: 14px; background: linear-gradient(135deg, #8b5cf6, #6366f1); display: flex; align-items: center; justify-content: center; color: #fff; }
.hero-title { font-size: 20px; font-weight: 700; color: #1e293b; margin: 0 0 2px; }
.hero-sub { font-size: 13px; color: #94a3b8; margin: 0; }

.type-filter-bar { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 18px; }

.table-toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; padding: 8px 12px; background: #f8fafc; border-radius: 10px; }
.tt-left { display: flex; align-items: center; gap: 10px; }
.tt-right { display: flex; gap: 8px; }
.selected-count { font-size: 13px; color: #64748b; }

.table-wrap { margin-bottom: 16px; }
.content-ellipsis { display: block; max-width: 260px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #64748b; font-size: 13px; }
.time-text { font-size: 13px; color: #64748b; }

.ac-type-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  color: #fff;
  font-weight: 500;
  white-space: nowrap;
}

.grid-skeleton { display: grid; grid-template-columns: 1fr; gap: 12px; }
.sk-item { background: #fff; border-radius: 14px; padding: 20px 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.sk-line { height: 14px; border-radius: 6px; background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; margin-bottom: 10px; }
.sk-line.w60 { width: 60%; }
.sk-line.w80 { width: 80%; }
.sk-line.w40 { width: 40%; }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

.empty-hint { text-align: center; padding: 80px 0; color: #94a3b8; }
.empty-hint p { margin-top: 12px; font-size: 14px; }

.announce-card-list { display: flex; flex-direction: column; gap: 14px; }

.announce-card {
  display: flex; gap: 18px; background: #fff; border-radius: 16px;
  padding: 20px 24px; cursor: pointer; transition: all 0.25s;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04); border: 1px solid #f1f5f9;
}
.announce-card:hover { box-shadow: 0 8px 24px rgba(99,102,241,0.10); border-color: #e0e7ff; transform: translateY(-2px); }
.announce-card.withdrawn { opacity: 0.65; background: #fafafa; }

.ac-left { flex-shrink: 0; }
.ac-badge {
  width: 44px; height: 44px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center; color: #fff;
}
.ac-badge.active { background: linear-gradient(135deg, #8b5cf6, #6366f1); }
.ac-badge.inactive { background: #cbd5e1; }

.ac-body { flex: 1; min-width: 0; }
.ac-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.ac-title { font-size: 16px; font-weight: 600; color: #1e293b; margin: 0; line-height: 1.4; }
.ac-tags { flex-shrink: 0; }
.ac-content {
  font-size: 14px; color: #64748b; margin: 8px 0 0;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
  line-height: 1.6;
}
.ac-footer { display: flex; align-items: center; justify-content: space-between; margin-top: 12px; }
.ac-meta { display: flex; align-items: center; gap: 16px; font-size: 13px; color: #94a3b8; }
.ac-author, .ac-time { display: flex; align-items: center; gap: 4px; }

.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 20px; }
.detail-wrap { padding: 10px 0; }
.detail-title { font-size: 20px; color: #1e293b; margin: 0 0 14px; }
.detail-meta { display: flex; align-items: center; gap: 18px; font-size: 13px; color: #94a3b8; flex-wrap: wrap; }
.detail-divider { height: 1px; background: #f1f5f9; margin: 14px 0; }
.detail-content { font-size: 15px; color: #334155; line-height: 1.9; white-space: pre-wrap; word-break: break-word; }
</style>