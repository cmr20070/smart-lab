<template>
  <div class="page-container">
    <div class="hero-section">
      <div class="hero-content">
        <div class="hero-icon">
          <el-icon :size="32"><Tools /></el-icon>
        </div>
        <div class="hero-text">
          <h2>维修维护中心</h2>
          <p>跟踪设备维修状态，保障实验室设备正常运行</p>
        </div>
      </div>
      <div class="hero-actions">
        <el-button v-if="canManage" @click="handleExport" icon="Download" plain>导出记录</el-button>
        <el-button type="primary" @click="handleReport" icon="Edit">
          <el-icon><Plus /></el-icon> 故障报修
        </el-button>
      </div>
    </div>

    <div class="status-stats">
      <div class="stat-card" :class="{ active: searchForm.status === null }" @click="selectStatus(null)">
        <div class="stat-icon" style="background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);">
          <el-icon :size="22"><Document /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-num">{{ total }}</span>
          <span class="stat-label">全部记录</span>
        </div>
        <div class="stat-trend up">
          <el-icon><TrendCharts /></el-icon>
        </div>
      </div>
      <div class="stat-card warning" :class="{ active: searchForm.status === 0 }" @click="selectStatus(0)">
        <div class="stat-icon" style="background: linear-gradient(135deg, #60a5fa 0%, #2563eb 100%);">
          <el-icon :size="22"><Warning /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-num">{{ statusStats[0] || 0 }}</span>
          <span class="stat-label">待处理</span>
        </div>
        <div class="stat-dot" style="background: #2563eb; animation: pulse 2s infinite;"></div>
      </div>
      <div class="stat-card processing" :class="{ active: searchForm.status === 1 }" @click="selectStatus(1)">
        <div class="stat-icon" style="background: linear-gradient(135deg, #3b82f6 0%, #0ea5e9 100%);">
          <el-icon :size="22"><Loading /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-num">{{ statusStats[1] || 0 }}</span>
          <span class="stat-label">维修中</span>
        </div>
        <div class="stat-progress">
          <div class="progress-bar" style="width: 60%;"></div>
        </div>
      </div>
      <div class="stat-card success" :class="{ active: searchForm.status === 2 }" @click="selectStatus(2)">
        <div class="stat-icon" style="background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);">
          <el-icon :size="22"><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-num">{{ statusStats[2] || 0 }}</span>
          <span class="stat-label">已完成</span>
        </div>
        <div class="stat-badge">
          <el-icon><CircleCheckFilled /></el-icon>
        </div>
      </div>
    </div>

    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="">
          <el-input v-model="searchForm.keyword" placeholder="搜索设备名称/编号" clearable style="width: 240px"
            @keyup.enter="fetchData">
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable @change="fetchData" style="width: 130px">
            <el-option label="全部" :value="null" />
            <el-option label="待处理" :value="0" />
            <el-option label="维修中" :value="1" />
            <el-option label="已完成" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="故障级别">
          <el-select v-model="searchForm.faultLevel" placeholder="全部级别" clearable @change="fetchData" style="width: 130px">
            <el-option label="全部" :value="null" />
            <el-option label="一般" :value="1" />
            <el-option label="紧急" :value="2" />
            <el-option label="严重" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData" :icon="Search">查询</el-button>
          <el-button @click="resetSearch" :icon="Refresh">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <div class="table-toolbar">
        <div class="table-info">
          <span class="info-label">维修记录</span>
          <span class="info-count">共 {{ total }} 条</span>
        </div>
        <div v-if="canManage && selectedRepairs.length" class="batch-bar">
          <el-button size="small" type="danger" @click="handleBatchDelete" plain :icon="Delete">
            批量删除 ({{ selectedRepairs.length }})
          </el-button>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" style="width: 100%"
        @selection-change="handleRepairSelectionChange" ref="repairTableRef"
        :header-cell-style="{ background: '#f8f9fc', color: '#303133', fontWeight: '600', borderBottom: '1px solid #ebeef5' }"
        :cell-style="{ borderBottom: '1px solid #f0f0f0' }"
        :empty-text="''">
        <template #empty>
          <div class="empty-wrapper">
            <el-icon :size="64" color="#dcdfe6"><Tools /></el-icon>
            <p>暂无维修记录</p>
            <el-button type="primary" size="small" @click="handleReport">立即报修</el-button>
          </div>
        </template>
        <el-table-column v-if="canManage" type="selection" width="45" />
        <el-table-column label="设备信息" min-width="200">
          <template #default="{ row }">
            <div class="device-cell">
              <el-image v-if="row.deviceImageUrl" :src="getRepairImageUrl(row.deviceImageUrl)" style="width:44px;height:44px;border-radius:8px;object-fit:cover;flex-shrink:0" fit="cover">
                <template #error>
                  <div class="device-avatar" :class="getDeviceClass(row.faultLevel)">
                    <el-icon :size="18"><Monitor /></el-icon>
                  </div>
                </template>
              </el-image>
              <div v-else class="device-avatar" :class="getDeviceClass(row.faultLevel)">
                <el-icon :size="18"><Monitor /></el-icon>
              </div>
              <div class="device-detail">
                <span class="device-name">{{ row.deviceName || '-' }}</span>
                <span class="device-code">{{ row.deviceCode || '-' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column v-if="canManage" prop="reporterName" label="报修人" width="110">
          <template #default="{ row }">
            <div class="reporter-cell">
              <el-avatar :size="24" :icon="UserFilled" style="background: #ecf5ff; color: #409eff;" />
              <span>{{ row.reporterName || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="faultDescription" label="故障描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="级别" width="100" align="center">
          <template #default="{ row }">
            <div class="level-badge" :class="'level-' + row.faultLevel">
              <el-icon :size="12"><Warning /></el-icon>
              <span>{{ levelLabel(row.faultLevel) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <div class="status-badge" :class="'status-' + row.status">
              <span class="status-dot" :style="{ background: statusDotColor(row.status) }"></span>
              <span>{{ statusLabel(row.status) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="报修时间" width="170">
          <template #default="{ row }">
            <div class="time-cell">
              <el-icon :size="14" class="time-icon"><Clock /></el-icon>
              <span>{{ row.reportTime }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0 && canManage"
              size="small" type="primary" @click="handleAssign(row)" :icon="Tools">接修</el-button>
            <el-button
              v-if="row.status === 1 && canManage"
              size="small" type="success" @click="handleComplete(row)" :icon="Finished">完成</el-button>
            <el-button v-if="canManage" size="small" type="warning" @click="handleEditOpen(row)" :icon="Edit">编辑</el-button>
            <el-button v-if="canManage" size="small" type="danger" link @click="handleDelete(row)" :icon="Delete">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchData"
        @current-change="fetchData"
        style="margin-top: 20px; justify-content: flex-end"
        background
      />
    </el-card>

    <el-dialog v-model="dialogVisible" title="故障报修" width="520px" @close="resetForm" class="modern-dialog">
      <div class="dialog-hero">
        <div class="dialog-hero-icon">
          <el-icon :size="28"><Warning /></el-icon>
        </div>
        <div>
          <h4>提交故障报修</h4>
          <p>请详细描述设备故障情况，以便及时处理</p>
        </div>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="选择设备" prop="deviceId">
          <el-select v-model="form.deviceId" placeholder="请选择需要报修的设备" filterable style="width: 100%">
            <el-option v-for="d in allDevices" :key="d.id" :label="d.name + ' (' + d.code + ')'" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="故障级别" prop="faultLevel">
          <el-radio-group v-model="form.faultLevel" class="level-radio-group">
            <el-radio :value="1" class="level-radio level-1">一般</el-radio>
            <el-radio :value="2" class="level-radio level-2">紧急</el-radio>
            <el-radio :value="3" class="level-radio level-3">严重</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="故障描述" prop="faultDescription">
          <el-input v-model="form.faultDescription" type="textarea" :rows="4" placeholder="请详细描述故障现象..." />
        </el-form-item>
        <el-form-item label="补充备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选，添加更多信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          <el-icon><Promotion /></el-icon> 提交报修
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="completeDialogVisible" title="维修完成确认" width="480px" class="modern-dialog">
      <div class="dialog-hero success">
        <div class="dialog-hero-icon">
          <el-icon :size="28"><CircleCheck /></el-icon>
        </div>
        <div>
          <h4>维修完成确认</h4>
          <p>请填写维修说明，完成后设备状态将更新</p>
        </div>
      </div>
      <el-form ref="completeFormRef" :model="completeForm" :rules="completeRules" label-width="100px">
        <el-form-item label="维修说明" prop="repairDescription">
          <el-input v-model="completeForm.repairDescription" type="textarea" :rows="4" placeholder="请描述具体维修内容..." />
        </el-form-item>
        <el-form-item label="维修费用">
          <el-input-number v-model="completeForm.repairCost" :precision="2" :min="0" style="width: 100%" />
          <div class="form-tip">单位：元，可留空</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmComplete">
          <el-icon><CircleCheckFilled /></el-icon> 确认完成
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialogVisible" title="编辑维修记录" width="520px" class="modern-dialog" @close="resetEditForm">
      <el-form ref="editFormRef" :model="editForm" label-width="100px">
        <el-form-item label="设备名称">
          <el-input :model-value="editForm.deviceName" disabled />
        </el-form-item>
        <el-form-item label="故障级别" prop="faultLevel">
          <el-radio-group v-model="editForm.faultLevel">
            <el-radio :value="1">一般</el-radio>
            <el-radio :value="2">紧急</el-radio>
            <el-radio :value="3">严重</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="editForm.status" style="width:100%">
            <el-option label="待处理" :value="0" />
            <el-option label="维修中" :value="1" />
            <el-option label="已完成" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="故障描述" prop="faultDescription">
          <el-input v-model="editForm.faultDescription" type="textarea" :rows="3" placeholder="故障描述" />
        </el-form-item>
        <el-form-item label="维修说明">
          <el-input v-model="editForm.repairDescription" type="textarea" :rows="2" placeholder="维修说明（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="confirmEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Refresh, Monitor, UserFilled, Clock, Warning, Loading, CircleCheck,
  CircleCheckFilled, Tools, Finished, Delete, Edit, Document, Plus, TrendCharts, Promotion
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { getRepairList, reportFault, assignRepair, completeRepair, updateRepair, deleteRepair } from '@/api/repair'
import { getAllDevices } from '@/api/device'
import { downloadFile } from '@/utils/request'

const getRepairImageUrl = (url) => {
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return url
  return encodeURI(url)
}

const userStore = useUserStore()

const canManage = computed(() => {
  const role = userStore.userInfo?.role
  return role === 'ADMIN' || role === 'LAB_ADMIN'
})
const isStudentOrTeacher = computed(() => {
  const role = userStore.userInfo?.role
  return role === 'STUDENT' || role === 'TEACHER'
})

const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const allDevices = ref([])
const statusStats = ref({ 0: 0, 1: 0, 2: 0 })

const searchForm = reactive({
  status: null,
  faultLevel: null,
  keyword: ''
})

const selectedRepairs = ref([])
const repairTableRef = ref(null)

const selectStatus = (status) => {
  searchForm.status = status
  fetchData()
}

const handleRepairSelectionChange = (rows) => {
  selectedRepairs.value = rows
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${selectedRepairs.value.length} 条维修记录?`, '批量删除', { type: 'warning' })
    for (const row of selectedRepairs.value) {
      await deleteRepair(row.id)
    }
    ElMessage.success('批量删除成功')
    selectedRepairs.value = []
    fetchData()
  } catch { /* cancelled */ }
}

const getDeviceClass = (level) => {
  const map = { 1: 'level-normal', 2: 'level-urgent', 3: 'level-severe' }
  return map[level] || 'level-normal'
}

const statusDotColor = (status) => {
  const map = { 0: '#2563eb', 1: '#3b82f6', 2: '#11998e' }
  return map[status] || '#909399'
}

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({
  deviceId: null,
  faultLevel: 1,
  faultDescription: '',
  remark: '',
})

const rules = {}

const completeDialogVisible = ref(false)
const completeFormRef = ref(null)
const currentRepairId = ref(null)

const completeForm = reactive({ repairDescription: '', repairCost: null })
const completeRules = {
  repairDescription: [{ required: true, message: '请填写维修描述', trigger: 'blur' }],
}

const levelLabel = (level) => {
  const map = { 1: '一般', 2: '紧急', 3: '严重' }
  return map[level] || '未知'
}

const statusLabel = (status) => {
  const map = { 0: '待处理', 1: '维修中', 2: '已完成' }
  return map[status] || '未知'
}

const fetchStats = async () => {
  try {
    const stats = { 0: 0, 1: 0, 2: 0 }
    for (let s = 0; s <= 2; s++) {
      try {
        const r = await getRepairList({ page: 1, size: 1, status: s })
        stats[s] = r.data.total
      } catch { /* ignore */ }
    }
    statusStats.value = stats
  } catch { /* ignore */ }
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (searchForm.status !== null && searchForm.status !== '') params.status = searchForm.status
    if (searchForm.faultLevel !== null && searchForm.faultLevel !== '') params.faultLevel = searchForm.faultLevel
    if (searchForm.keyword) params.keyword = searchForm.keyword
    const res = await getRepairList(params)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
    fetchStats()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleExport = () => {
  const params = new URLSearchParams()
  if (searchForm.status !== null && searchForm.status !== '') params.append('status', searchForm.status)
  if (searchForm.keyword) params.append('keyword', searchForm.keyword)
  downloadFile('/export/repair?' + params.toString(), '维修记录')
}

const resetSearch = () => {
  searchForm.status = null
  searchForm.faultLevel = null
  searchForm.keyword = ''
  fetchData()
}

const handleReport = async () => {
  try {
    const res = await getAllDevices()
    allDevices.value = (res.data || []).filter(d => d.status === 1)
  } catch (e) {
    console.error('加载设备列表失败', e)
  }
  resetForm()
  dialogVisible.value = true
}

const resetForm = () => {
  form.deviceId = null
  form.faultLevel = 1
  form.faultDescription = ''
  form.remark = ''
}

const handleSubmit = async () => {
  if (!form.deviceId) {
    ElMessage.warning('请选择设备')
    return
  }
  if (!form.faultLevel || form.faultLevel < 1 || form.faultLevel > 3) {
    ElMessage.warning('请选择故障级别')
    return
  }
  if (!form.faultDescription || !form.faultDescription.trim()) {
    ElMessage.warning('请填写故障描述')
    return
  }

  submitting.value = true
  try {
    const payload = {
      deviceId: form.deviceId,
      faultLevel: form.faultLevel,
      faultDescription: form.faultDescription.trim(),
      remark: form.remark?.trim() || ''
    }
    console.log('提交报修 payload:', payload)
    await reportFault(payload)
    ElMessage.success('报修提交成功')
    dialogVisible.value = false
    fetchData()
  } catch (e) {
    const msg = e?.response?.data?.message || e?.message || '报修提交失败，请稍后重试'
    ElMessage.error(msg)
    console.error('报修失败:', e)
  } finally {
    submitting.value = false
  }
}

const handleAssign = async (row) => {
  try {
    await ElMessageBox.confirm('确定要接修该设备吗？', '确认接修', { type: 'info' })
    await assignRepair(row.id)
    ElMessage.success('接修成功')
    fetchData()
  } catch { /* cancel */ }
}

const handleComplete = (row) => {
  currentRepairId.value = row.id
  completeForm.repairDescription = ''
  completeForm.repairCost = null
  completeDialogVisible.value = true
}

const confirmComplete = async () => {
  const valid = await completeFormRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    await completeRepair(currentRepairId.value, { ...completeForm })
    ElMessage.success('维修完成')
    completeDialogVisible.value = false
    fetchData()
  } catch (e) {
    console.error(e)
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该维修记录吗？', '提示', { type: 'warning' })
    .then(async () => {
      await deleteRepair(row.id)
      ElMessage.success('删除成功')
      fetchData()
    })
    .catch(() => {})
}

const editDialogVisible = ref(false)
const editSubmitting = ref(false)
const editFormRef = ref(null)
const editForm = reactive({
  deviceName: '',
  faultLevel: 1,
  status: 0,
  faultDescription: '',
  repairDescription: ''
})

const handleEdit = (row) => {
  editForm.deviceName = row.deviceName || ''
  editForm.faultLevel = row.faultLevel || 1
  editForm.status = row.status ?? 0
  editForm.faultDescription = row.faultDescription || ''
  editForm.repairDescription = row.repairDescription || ''
  editDialogVisible.value = true
}

const resetEditForm = () => {
  editForm.deviceName = ''
  editForm.faultLevel = 1
  editForm.status = 0
  editForm.faultDescription = ''
  editForm.repairDescription = ''
}

const confirmEdit = async () => {
  if (!editForm.faultDescription.trim()) {
    ElMessage.warning('请填写故障描述')
    return
  }
  editSubmitting.value = true
  try {
    await updateRepair(currentEditId, {
      faultLevel: editForm.faultLevel,
      status: editForm.status,
      faultDescription: editForm.faultDescription.trim(),
      repairDescription: editForm.repairDescription?.trim() || ''
    })
    ElMessage.success('编辑成功')
    editDialogVisible.value = false
    fetchData()
  } catch (e) {
    console.error(e)
  } finally {
    editSubmitting.value = false
  }
}
let currentEditId = null
const handleEditOpen = (row) => {
  currentEditId = row.id
  handleEdit(row)
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.page-container {
  height: 100%;
  overflow-y: auto;
  padding: 24px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
}

.hero-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 28px;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  border-radius: 16px;
  margin-bottom: 20px;
  color: #fff;
  position: relative;
  overflow: hidden;
}

.hero-section::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -10%;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 70%);
  border-radius: 50%;
}

.hero-content {
  display: flex;
  align-items: center;
  gap: 18px;
  position: relative;
  z-index: 1;
}

.hero-icon {
  width: 56px;
  height: 56px;
  background: rgba(255,255,255,0.2);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
}

.hero-text h2 {
  margin: 0 0 4px;
  font-size: 22px;
  font-weight: 700;
}

.hero-text p {
  margin: 0;
  font-size: 14px;
  opacity: 0.9;
}

.hero-actions {
  display: flex;
  gap: 12px;
  position: relative;
  z-index: 1;
}

.hero-actions .el-button {
  border-radius: 10px;
  font-weight: 500;
}

.hero-actions .el-button:not([type="primary"]) {
  background: rgba(255,255,255,0.2);
  border-color: rgba(255,255,255,0.3);
  color: #fff;
}

.status-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border-radius: 14px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  position: relative;
  overflow: hidden;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.1);
}

.stat-card.active {
  border-color: #2563eb;
  background: linear-gradient(135deg, #f5f7ff 0%, #eef2ff 100%);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-num {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a2e;
  line-height: 1;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

.stat-trend {
  color: #67c23a;
  display: flex;
  align-items: center;
}

.stat-progress {
  width: 40px;
  height: 6px;
  background: #ebeef5;
  border-radius: 3px;
  overflow: hidden;
}

.stat-progress .progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #0ea5e9);
  border-radius: 3px;
}

.stat-badge {
  color: #67c23a;
}

.stat-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.search-card {
  border-radius: 14px;
  border: none;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}

.search-card :deep(.el-card__body) {
  padding: 18px 22px;
}

.search-form {
  margin: 0;
}

.search-form .el-form-item {
  margin-bottom: 0;
}

.table-card {
  border-radius: 14px;
  border: none;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}

.table-card :deep(.el-card__body) {
  padding: 20px 22px;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.table-info {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.table-info .info-label {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
}

.table-info .info-count {
  font-size: 13px;
  color: #909399;
}

.empty-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px 0;
}

.empty-wrapper p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.device-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.device-avatar {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.device-avatar.level-normal {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  color: #0a9478;
}

.device-avatar.level-urgent {
  background: linear-gradient(135deg, #60a5fa 0%, #2563eb 100%);
}

.device-avatar.level-severe {
  background: linear-gradient(135deg, #ff6b6b 0%, #1e40af 100%);
}

.device-detail {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.device-name {
  font-weight: 600;
  color: #1a1a2e;
}

.device-code {
  font-size: 12px;
  color: #909399;
  font-family: 'Consolas', monospace;
}

.reporter-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.level-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.level-badge.level-1 {
  background: #e1f3d8;
  color: #529b2e;
}

.level-badge.level-2 {
  background: #faecd8;
  color: #c68c06;
}

.level-badge.level-3 {
  background: #fde2e2;
  color: #d10000;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge .status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-badge.status-0 {
  background: #fef0f0;
  color: #f56c6c;
}

.status-badge.status-1 {
  background: #ecf5ff;
  color: #409eff;
}

.status-badge.status-2 {
  background: #f0f9eb;
  color: #67c23a;
}

.time-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
}

.time-icon {
  color: #c0c4cc;
}

.modern-dialog :deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
}

.modern-dialog :deep(.el-dialog__header) {
  padding: 20px 24px 0;
}

.modern-dialog :deep(.el-dialog__body) {
  padding: 16px 24px 24px;
}

.modern-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px 20px;
  border-top: 1px solid #f0f0f0;
}

.dialog-hero {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  background: linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%);
  border-radius: 12px;
  margin-bottom: 20px;
}

.dialog-hero.success {
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
}

.dialog-hero-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #2563eb 0%, #60a5fa 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dialog-hero.success .dialog-hero-icon {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.dialog-hero h4 {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
}

.dialog-hero p {
  margin: 0;
  font-size: 13px;
  color: #606266;
}

.level-radio-group {
  display: flex;
  gap: 8px;
}

.level-radio {
  padding: 8px 20px;
  border-radius: 8px;
  border: 1px solid #dcdfe6;
  transition: all 0.3s;
}

.level-radio.is-checked {
  font-weight: 600;
}

.level-radio.level-1.is-checked {
  border-color: #67c23a;
  background: #f0f9eb;
}

.level-radio.level-2.is-checked {
  border-color: #e6a23c;
  background: #fdf6ec;
}

.level-radio.level-3.is-checked {
  border-color: #f56c6c;
  background: #fef0f0;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.el-pagination :deep(.btn-prev),
.el-pagination :deep(.btn-next),
.el-pagination :deep(.el-pager li) {
  border-radius: 8px;
}
</style>