<template>
  <div class="page-container">
    <div class="page-hero">
      <div class="hero-left">
        <div class="hero-icon">
          <el-icon :size="26"><Delete /></el-icon>
        </div>
        <div>
          <h2 class="hero-title">回收站</h2>
          <p class="hero-sub">恢复已删除的数据，或彻底清理</p>
        </div>
      </div>
      <div class="hero-actions">
        <el-button
          v-if="currentCount > 0"
          type="danger"
          plain
          @click="handlePurgeAll"
        >
          <el-icon><Delete /></el-icon>
          清空 {{ currentTypeLabel }} ({{ currentCount }})
        </el-button>
      </div>
    </div>

    <div class="type-stats">
      <div
        v-for="tab in tabs"
        :key="tab.type"
        class="type-card"
        :class="{ active: currentType === tab.type }"
        @click="switchType(tab.type)"
      >
        <div class="type-icon" :style="{ background: tab.gradient }">
          <el-icon :size="20"><component :is="tab.icon" /></el-icon>
        </div>
        <div class="type-info">
          <span class="type-num">{{ counts[tab.type] || 0 }}</span>
          <span class="type-label">{{ tab.label }}</span>
        </div>
      </div>
    </div>

    <el-card class="content-card" shadow="never">
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column label="序号" width="70" align="center">
          <template #default="{ $index }">
            <span class="seq-num">{{ (page - 1) * size + $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="名称" min-width="180">
          <template #default="{ row }">
            <div class="name-cell">
              <div class="name-text">{{ getDisplayName(row) }}</div>
              <div class="name-id">ID: {{ row.id }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="详情" min-width="220">
          <template #default="{ row }">
            <template v-if="currentType === 'device'">
              <div class="detail-row"><span class="label">编号</span><span>{{ row.code || '-' }}</span></div>
              <div class="detail-row"><span class="label">分类</span><span>{{ row.category || '-' }}</span></div>
              <div class="detail-row"><span class="label">型号</span><span>{{ row.model || '-' }}</span></div>
            </template>
            <template v-else-if="currentType === 'lab'">
              <div class="detail-row"><span class="label">编号</span><span>{{ row.code || '-' }}</span></div>
              <div class="detail-row"><span class="label">位置</span><span>{{ row.location || '-' }}</span></div>
              <div class="detail-row"><span class="label">分类</span><span>{{ row.category || '-' }}</span></div>
            </template>
            <template v-else-if="currentType === 'borrow'">
              <div class="detail-row"><span class="label">设备</span><span>{{ row.device_name || '-' }}</span></div>
              <div class="detail-row"><span class="label">状态</span><span>{{ borrowStatusLabel(row.status) }}</span></div>
              <div class="detail-row"><span class="label">借用时间</span><span>{{ row.borrow_time || '-' }}</span></div>
            </template>
            <template v-else-if="currentType === 'repair'">
              <div class="detail-row"><span class="label">设备</span><span>{{ row.device_name || '-' }}</span></div>
              <div class="detail-row"><span class="label">报修人</span><span>{{ row.reporter_name || '-' }}</span></div>
              <div class="detail-row"><span class="label">故障等级</span><span>{{ row.fault_level ? levelLabel(row.fault_level) : '-' }}</span></div>
            </template>
            <template v-else-if="currentType === 'user'">
              <div class="detail-row"><span class="label">手机</span><span>{{ row.phone || '-' }}</span></div>
              <div class="detail-row"><span class="label">邮箱</span><span>{{ row.email || '-' }}</span></div>
              <div class="detail-row"><span class="label">角色</span><span>{{ roleLabel(row.role) }}</span></div>
            </template>
            <template v-else-if="currentType === 'lab_reservation'">
              <div class="detail-row"><span class="label">实验室ID</span><span>{{ row.lab_id || '-' }}</span></div>
              <div class="detail-row"><span class="label">预约日期</span><span>{{ row.reserve_date || '-' }}</span></div>
              <div class="detail-row"><span class="label">状态</span><span>{{ reservationStatusLabel(row.status) }}</span></div>
            </template>
            <template v-else-if="currentType === 'announcement'">
              <div class="detail-row"><span class="label">类型</span><span>{{ announcementTypeLabel(row.type) }}</span></div>
              <div class="detail-row"><span class="label">发布人ID</span><span>{{ row.publisher_id || '-' }}</span></div>
              <div class="detail-row"><span class="label">状态</span><span>{{ row.status === 1 ? '已发布' : '已撤回' }}</span></div>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="删除时间" width="180">
          <template #default="{ row }">
            <span class="time-text">{{ row.update_time || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleRestore(row)">
              <el-icon><RefreshLeft /></el-icon>恢复
            </el-button>
            <el-button size="small" type="danger" plain @click="handlePurge(row)">
              <el-icon><Delete /></el-icon>彻底删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        class="pagination"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        :page-size="size"
        :current-page="page"
        @current-change="fetchData"
        @size-change="handleSizeChange"
      />

      <el-empty v-if="!loading && total === 0" description="该类型没有已删除的数据">
        <el-icon :size="48" color="#dcdfe6"><Delete /></el-icon>
      </el-empty>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTrashList, getTrashCounts, restore, purge, purgeAll } from '@/api/recyclebin'
import {
  Cpu, OfficeBuilding, Timer, Tools, User, Delete, RefreshLeft,
  Calendar, Bell,
} from '@element-plus/icons-vue'

const tabs = [
  { type: 'device', label: '设备', icon: Cpu, gradient: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)' },
  { type: 'lab', label: '实验室', icon: OfficeBuilding, gradient: 'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)' },
  { type: 'borrow', label: '借用记录', icon: Timer, gradient: 'linear-gradient(135deg, #60a5fa 0%, #2563eb 100%)' },
  { type: 'repair', label: '维修记录', icon: Tools, gradient: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)' },
  { type: 'lab_reservation', label: '实验室预约', icon: Calendar, gradient: 'linear-gradient(135deg, #a78bfa 0%, #7c3aed 100%)' },
  { type: 'announcement', label: '公告', icon: Bell, gradient: 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)' },
  { type: 'user', label: '用户', icon: User, gradient: 'linear-gradient(135deg, #30cfd0 0%, #330867 100%)' },
]

const currentType = ref('device')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)
const tableData = ref([])
const counts = reactive({ device: 0, lab: 0, borrow: 0, repair: 0, lab_reservation: 0, announcement: 0, user: 0 })

const currentTypeLabel = computed(() => {
  const t = tabs.find(x => x.type === currentType.value)
  return t ? t.label : ''
})

const currentCount = computed(() => counts[currentType.value] || 0)

const switchType = (type) => {
  currentType.value = type
  page.value = 1
  fetchData()
}

const handleSizeChange = (newSize) => {
  size.value = newSize
  page.value = 1
  fetchData()
}

const fetchCounts = async () => {
  try {
    const res = await getTrashCounts()
    Object.assign(counts, res.data)
  } catch (e) {}
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getTrashList(currentType.value, page.value, size.value)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {}
  loading.value = false
}

const getDisplayName = (row) => {
  if (currentType.value === 'device') return row.name || '未命名设备'
  if (currentType.value === 'lab') return row.name || '未命名实验室'
  if (currentType.value === 'borrow') return row.username || row.device_name || '借用记录'
  if (currentType.value === 'repair') return row.device_name || '维修记录'
  if (currentType.value === 'lab_reservation') return '实验室预约记录'
    if (currentType.value === 'announcement') return row.title || '公告'
  if (currentType.value === 'user') return row.username || '用户'
  return '数据'
}

const borrowStatusLabel = (s) => {
  const map = { 0: '待审批', 1: '已通过', 2: '已拒绝', 3: '已归还', 4: '已取消', 5: '已逾期' }
  return map[s] ?? '未知'
}
const repairStatusLabel = (s) => {
  const map = { 0: '待处理', 1: '维修中', 2: '已完成' }
  return map[s] ?? '未知'
}
const levelLabel = (l) => {
  const map = { 1: '一般', 2: '紧急', 3: '严重' }
  return map[l] ?? '未知'
}
const roleLabel = (r) => {
  const map = { ADMIN: '管理员', LAB_ADMIN: '实验室管理员', TEACHER: '教师', STUDENT: '学生' }
  return map[r] ?? r
}

const reservationStatusLabel = (s) => {
  const map = { 0: '待审批', 1: '已通过', 2: '已拒绝', 3: '已取消' }
  return map[s] ?? '未知'
}

const announcementTypeLabel = (t) => {
  const map = { GENERAL: '通知', NOTICE: '提醒', POLICY: '制度', MAINTENANCE: '维护', HOLIDAY: '假期' }
  return map[t] ?? (t || '通知')
}

const handleRestore = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要恢复这条${currentTypeLabel.value}吗？`, '恢复确认', {
      confirmButtonText: '恢复',
      cancelButtonText: '取消',
      type: 'info',
    })
    await restore(currentType.value, row.id)
    ElMessage.success('恢复成功')
    await fetchCounts()
    fetchData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '恢复失败')
  }
}

const handlePurge = async (row) => {
  try {
    await ElMessageBox.confirm(`彻底删除无法恢复！确定要删除这条${currentTypeLabel.value}吗？`, '彻底删除确认', {
      confirmButtonText: '彻底删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await purge(currentType.value, row.id)
    ElMessage.success('已彻底删除')
    await fetchCounts()
    fetchData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

const handlePurgeAll = async () => {
  try {
    await ElMessageBox.confirm(`将彻底清空所有"${currentTypeLabel.value}"回收站数据，无法恢复！`, '清空确认', {
      confirmButtonText: '确认清空',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await purgeAll(currentType.value)
    ElMessage.success('已清空')
    await fetchCounts()
    fetchData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '清空失败')
  }
}

onMounted(async () => {
  await fetchCounts()
  fetchData()
})
</script>

<style scoped>
.page-container { padding: 0; }

.page-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 28px;
  background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
  border-radius: 14px;
  margin-bottom: 20px;
}

.hero-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.hero-icon {
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(29, 78, 216, 0.2);
  color: #fca5a5;
  border-radius: 12px;
}

.hero-title {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  margin: 0;
}

.hero-sub {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  margin: 4px 0 0 0;
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.type-stats {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.type-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s;
  border: 2px solid transparent;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.type-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

.type-card.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.type-icon {
  width: 42px;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  color: #fff;
  flex-shrink: 0;
}

.type-info {
  display: flex;
  flex-direction: column;
}

.type-num {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  line-height: 1;
}

.type-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.content-card {
  border-radius: 12px;
  border: none;
}

.content-card :deep(.el-card__body) {
  padding: 0;
}

.seq-num {
  color: #c0c4cc;
  font-weight: 500;
}

.name-cell {
  display: flex;
  flex-direction: column;
}

.name-text {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.name-id {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 2px;
}

.detail-row {
  display: flex;
  gap: 8px;
  font-size: 13px;
  line-height: 1.8;
}

.detail-row .label {
  color: #c0c4cc;
  flex-shrink: 0;
}

.time-text {
  font-size: 13px;
  color: #606266;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>