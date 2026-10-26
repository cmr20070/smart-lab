<template>
  <div class="page-container">
    <div class="page-hero">
      <div class="hero-left">
        <div class="hero-icon">
          <el-icon :size="26"><OfficeBuilding /></el-icon>
        </div>
        <div>
          <h2 class="hero-title">{{ isStudentOrTeacher ? '实验室浏览' : '实验室管理' }}</h2>
          <p class="hero-sub">{{ isStudentOrTeacher ? '查看所有实验室信息、位置、容量与设备状态' : '管理实验室信息、分类、容量与负责人分配' }}</p>
        </div>
      </div>
      <div v-if="!isStudentOrTeacher" class="hero-actions">
        <el-button v-if="canManageLab" :icon="Calendar" plain size="large" @click="openReserveAdminPanel">预约审批</el-button>
        <el-button :icon="Plus" type="primary" size="large" @click="handleAdd">新增实验室</el-button>
      </div>
      <div v-else class="hero-actions">
        <el-button :icon="Calendar" plain size="large" @click="openMyReservations">我的预约</el-button>
      </div>
    </div>

    <div class="toolbar-card">
      <div class="search-fields">
        <el-input v-model="searchForm.keyword" placeholder="搜索实验室名称 / 编号" clearable style="width: 260px"
          :prefix-icon="Search" @clear="fetchData" @keyup.enter="fetchData" />
        <el-select v-model="searchForm.category" placeholder="全部分类" clearable style="width: 140px" @change="fetchData">
          <el-option label="计算机" value="计算机" />
          <el-option label="物理" value="物理" />
          <el-option label="化学" value="化学" />
          <el-option label="电子" value="电子" />
        </el-select>
        <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
      </div>
      <div v-if="selectedLabs.length > 0" class="batch-bar">
        <span class="batch-count">已选 <b>{{ selectedLabs.length }}</b> 项</span>
        <el-button size="small" @click="handleLabSelectAll(true)">全选</el-button>
        <el-button size="small" @click="handleLabInvertSelect">反选</el-button>
        <el-button size="small" type="danger" plain @click="handleBatchDelete">批量删除</el-button>
      </div>
    </div>

    <div v-if="viewMode === 'grid'" class="lab-grid">
      <div v-for="row in tableData" :key="row.id" class="lab-card" @click="handleCardClick(row)">
        <div class="lab-cover" :style="row.imageUrl ? { backgroundImage: `url(${getImageUrl(row.imageUrl)})`, backgroundSize: 'cover', backgroundPosition: 'center' } : { background: getCategoryGradient(row.category) }">
          <div v-if="!row.imageUrl" class="lab-cover-icon">
            <el-icon :size="32"><OfficeBuilding /></el-icon>
          </div>
          <div class="lab-cover-overlay"></div>
          <div v-if="row.status === 1" class="status-badge active">启用中</div>
          <div v-else class="status-badge inactive">已停用</div>
        </div>
        <div class="lab-body">
          <div class="lab-name">{{ row.name }}</div>
          <div class="lab-code">{{ row.code }}</div>
          <div class="lab-meta">
            <span class="meta-item"><el-icon><Location /></el-icon>{{ row.location }}</span>
            <span class="meta-item"><el-icon><User /></el-icon>{{ getUserName(row.managerId) || '未分配' }}</span>
          </div>
          <div class="lab-footer">
            <span class="capacity-badge">
              <el-icon><Aim /></el-icon> 容量 {{ row.capacity }}
            </span>
            <el-tag v-if="row.status === 1" size="small" :style="{ background: getCategoryColor(row.category), borderColor: getCategoryColor(row.category), color: '#fff' }">
              {{ row.category }}
            </el-tag>
            <el-button v-if="isStudentOrTeacher && row.status === 1" type="primary" size="small" :icon="Calendar" @click.stop="openReserveDialog(row)">预约</el-button>
          </div>
        </div>
      </div>
      <el-empty v-if="tableData.length === 0" description="暂无实验室数据" />
    </div>

    <el-card v-else class="table-card" shadow="never">
      <el-table ref="labTableRef" :data="tableData" v-loading="loading" style="width: 100%"
        @selection-change="handleLabSelectionChange" highlight-current-row>
        <el-table-column type="selection" width="48" />
        <el-table-column label="序号" width="60" align="center">
          <template #default="{ $index }">
            <span class="seq">{{ (page - 1) * size + $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="160">
          <template #default="{ row }">
            <div class="table-lab-cell">
              <div class="mini-icon" :style="{ background: getCategoryGradient(row.category) }">
                <el-icon :size="14" color="#fff"><OfficeBuilding /></el-icon>
              </div>
              <div class="lab-info">
                <div class="lab-name-text">{{ row.name }}</div>
                <div class="lab-code-text">{{ row.code }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            <el-tag effect="dark" round :style="{ background: getCategoryColor(row.category) }">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="位置" min-width="140" />
        <el-table-column prop="capacity" label="容量" width="80" align="center" />
        <el-table-column label="负责人" width="180">
          <template #default="{ row }">
            <div class="user-cell-inline">
              <div class="mini-avatar">{{ getUserName(row.managerId)?.charAt(0) || '?' }}</div>
              <div>
                <div class="u-name">{{ getUserName(row.managerId) || '未分配' }}</div>
                <div class="u-phone">{{ getUserPhone(row.managerId) }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success" effect="dark" round>启用</el-tag>
            <el-tag v-else type="info" effect="dark" round>停用</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="图片" width="100" align="center">
          <template #default="{ row }">
            <el-image v-if="row.imageUrl" :src="row.imageUrl" :preview-src-list="[row.imageUrl]"
              style="width:60px;height:60px;border-radius:10px" fit="cover" />
            <div v-else class="no-image"><el-icon><Picture /></el-icon></div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" :icon="Delete" plain @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <div class="pager-bar">
      <span class="pager-info">共 <b>{{ total }}</b> 条记录</span>
      <el-pagination
        v-model:current-page="page" v-model:page-size="size" :total="total"
        :page-sizes="[10, 20, 50]" layout="sizes, prev, pager, next, jumper"
        @size-change="fetchData" @current-change="fetchData" background
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close class="modern-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入实验室名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="编号" prop="code">
              <el-input v-model="form.code" placeholder="如 LAB-001" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-select v-model="form.category" placeholder="选择分类" style="width: 100%">
                <el-option label="计算机" value="计算机" />
                <el-option label="物理" value="物理" />
                <el-option label="化学" value="化学" />
                <el-option label="电子" value="电子" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="容量">
              <el-input-number v-model="form.capacity" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="位置" prop="location">
          <el-input v-model="form.location" placeholder="如 教学楼 B 栋 301" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="form.managerId" placeholder="选择负责人" clearable filterable style="width: 100%">
            <el-option v-for="u in users" :key="u.id" :value="u.id"
              :label="u.username + ' (' + (u.phone || '无手机号') + ')'" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio-button :value="1">启用</el-radio-button>
            <el-radio-button :value="0">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="实验室简介（选填）" />
        </el-form-item>
        <el-form-item label="图片">
          <div class="upload-area">
            <div v-if="form.imageUrl" class="preview-box">
              <el-image :src="form.imageUrl" style="width:100%;height:100%;border-radius:10px" fit="cover" />
              <div class="preview-actions">
                <el-button size="small" type="danger" circle :icon="Delete" @click="removeImage" />
              </div>
            </div>
            <el-upload v-else :before-upload="beforeLabUpload" :http-request="handleLabUpload" :show-file-list="false"
              accept="image/*">
              <div class="upload-placeholder">
                <el-icon :size="28"><Plus /></el-icon>
                <span>点击上传图片</span>
                <span class="upload-hint">支持 JPG / PNG，≤2MB</span>
              </div>
            </el-upload>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>
    <!-- 实验室预约对话框 (学生/教师) -->
    <el-dialog v-if="!canManageLab" v-model="reserveDialogVisible" title="预约实验室" width="520px" class="modern-dialog">
      <div class="dialog-hero">
        <div class="dialog-hero-icon" style="background: linear-gradient(135deg, #3b82f6, #0ea5e9);">
          <el-icon :size="28"><Calendar /></el-icon>
        </div>
        <div>
          <h4>预约实验室：{{ reserveTarget?.name }}</h4>
          <p>选择日期和时段，提交后等待管理员审批</p>
        </div>
      </div>
      <el-form ref="reserveFormRef" :model="reserveForm" :rules="reserveRules" label-width="100px">
        <el-form-item label="预约日期" prop="reserveDate">
          <el-date-picker v-model="reserveForm.reserveDate" type="date" placeholder="选择日期" :disabled-date="disableReservePast" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-time-picker v-model="reserveForm.startTime" format="HH:mm" value-format="HH:mm" placeholder="08:00" style="width:100%" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-time-picker v-model="reserveForm.endTime" format="HH:mm" value-format="HH:mm" placeholder="20:00" style="width:100%" />
        </el-form-item>
        <el-form-item label="预约用途" prop="purpose">
          <el-input v-model="reserveForm.purpose" type="textarea" :rows="3" placeholder="说明使用目的（如：物理实验课、学术研究等）" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reserveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reserveSubmitting" @click="handleReserveSubmit">提交预约</el-button>
      </template>
    </el-dialog>

    <!-- 我的预约列表 -->
    <el-dialog v-if="!canManageLab" v-model="myReserveDialogVisible" title="我的实验室预约" width="720px" class="modern-dialog">
      <el-table :data="myReservations" v-loading="reserveLoading" style="width:100%">
        <el-table-column prop="labName" label="实验室" min-width="120" />
        <el-table-column prop="reserveDate" label="日期" width="110" />
        <el-table-column label="时段" width="140">
          <template #default="{ row }">{{ row.startTime }} - {{ row.endTime }}</template>
        </el-table-column>
        <el-table-column prop="purpose" label="用途" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="['warning','success','danger','info'][row.status]" size="small">
              {{ ['待审批','已通过','已拒绝','已取消'][row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" link type="danger" size="small" @click="cancelReserve(row.id)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 预约审批管理 (管理员/实验室管理员) -->
    <el-dialog v-if="canManageLab" v-model="reserveAdminVisible" title="实验室预约审批" width="860px" class="modern-dialog">
      <div class="reserve-admin-toolbar">
        <el-tag>共 {{ adminReserveTotal }} 条预约</el-tag>
        <el-select v-model="reserveAdminFilter" placeholder="全部状态" clearable style="width:140px" @change="loadAdminReservations">
          <el-option label="待审批" :value="0" />
          <el-option label="已通过" :value="1" />
          <el-option label="已拒绝" :value="2" />
        </el-select>
      </div>
      <el-table :data="adminReservations" v-loading="adminLoading" style="width:100%">
        <el-table-column prop="username" label="预约人" width="100" />
        <el-table-column label="实验室" width="140">
          <template #default="{ row }">{{ row.labName }}</template>
        </el-table-column>
        <el-table-column prop="reserveDate" label="日期" width="110" />
        <el-table-column label="时段" width="130">
          <template #default="{ row }">{{ row.startTime }} - {{ row.endTime }}</template>
        </el-table-column>
        <el-table-column prop="purpose" label="用途" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 0" type="warning" size="small">待审批</el-tag>
            <el-tag v-else-if="row.status === 1" type="success" size="small">已通过</el-tag>
            <el-tag v-else-if="row.status === 2" type="danger" size="small">已拒绝</el-tag>
            <el-tag v-else type="info" size="small">已取消</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审批备注" prop="approveRemark" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button link type="success" size="small" @click="handleAdminApprove(row, 1)">通过</el-button>
              <el-button link type="danger" size="small" @click="handleAdminApprove(row, 2)">拒绝</el-button>
            </template>
            <template v-else>
              <el-button link size="small" disabled>已处理</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { getLabList, addLab, updateLab, deleteLab } from '@/api/lab'
import { getUserList, getAllUsers } from '@/api/auth'
import { applyReservation, getReservationList, approveReservation, cancelReservation } from '@/api/labReservation'
import { uploadImage, deleteImage } from '@/api/upload'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { Plus, Search, Refresh, Edit, Delete, OfficeBuilding, Location, User, Aim, Picture, Calendar } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const users = ref([])
const labTableRef = ref(null)
const selectedLabs = ref([])
const userStore = useUserStore()
const role = computed(() => userStore.userInfo?.role || 'STUDENT')
const isStudentOrTeacher = computed(() => role.value === 'STUDENT' || role.value === 'TEACHER')
const isTeacher = computed(() => role.value === 'TEACHER')
const canManageLab = computed(() => role.value === 'ADMIN' || role.value === 'LAB_ADMIN')
const viewMode = ref(isStudentOrTeacher.value ? 'grid' : 'table')

const searchForm = reactive({ keyword: '', category: '' })

const handleLabSelectionChange = (rows) => { selectedLabs.value = rows }
const handleLabSelectAll = (checked) => {
  if (checked) tableData.value.forEach(row => labTableRef.value?.toggleRowSelection(row, true))
  else labTableRef.value?.clearSelection()
}
const handleLabInvertSelect = () => {
  tableData.value.forEach(row => labTableRef.value?.toggleRowSelection(row, !selectedLabs.value.includes(row)))
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm('确认删除选中的 ' + selectedLabs.value.length + ' 个实验室？', '批量删除', { type: 'warning' })
    for (const row of selectedLabs.value) await deleteLab(row.id)
    ElMessage.success('批量删除成功')
    selectedLabs.value = []
    fetchData()
  } catch (e) {}
}

const dialogVisible = ref(false)
const dialogTitle = ref('新增实验室')
const isEdit = ref(false)
const editId = ref(null)
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({
  name: '', code: '', category: '', location: '', capacity: 0,
  managerId: null, description: '', imageUrl: '', status: 1,
})

const rules = {
  name: [{ required: true, message: '请输入实验室名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入编号', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  location: [{ required: true, message: '请输入位置', trigger: 'blur' }],
}

const categoryColors = {
  '计算机': '#2563eb',
  '物理': '#0ea5e9',
  '化学': '#3b82f6',
  '生物': '#1d4ed8',
  '电子': '#1e40af',
  '机械': '#0c2340',
  '其他': '#60a5fa',
}
const getCategoryColor = (c) => categoryColors[c] || '#2563eb'
const getCategoryGradient = (c) => {
  const base = getCategoryColor(c)
  return `linear-gradient(135deg, ${base}, ${base}dd)`
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getLabList({ page: page.value, size: size.value, ...searchForm })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.category = ''
  fetchData()
}

const handleAdd = () => {
  isEdit.value = false; editId.value = null
  dialogTitle.value = '新增实验室'
  resetForm()
  dialogVisible.value = true
}

const handleCardClick = (row) => {
  if (isStudentOrTeacher.value) {
    ElMessageBox.alert(
      `<div style="line-height:2;color:#334155;">
        <p><b>实验室:</b> ${row.name}</p>
        <p><b>编号:</b> ${row.code}</p>
        <p><b>分类:</b> ${row.category}</p>
        <p><b>位置:</b> ${row.location}</p>
        <p><b>容量:</b> ${row.capacity} 人</p>
        <p><b>状态:</b> ${row.status === 1 ? '<span style=color:#2563eb>启用中</span>' : '<span style=color:#64748b>已停用</span>'}</p>
        <p><b>负责人:</b> ${getUserName(row.managerId) || '未分配'}</p>
      </div>`,
      '实验室详情',
      { dangerouslyUseHTMLString: true, confirmButtonText: '知道了', showCancelButton: false }
    )
  } else {
    handleEdit(row)
  }
}

const handleEdit = (row) => {
  isEdit.value = true; editId.value = row.id
  dialogTitle.value = '编辑实验室'
  Object.assign(form, row)
  dialogVisible.value = true
}

const resetForm = () => {
  if (formRef.value) formRef.value.resetFields()
  Object.assign(form, {
    name: '', code: '', category: '', location: '', capacity: 0,
    managerId: null, description: '', imageUrl: '', status: 1,
  })
}

const beforeLabUpload = (file) => {
  if (!file.type.startsWith('image/')) { ElMessage.error('只能上传图片文件'); return false }
  if (file.size / 1024 / 1024 > 2) { ElMessage.error('图片大小不能超过2MB'); return false }
  return true
}

const getUserName = (managerId) => {
  if (!managerId) return ''
  const u = users.value.find(u => u.id === managerId)
  return u ? u.username : ''
}

const getUserPhone = (managerId) => {
  if (!managerId) return ''
  const u = users.value.find(u => u.id === managerId)
  return u ? u.phone : ''
}

const getImageUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return encodeURI(url)
}

const removeImage = async () => {
  if (form.imageUrl) await deleteImage(form.imageUrl).catch(() => {})
  form.imageUrl = ''
}

const handleLabUpload = async (options) => {
  try {
    const res = await uploadImage(options.file, 'lab')
    form.imageUrl = res.data.url
    ElMessage.success('上传成功')
  } catch (e) { ElMessage.error('上传失败') }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateLab(editId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      await addLab({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) { console.error(e) }
  finally { submitting.value = false }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该实验室吗？', '提示', { type: 'warning' })
    .then(async () => { await deleteLab(row.id); ElMessage.success('删除成功'); fetchData() })
    .catch(() => {})
}

onMounted(async () => {
  const userRes = await getAllUsers().catch(() => ({ data: [] }))
  users.value = userRes.data || []
  fetchData()
})

const reserveDialogVisible = ref(false)
const myReserveDialogVisible = ref(false)
const reserveSubmitting = ref(false)
const reserveLoading = ref(false)
const reserveFormRef = ref(null)
const reserveTarget = ref(null)
const myReservations = ref([])
const reserveForm = reactive({ reserveDate: '', startTime: '08:00', endTime: '20:00', purpose: '' })
const reserveRules = {
  reserveDate: [{ required: true, message: '请选择预约日期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  purpose: [{ required: true, message: '请填写预约用途', trigger: 'blur' }],
}
const disableReservePast = (time) => {
  const today = new Date(); today.setHours(0, 0, 0, 0)
  return time.getTime() < today.getTime()
}
const openReserveDialog = (row) => {
  reserveTarget.value = row
  reserveForm.reserveDate = ''
  reserveForm.startTime = '08:00'
  reserveForm.endTime = '20:00'
  reserveForm.purpose = ''
  reserveDialogVisible.value = true
}
const handleReserveSubmit = async () => {
  const valid = await reserveFormRef.value.validate().catch(() => false)
  if (!valid) return
  reserveSubmitting.value = true
  try {
    await applyReservation({ labId: reserveTarget.value.id, ...reserveForm })
    ElMessage.success('预约提交成功，等待审批')
    reserveDialogVisible.value = false
  } catch (e) { console.error(e) }
  finally { reserveSubmitting.value = false }
}
const openMyReservations = async () => {
  reserveLoading.value = true
  try {
    const res = await getReservationList({ page: 1, size: 50 })
    myReservations.value = res.data?.records || []
    myReserveDialogVisible.value = true
  } catch (e) { console.error(e) }
  finally { reserveLoading.value = false }
}
const cancelReserve = async (id) => {
  await ElMessageBox.confirm('确定取消该预约？', '提示', { type: 'warning' }).catch(() => false)
  await cancelReservation(id).catch(() => {})
  ElMessage.success('已取消')
  openMyReservations()
}

const reserveAdminVisible = ref(false)
const reserveAdminFilter = ref(null)
const adminLoading = ref(false)
const adminReservations = ref([])
const adminReserveTotal = ref(0)
const openReserveAdminPanel = async () => {
  reserveAdminVisible.value = true
  await loadAdminReservations()
}
const loadAdminReservations = async () => {
  adminLoading.value = true
  try {
    const params = { page: 1, size: 100 }
    if (reserveAdminFilter.value !== null && reserveAdminFilter.value !== '') params.status = reserveAdminFilter.value
    const res = await getReservationList(params)
    adminReservations.value = res.data?.records || []
    adminReserveTotal.value = res.data?.total || 0
  } catch (e) { console.error(e) }
  finally { adminLoading.value = false }
}
const handleAdminApprove = async (row, status) => {
  try {
    await approveReservation({ id: row.id, status, remark: '' })
    ElMessage.success(status === 1 ? '已通过该预约' : '已拒绝该预约')
    loadAdminReservations()
  } catch (e) { console.error(e) }
}
</script>

<style scoped>
.page-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: linear-gradient(135deg, #1e40af 0%, #0ea5e9 100%);
  border-radius: 20px;
  color: #fff;
  box-shadow: 0 10px 32px rgba(14, 165, 233, 0.25);
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

.toolbar-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  background: #fff;
  border-radius: 14px;
  border: 1px solid var(--border-light);
}
.search-fields { display: flex; align-items: center; gap: 12px; }
.batch-bar {
  display: flex; align-items: center; gap: 10px;
  padding: 6px 14px;
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  border-radius: 10px;
  animation: fadeIn 0.3s;
}
.batch-count { color: #92400e; font-size: 13px; }
.batch-count b { font-weight: 700; }

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

.lab-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 18px;
}

.lab-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--border-light);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.lab-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.1);
  border-color: transparent;
}

.lab-cover {
  height: 140px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.5);
  overflow: hidden;
}

.lab-cover-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 40%, rgba(0, 0, 0, 0.35) 100%);
  pointer-events: none;
}

.lab-cover-icon {
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.2));
  position: relative;
  z-index: 1;
}

.status-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  backdrop-filter: blur(10px);
}
.status-badge.active { background: rgba(37, 99, 235, 0.9); color: #fff; }
.status-badge.inactive { background: rgba(148, 163, 184, 0.9); color: #fff; }

.lab-body { padding: 16px 18px; }
.lab-name { font-size: 16px; font-weight: 600; color: #0f172a; }
.lab-code { font-size: 12px; color: #94a3b8; font-family: Consolas, monospace; margin-top: 2px; }
.lab-meta {
  display: flex; flex-direction: column; gap: 6px;
  margin-top: 12px; font-size: 12px; color: #64748b;
}
.meta-item { display: flex; align-items: center; gap: 5px; }
.meta-item .el-icon { color: #94a3b8; }

.lab-footer {
  display: flex; justify-content: space-between; align-items: center;
  margin-top: 14px; padding-top: 12px; border-top: 1px solid var(--border-light);
}
.capacity-badge {
  font-size: 12px; color: #64748b; display: flex; align-items: center; gap: 4px;
}
.capacity-badge .el-icon { color: #2563eb; }

.table-card { border-radius: 14px; border: 1px solid var(--border-light); }
.table-card :deep(.el-card__body) { padding: 0; }

.seq {
  display: inline-block; width: 22px; height: 22px; line-height: 22px;
  border-radius: 50%; background: #f0f4ff; color: #2563eb;
  font-size: 12px; font-weight: 600;
}

.table-lab-cell { display: flex; align-items: center; gap: 10px; }
.mini-icon {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.lab-info { min-width: 0; }
.lab-name-text { font-weight: 500; color: #0f172a; }
.lab-code-text { font-size: 12px; color: #94a3b8; font-family: Consolas, monospace; }

.user-cell-inline { display: flex; align-items: center; gap: 8px; }
.mini-avatar {
  width: 30px; height: 30px; border-radius: 8px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff; font-size: 13px; font-weight: 600;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.u-name { font-size: 13px; color: #334155; }
.u-phone { font-size: 11px; color: #94a3b8; }

.no-image {
  width: 60px; height: 60px;
  background: #f1f5f9; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: #cbd5e1;
}

.pager-bar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 4px;
}
.pager-info { font-size: 13px; color: #64748b; }
.pager-info b { color: #0f172a; font-weight: 600; }

.modern-dialog :deep(.el-dialog) { border-radius: 20px; overflow: hidden; }
.modern-dialog :deep(.el-dialog__header) {
  margin: 0; padding: 20px 24px;
  background: linear-gradient(135deg, #1e40af 0%, #0ea5e9 100%);
  color: #fff;
}
.modern-dialog :deep(.el-dialog__body) { padding: 24px 24px 8px; }
.modern-dialog :deep(.el-dialog__footer) { padding: 16px 24px; border-top: 1px solid #f1f5f9; }

.upload-area { display: flex; gap: 12px; }
.upload-placeholder {
  width: 140px; height: 140px;
  border: 2px dashed #cbd5e1;
  border-radius: 12px;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px;
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.2s;
}
.upload-placeholder:hover { border-color: #2563eb; color: #2563eb; background: #f0f4ff; }
.upload-placeholder .upload-hint { font-size: 11px; }

.preview-box {
  width: 140px; height: 140px;
  border-radius: 12px; overflow: hidden; position: relative;
}
.preview-actions {
  position: absolute; bottom: 8px; right: 8px;
  opacity: 0; transition: opacity 0.2s;
}
.preview-box:hover .preview-actions { opacity: 1; }

.dialog-footer { display: flex; justify-content: flex-end; gap: 10px; }
</style>