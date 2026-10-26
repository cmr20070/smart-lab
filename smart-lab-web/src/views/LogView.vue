<template>
  <div class="page-container">
    <div class="page-hero">
      <div class="hero-left">
        <div class="hero-icon">
          <el-icon :size="26"><Document /></el-icon>
        </div>
        <div>
          <h2 class="hero-title">操作日志</h2>
          <p class="hero-sub">追踪系统内所有关键操作，保障安全审计</p>
        </div>
      </div>
      <div class="hero-actions">
        <el-button :icon="Download" plain @click="handleExport" :disabled="!total">导出日志</el-button>
      </div>
    </div>

    <div class="module-stats">
      <div v-for="item in statCards" :key="item.key"
        class="module-card" :class="{ active: searchForm.module === item.key }"
        @click="quickFilterModule(item.key)">
        <div class="module-icon" :style="{ background: item.gradient }">
          <el-icon :size="18"><component :is="item.icon" /></el-icon>
        </div>
        <div class="module-info">
          <span class="module-num">{{ item.count }}</span>
          <span class="module-label">{{ item.label }}</span>
        </div>
      </div>
    </div>

    <div class="toolbar-card">
      <div class="search-fields">
        <el-select v-model="searchForm.action" placeholder="全部操作" clearable style="width: 130px" @change="fetchData">
          <el-option v-for="a in actionOptions" :key="a.value" :label="a.label" :value="a.value" />
        </el-select>
        <el-date-picker
          v-model="searchForm.dateRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始"
          end-placeholder="结束"
          format="YYYY-MM-DD HH:mm"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 340px"
        />
        <el-input v-model="searchForm.keyword" placeholder="搜索操作人 / 描述" clearable style="width: 220px"
          :prefix-icon="Search" @keyup.enter="fetchData" @clear="fetchData" />
        <el-button :icon="Search" type="primary" @click="fetchData">查询</el-button>
        <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
      </div>
    </div>

    <el-card class="table-card" shadow="never">
      <div class="table-toolbar">
        <div class="table-info">
          <span class="info-title">日志记录</span>
          <span class="info-count">共 <b>{{ total }}</b> 条</span>
        </div>
      </div>
      <el-table :data="tableData" v-loading="loading" style="width: 100%" highlight-current-row>
        <el-table-column label="序号" width="65" align="center">
          <template #default="{ $index }">
            <span class="seq">{{ (page - 1) * size + $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作人" width="130">
          <template #default="{ row }">
            <div class="operator-cell">
              <div class="operator-avatar">{{ row.username?.charAt(0) || 'S' }}</div>
              <div class="operator-info">
                <div class="operator-name">{{ row.username || '系统' }}</div>
                <div class="operator-sub">{{ row.ip || '-' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="模块" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="moduleTag(row.module)" effect="dark" round size="small">{{ moduleLabel(row.module) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="actionTag(row.action)" effect="plain" round size="small">{{ actionLabel(row.action) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="操作描述" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="desc-text">{{ row.description }}</span>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            <div class="time-cell">
              <el-icon :size="14" class="time-icon"><Clock /></el-icon>
              <span>{{ row.createTime }}</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager-bar">
        <el-pagination
          v-model:current-page="page" v-model:page-size="size" :total="total"
          :page-sizes="[10, 20, 50, 100]" layout="sizes, prev, pager, next, jumper"
          @size-change="fetchData" @current-change="fetchData" background
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search, Refresh, Download, Clock, Document, User, Monitor,
  OfficeBuilding, Tickets, Tools, Cpu
} from '@element-plus/icons-vue'
import { getLogList } from '@/api/log'
import request from '@/utils/request'

const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const moduleStats = ref({})

const searchForm = reactive({
  module: '',
  action: '',
  dateRange: [],
  keyword: ''
})

const actionOptions = [
  { label: '新增', value: 'add' },
  { label: '编辑', value: 'edit' },
  { label: '删除', value: 'delete' },
  { label: '申请', value: 'apply' },
  { label: '审批通过', value: 'approve' },
  { label: '审批拒绝', value: 'reject' },
  { label: '归还', value: 'return' },
  { label: '取消', value: 'cancel' },
  { label: '报修', value: 'report' },
  { label: '完成', value: 'complete' },
  { label: '报废申请', value: 'discard' },
]

const statCards = computed(() => [
  { key: '', label: '全部', icon: Document, gradient: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)', count: total.value || 0 },
  { key: 'user', label: '用户', icon: User, gradient: 'linear-gradient(135deg, #3b82f6 0%, #0ea5e9 100%)', count: moduleStats.value.user || 0 },
  { key: 'lab', label: '实验室', icon: OfficeBuilding, gradient: 'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)', count: moduleStats.value.lab || 0 },
  { key: 'device', label: '设备', icon: Monitor, gradient: 'linear-gradient(135deg, #60a5fa 0%, #2563eb 100%)', count: moduleStats.value.device || 0 },
  { key: 'borrow', label: '借用', icon: Tickets, gradient: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)', count: moduleStats.value.borrow || 0 },
  { key: 'repair', label: '维修', icon: Tools, gradient: 'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)', count: moduleStats.value.repair || 0 },
])

const moduleLabel = (m) => {
  const map = { user: '用户', lab: '实验室', device: '设备', borrow: '借用', repair: '维修' }
  return map[m] || m
}
const moduleTag = (m) => {
  const map = { user: 'primary', lab: 'success', device: 'warning', borrow: 'danger', repair: 'info' }
  return map[m] || 'info'
}
const actionLabel = (a) => {
  const map = {
    add: '新增', edit: '编辑', delete: '删除',
    approve: '审批通过', reject: '审批拒绝', return: '归还',
    apply: '申请', cancel: '取消', report: '报修',
    complete: '完成', status: '改状态', export: '导出',
    discard: '报废申请', discardApprove: '报废通过', discardReject: '报废驳回',
    enable: '启用', disable: '禁用', assign: '分配',
  }
  return map[a] || a
}
const actionTag = (a) => {
  if (['delete', 'reject', 'disable', 'discardReject'].includes(a)) return 'danger'
  if (['add', 'approve', 'enable', 'discardApprove', 'complete', 'return'].includes(a)) return 'success'
  if (['edit', 'assign', 'discard'].includes(a)) return 'warning'
  return 'primary'
}

const quickFilterModule = (key) => {
  searchForm.module = searchForm.module === key ? '' : key
  fetchData()
}

const fetchStats = async () => {
  try {
    const res = await request({ url: '/log/module-stats', method: 'get' })
    moduleStats.value = res.data || {}
  } catch {
    moduleStats.value = {}
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (searchForm.module) params.module = searchForm.module
    if (searchForm.action) params.action = searchForm.action
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startTime = searchForm.dateRange[0]
      params.endTime = searchForm.dateRange[1]
    }
    const res = await getLogList(params)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
    fetchStats()
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.module = ''
  searchForm.action = ''
  searchForm.dateRange = []
  searchForm.keyword = ''
  fetchData()
}

const handleExport = async () => {
  try {
    const params = new URLSearchParams()
    if (searchForm.module) params.append('module', searchForm.module)
    if (searchForm.action) params.append('action', searchForm.action)
    if (searchForm.keyword) params.append('keyword', searchForm.keyword)
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.append('startTime', searchForm.dateRange[0])
      params.append('endTime', searchForm.dateRange[1])
    }
    const res = await request({ url: '/log/export', method: 'get', params, responseType: 'blob' })
    const url = URL.createObjectURL(res)
    const a = document.createElement('a')
    a.href = url
    a.download = '操作日志.xlsx'
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.page-container { height: 100%; display: flex; flex-direction: column; gap: 16px; }

.page-hero {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20px 24px;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  border-radius: 20px; color: #fff;
  box-shadow: 0 10px 32px rgba(37, 99, 235, 0.3);
}
.hero-left { display: flex; align-items: center; gap: 16px; }
.hero-icon {
  width: 52px; height: 52px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
}
.hero-title { margin: 0; font-size: 22px; font-weight: 700; letter-spacing: 0.5px; }
.hero-sub { margin: 4px 0 0; font-size: 13px; opacity: 0.85; }

.module-stats {
  display: flex; gap: 12px; flex-wrap: wrap;
  background: #fff; border-radius: 14px; padding: 16px; border: 1px solid var(--border-light);
}
.module-card {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 16px; border-radius: 10px;
  background: #f8fafc;
  cursor: pointer;
  transition: all 0.25s;
  min-width: 120px;
  border: 2px solid transparent;
}
.module-card:hover { background: #f1f5f9; transform: translateY(-2px); }
.module-card.active {
  background: #f0f4ff;
  border-color: #2563eb;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.15);
}
.module-icon {
  width: 38px; height: 38px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: #fff; flex-shrink: 0;
}
.module-info { display: flex; flex-direction: column; line-height: 1.2; }
.module-num { font-size: 18px; font-weight: 700; color: #0f172a; }
.module-label { font-size: 11px; color: #64748b; }

.toolbar-card {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 18px;
  background: #fff; border-radius: 14px; border: 1px solid var(--border-light);
}
.search-fields { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }

.table-card { border-radius: 14px; border: 1px solid var(--border-light); }
.table-card :deep(.el-card__body) { padding: 0; }

.table-toolbar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 20px 0;
}
.table-info { display: flex; align-items: center; gap: 10px; }
.info-title { font-size: 15px; font-weight: 600; color: #0f172a; }
.info-count { font-size: 13px; color: #64748b; }
.info-count b { color: #0f172a; font-weight: 600; }

.seq {
  display: inline-block; width: 22px; height: 22px; line-height: 22px;
  border-radius: 50%; background: #f0f4ff; color: #2563eb;
  font-size: 12px; font-weight: 600;
}

.operator-cell { display: flex; align-items: center; gap: 10px; }
.operator-avatar {
  width: 36px; height: 36px; border-radius: 10px;
  background: linear-gradient(135deg, #2563eb, #1e40af);
  color: #fff; font-weight: 700; font-size: 14px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.operator-info { min-width: 0; }
.operator-name { font-weight: 500; color: #0f172a; font-size: 14px; }
.operator-sub { font-size: 11px; color: #94a3b8; font-family: Consolas, monospace; }

.desc-text { font-size: 13px; color: #334155; }

.time-cell {
  display: flex; align-items: center; gap: 5px;
  font-size: 13px; color: #475569;
}
.time-icon { color: #94a3b8; flex-shrink: 0; }

.pager-bar {
  display: flex; justify-content: flex-end; align-items: center;
  padding: 16px 20px;
}
</style>