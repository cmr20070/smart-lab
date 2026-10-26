<template>
  <div class="page-container">
    <div class="page-hero">
      <div class="hero-left">
        <div class="hero-icon">
          <el-icon :size="26"><Calendar /></el-icon>
        </div>
        <div>
          <h2 class="hero-title">实验室预约管理</h2>
          <p class="hero-sub">{{ heroSubtitle }}</p>
        </div>
      </div>
      <div class="hero-stats">
        <div class="stat-chip clickable" @click="quickFilter(0)">
          <span class="chip-num pending">{{ stats.pending }}</span>
          <span class="chip-label">待审批</span>
        </div>
        <div class="stat-chip clickable" @click="quickFilter(1)">
          <span class="chip-num approved">{{ stats.approved }}</span>
          <span class="chip-label">已通过</span>
        </div>
        <div class="stat-chip clickable" @click="quickFilter(2)">
          <span class="chip-num rejected">{{ stats.rejected }}</span>
          <span class="chip-label">已拒绝</span>
        </div>
        <div class="stat-chip clickable" @click="quickFilter(3)">
          <span class="chip-num cancelled">{{ stats.cancelled }}</span>
          <span class="chip-label">已取消</span>
        </div>
      </div>
    </div>

    <div class="toolbar-card">
      <div class="toolbar-left">
        <el-radio-group v-model="searchForm.status" @change="fetchData" size="default">
          <el-radio-button :value="''">全部</el-radio-button>
          <el-radio-button :value="0">待审批</el-radio-button>
          <el-radio-button :value="1">已通过</el-radio-button>
          <el-radio-button :value="2">已拒绝</el-radio-button>
          <el-radio-button :value="3">已取消</el-radio-button>
        </el-radio-group>
        <el-select
          v-if="isLabAdmin && managedLabs.length > 0"
          v-model="searchForm.labId"
          placeholder="选择实验室"
          clearable
          style="width: 200px"
          @change="fetchData"
        >
          <el-option v-for="lab in managedLabs" :key="lab.id" :label="lab.name" :value="lab.id" />
        </el-select>
      </div>
    </div>

    <el-card class="table-card" shadow="never">
      <el-table :data="tableData" v-loading="loading" style="width: 100%"
        :header-cell-style="{ background: '#fafbfc', color: '#303133', fontWeight: 600 }"
        highlight-current-row>
        <el-table-column label="序号" width="60" align="center">
          <template #default="{ $index }">
            <span class="seq">{{ (page - 1) * size + $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="labName" label="实验室" min-width="150">
          <template #default="{ row }">
            <div class="lab-cell">
              <div class="lab-avatar">{{ row.labName?.charAt(0) || '?' }}</div>
              <div class="lab-info">
                <div class="lab-name">{{ row.labName }}</div>
                <div class="lab-id">ID: {{ row.labId }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="申请人" width="120">
          <template #default="{ row }">
            <div class="user-cell">
              <span class="user-name">{{ row.username }}</span>
              <el-tag :type="roleTagType(row.userRole)" size="small" effect="light">{{ roleLabel(row.userRole) }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="预约日期" min-width="200">
          <template #default="{ row }">
            <div class="date-cell">
              <div class="date-range">
                {{ row.reserveDate }}
                <span v-if="row.endDate && row.endDate !== row.reserveDate" class="date-sep">→</span>
                <span v-if="row.endDate && row.endDate !== row.reserveDate">{{ row.endDate }}</span>
              </div>
              <div class="time-range">{{ row.startTime }} - {{ row.endTime }}</div>
              <div v-if="row.endDate && row.endDate !== row.reserveDate" class="days-tag">
                共 {{ daysBetween(row.reserveDate, row.endDate) + 1 }} 天
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="purpose" label="用途" min-width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="dark" round>{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审批信息" min-width="160">
          <template #default="{ row }">
            <div v-if="row.status !== 0" class="approve-info">
              <div class="approver">审批人: {{ row.approverName || '-' }}</div>
              <div class="approve-time">{{ formatTime(row.approveTime) }}</div>
              <div v-if="row.approveRemark" class="approve-remark">{{ row.approveRemark }}</div>
            </div>
            <div v-else class="approve-info pending">等待审批中...</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" v-if="canManage">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button type="success" size="small" @click="handleApprove(row, 1)">通过</el-button>
              <el-button type="danger" size="small" @click="handleApprove(row, 2)">拒绝</el-button>
            </template>
            <template v-else>
              <el-button type="warning" size="small" plain @click="handleAdminCancel(row)" cancel-text>强制取消</el-button>
              <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <el-dialog v-model="approveDialogVisible" :title="approveDialogTitle" width="480px" :close-on-click-modal="false">
      <el-form :model="approveForm" label-width="80px">
        <el-form-item label="实验室">
          <span>{{ approveForm.labName }}</span>
        </el-form-item>
        <el-form-item label="申请人">
          <span>{{ approveForm.username }}</span>
        </el-form-item>
        <el-form-item label="用途">
          <span>{{ approveForm.purpose || '-' }}</span>
        </el-form-item>
        <el-form-item label="审批备注">
          <el-input v-model="approveForm.remark" type="textarea" :rows="3" :placeholder="approveDialogPlaceholder" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button :type="approveForm.status === 1 ? 'success' : 'danger'" @click="submitApprove">
          确认{{ approveForm.status === 1 ? '通过' : '拒绝' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="cancelDialogVisible" title="强制取消预约" width="440px" :close-on-click-modal="false">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 16px">
        该操作会将预约状态改为"已取消"，申请人将收到通知。
      </el-alert>
      <el-form :model="cancelForm" label-width="80px">
        <el-form-item label="取消原因">
          <el-input v-model="cancelForm.remark" type="textarea" :rows="3" placeholder="请输入取消原因（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelDialogVisible = false">取消</el-button>
        <el-button type="warning" @click="submitAdminCancel">确认取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar } from '@element-plus/icons-vue'
import { getReservationList, approveReservation, adminCancelReservation, deleteReservation } from '@/api/labReservation'
import { getManagedLabs } from '@/api/lab'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const role = computed(() => userStore.userInfo?.role || '')
const isAdmin = computed(() => role.value === 'ADMIN')
const isLabAdmin = computed(() => role.value === 'LAB_ADMIN')
const canManage = computed(() => isAdmin.value || isLabAdmin.value)

const heroSubtitle = computed(() => {
  if (isAdmin.value) return '管理全校所有实验室的预约申请'
  if (isLabAdmin.value) return '管理您负责的实验室的预约申请'
  return '查看实验室预约记录'
})

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

const searchForm = reactive({ status: '', labId: null })

const managedLabs = ref([])

const stats = reactive({ pending: 0, approved: 0, rejected: 0, cancelled: 0 })

const approveDialogVisible = ref(false)
const approveForm = reactive({ id: null, status: 1, remark: '', labName: '', username: '', purpose: '' })
const approveDialogTitle = computed(() => approveForm.status === 1 ? '通过预约申请' : '拒绝预约申请')
const approveDialogPlaceholder = computed(() => approveForm.status === 1 ? '通过原因（可选）' : '请输入拒绝原因')

const cancelDialogVisible = ref(false)
const cancelForm = reactive({ id: null, remark: '' })

function roleLabel(r) { return { TEACHER: '教师', STUDENT: '学生', ADMIN: '管理员', LAB_ADMIN: '实验室管理员' }[r] || r }
function roleTagType(r) { return { TEACHER: 'primary', STUDENT: 'success', ADMIN: 'danger', LAB_ADMIN: 'warning' }[r] || 'info' }
function statusLabel(s) { return ['待审批', '已通过', '已拒绝', '已取消'][s] || '未知' }
function statusTagType(s) { return ['warning', 'success', 'danger', 'info'][s] || 'info' }
function daysBetween(d1, d2) {
  if (!d1 || !d2) return 0
  const a = new Date(d1), b = new Date(d2)
  return Math.round((b - a) / (1000 * 60 * 60 * 24))
}
function formatTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

function quickFilter(status) {
  searchForm.status = status
  fetchData()
}

async function loadManagedLabs() {
  if (!isLabAdmin.value) return
  try {
    const res = await getManagedLabs()
    const allLabs = res.data || []
    managedLabs.value = allLabs.filter(l => l.managerId === userStore.userInfo?.id)
  } catch (e) { managedLabs.value = [] }
}

async function fetchData() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (searchForm.status !== '' && searchForm.status !== null) params.status = searchForm.status
    if (searchForm.labId) params.labId = searchForm.labId
    const res = await getReservationList(params)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
    calcStats()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

function calcStats() {
  stats.pending = stats.approved = stats.rejected = stats.cancelled = 0
  const allStatuses = [0, 1, 2, 3]
  allStatuses.forEach(s => {
    if (s === 0) stats.pending += tableData.value.filter(r => r.status === s).length
    else if (s === 1) stats.approved += tableData.value.filter(r => r.status === s).length
    else if (s === 2) stats.rejected += tableData.value.filter(r => r.status === s).length
    else if (s === 3) stats.cancelled += tableData.value.filter(r => r.status === s).length
  })
}

function handleApprove(row, status) {
  approveForm.id = row.id
  approveForm.status = status
  approveForm.remark = ''
  approveForm.labName = row.labName
  approveForm.username = row.username
  approveForm.purpose = row.purpose
  approveDialogVisible.value = true
}

async function submitApprove() {
  try {
    await approveReservation({ id: approveForm.id, status: approveForm.status, remark: approveForm.remark })
    ElMessage.success(approveForm.status === 1 ? '已通过' : '已拒绝')
    approveDialogVisible.value = false
    fetchData()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

function handleAdminCancel(row) {
  cancelForm.id = row.id
  cancelForm.remark = ''
  cancelDialogVisible.value = true
}

async function submitAdminCancel() {
  try {
    await adminCancelReservation(cancelForm.id, cancelForm.remark)
    ElMessage.success('已取消')
    cancelDialogVisible.value = false
    fetchData()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定要删除该预约记录吗？`, '确认删除', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
    await deleteReservation(row.id)
    ElMessage.success('已删除')
    fetchData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || '删除失败')
  }
}

onMounted(() => {
  loadManagedLabs()
  fetchData()
})
</script>

<style scoped>
.page-container { padding: 20px; display: flex; flex-direction: column; gap: 16px; }

.page-hero {
  display: flex; justify-content: space-between; align-items: center;
  padding: 22px 26px; border-radius: 14px;
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: #fff; box-shadow: 0 4px 18px rgba(64,158,255,0.3);
}
.hero-left { display: flex; align-items: center; gap: 16px; }
.hero-icon {
  width: 52px; height: 52px; border-radius: 12px;
  background: rgba(255,255,255,0.2); display: flex; align-items: center; justify-content: center;
}
.hero-title { margin: 0; font-size: 22px; font-weight: 700; }
.hero-sub { margin: 4px 0 0; font-size: 13px; opacity: 0.85; }

.hero-stats { display: flex; gap: 12px; }
.stat-chip {
  display: flex; flex-direction: column; align-items: center;
  padding: 10px 18px; border-radius: 10px;
  background: rgba(255,255,255,0.18); min-width: 76px;
  transition: all .2s;
}
.stat-chip.clickable { cursor: pointer; }
.stat-chip.clickable:hover { background: rgba(255,255,255,0.3); transform: translateY(-2px); }
.chip-num { font-size: 22px; font-weight: 700; line-height: 1.2; }
.chip-label { font-size: 12px; opacity: 0.85; margin-top: 2px; }

.toolbar-card {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 18px; border-radius: 12px; background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.toolbar-left { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.toolbar-right { display: flex; gap: 10px; }

.table-card { border-radius: 12px; border: none; box-shadow: 0 2px 12px rgba(0,0,0,0.05); }
.table-card :deep(.el-card__body) { padding: 0; }
.seq { color: #909399; font-size: 13px; }

.lab-cell { display: flex; align-items: center; gap: 10px; }
.lab-avatar {
  width: 36px; height: 36px; border-radius: 8px;
  background: linear-gradient(135deg, #409eff, #66b1ff);
  color: #fff; display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 15px;
}
.lab-name { font-weight: 600; color: #303133; }
.lab-id { font-size: 12px; color: #909399; }

.user-cell { display: flex; flex-direction: column; gap: 4px; }
.user-name { font-weight: 500; }

.date-cell { display: flex; flex-direction: column; gap: 2px; }
.date-range { font-weight: 600; color: #303133; font-size: 13px; }
.date-sep { margin: 0 4px; color: #909399; }
.time-range { font-size: 12px; color: #606266; }
.days-tag { font-size: 11px; color: #409eff; font-weight: 600; margin-top: 2px; }

.approve-info { font-size: 12px; color: #606266; }
.approve-info .approver { color: #303133; font-weight: 500; }
.approve-info .approve-time { color: #909399; margin-top: 2px; }
.approve-info .approve-remark { color: #e6a23c; margin-top: 2px; }
.approve-info.pending { color: #409eff; font-style: italic; }

.pagination-wrap { padding: 16px; display: flex; justify-content: flex-end; }
</style>