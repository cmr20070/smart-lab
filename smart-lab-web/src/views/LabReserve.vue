<template>
  <div class="page-container">
    <div class="page-hero">
      <div class="hero-left">
        <div class="hero-icon">
          <el-icon :size="26"><OfficeBuilding /></el-icon>
        </div>
        <div>
          <h2 class="hero-title">实验室预约</h2>
          <p class="hero-sub">查看实验室信息 → 点击预约按钮提交预约申请</p>
        </div>
      </div>
      <div class="hero-actions">
        <el-button plain size="large" :icon="List" @click="openMyReservations">我的预约</el-button>
      </div>
    </div>

    <div class="lab-grid-head">
      <h3 class="grid-title">实验室列表</h3>
      <el-input v-model="keyword" placeholder="搜索实验室名称/编号" clearable :prefix-icon="Search"
        style="width: 260px" @keyup.enter="loadLabs" @clear="loadLabs" />
    </div>

    <div v-loading="labLoading" class="lab-grid">
      <div v-for="lab in filteredLabs" :key="lab.id" class="lab-card">
        <div class="lab-card-cover" :style="lab.imageUrl ? { backgroundImage: `url(${getImageUrl(lab.imageUrl)})`, backgroundSize: 'cover', backgroundPosition: 'center' } : { background: getCategoryGradient(lab.category) }">
          <el-icon v-if="!lab.imageUrl" :size="34" color="rgba(255,255,255,0.9)"><OfficeBuilding /></el-icon>
          <div v-if="lab.imageUrl" class="cover-shade"></div>
          <span class="cover-tag">{{ lab.category }}</span>
        </div>
        <div class="lab-card-body">
          <div class="lab-title-row">
            <h4 class="lab-name">{{ lab.name }}</h4>
            <el-tag v-if="lab.status === 1" type="success" size="small" effect="light">启用中</el-tag>
            <el-tag v-else type="info" size="small" effect="light">已停用</el-tag>
          </div>
          <div class="lab-code">{{ lab.code }}</div>
          <div class="lab-info-list">
            <div class="lab-info-item">
              <el-icon :size="14" color="#64748b"><Location /></el-icon>
              <span>{{ lab.location || '未填写地址' }}</span>
            </div>
            <div class="lab-info-item">
              <el-icon :size="14" color="#64748b"><User /></el-icon>
              <span>容量 {{ lab.capacity }} 人</span>
            </div>
            <div class="lab-info-item" v-if="lab.description">
              <el-icon :size="14" color="#64748b"><Document /></el-icon>
              <span class="lab-desc">{{ lab.description }}</span>
            </div>
          </div>
          <div class="lab-action">
            <el-button type="primary" :icon="Calendar" :disabled="lab.status !== 1"
              style="width: 100%" @click="openReserveDialog(lab)">
              预约
            </el-button>
          </div>
        </div>
      </div>
      <el-empty v-if="!labLoading && filteredLabs.length === 0" description="暂无可预约实验室" style="grid-column: 1 / -1" />
    </div>

    <!-- 预约弹窗 -->
    <el-dialog v-model="reserveVisible" :title="`预约 · ${selectedLab?.name || ''}`" width="520px" destroy-on-close class="reserve-dialog">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="实验室">
          <div v-if="selectedLab" class="sel-lab">
            <span class="sl-name">{{ selectedLab.name }}</span>
            <span class="sl-sub">{{ selectedLab.code }} · {{ selectedLab.location }} · 容量{{ selectedLab.capacity }}</span>
          </div>
        </el-form-item>

        <el-form-item label="日期模式">
          <el-radio-group v-model="dateMode">
            <el-radio-button value="single">单日预约</el-radio-button>
            <el-radio-button value="range">多日预约</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="dateMode === 'single'" label="预约日期" prop="reserveDate">
          <el-date-picker v-model="form.reserveDate" type="date" placeholder="选择日期"
            value-format="YYYY-MM-DD" style="width: 100%" :disabled-date="disablePast" />
        </el-form-item>
        <el-form-item v-else label="日期范围">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期"
            end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 100%"
            :disabled-date="disablePast" :disabled-range="disableRangeLimit" />
          <div class="form-hint">单次最长30天</div>
        </el-form-item>

        <el-form-item label="时间段">
          <div class="time-row">
            <el-time-picker v-model="form.startTime" format="HH:mm" value-format="HH:mm" placeholder="开始" style="flex:1" />
            <span class="time-sep">至</span>
            <el-time-picker v-model="form.endTime" format="HH:mm" value-format="HH:mm" placeholder="结束" style="flex:1" />
          </div>
        </el-form-item>

        <el-form-item label="使用天数">
          <div class="days-display">
            <b>{{ totalDays }}</b> <span>天</span>
            <span v-if="totalDays > 30" class="days-warn">⚠ 超过30天限制</span>
          </div>
        </el-form-item>

        <el-form-item label="预约用途" prop="purpose">
          <el-input v-model="form.purpose" type="textarea" :rows="4" placeholder="请填写预约用途（如：XX实验课、XX项目研发）" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reserveVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="totalDays === 0 || totalDays > 30" @click="submit">提交预约</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showMyReservations" width="800px">
      <template #header><b>我的实验室预约</b></template>
      <el-table :data="myReservations" v-loading="myLoading" style="width: 100%">
        <el-table-column prop="labName" label="实验室" min-width="140" />
        <el-table-column label="日期范围" min-width="180">
          <template #default="{ row }">
            {{ row.reserveDate }}{{ row.endDate && row.endDate !== row.reserveDate ? ' ~ ' + row.endDate : '' }}
          </template>
        </el-table-column>
        <el-table-column label="时段" width="140">
          <template #default="{ row }">{{ row.startTime || '08:00' }} ~ {{ row.endTime || '20:00' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="reserveStatusType(row.status)" size="small">{{ reserveStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" v-if="isStudentOrTeacher">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" size="small" type="danger" link @click="handleCancelReservation(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { OfficeBuilding, Search, List, Calendar, Location, User, Document } from '@element-plus/icons-vue'
import { getAllLabs } from '@/api/lab'
import { applyReservation, getReservationList, cancelReservation } from '@/api/labReservation'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const isStudentOrTeacher = computed(() => ['STUDENT', 'TEACHER'].includes(userStore.userInfo?.role))

const keyword = ref('')
const labLoading = ref(false)
const labList = ref([])
const reserveVisible = ref(false)
const selectedLab = ref(null)
const dateMode = ref('single')
const dateRange = ref(null)
const formRef = ref(null)
const submitting = ref(false)

const showMyReservations = ref(false)
const myLoading = ref(false)
const myReservations = ref([])

const form = reactive({
  reserveDate: '',
  startTime: '08:00',
  endTime: '20:00',
  purpose: '',
})

const rules = {
  reserveDate: [{ required: true, message: '请选择预约日期', trigger: 'change' }],
  purpose: [{ required: true, message: '请填写预约用途', trigger: 'blur' }],
}

const filteredLabs = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return labList.value
  return labList.value.filter(l =>
    (l.name || '').toLowerCase().includes(kw) ||
    (l.code || '').toLowerCase().includes(kw) ||
    (l.location || '').toLowerCase().includes(kw))
})

const totalDays = computed(() => {
  if (dateMode.value === 'single') {
    return form.reserveDate ? 1 : 0
  } else {
    if (!dateRange.value || dateRange.value.length !== 2) return 0
    const d1 = new Date(dateRange.value[0]).getTime()
    const d2 = new Date(dateRange.value[1]).getTime()
    return Math.ceil((d2 - d1) / (1000 * 60 * 60 * 24)) + 1
  }
})

const disablePast = (date) => {
  const now = new Date()
  return date.getTime() < now.setHours(0, 0, 0, 0)
}
const disableRangeLimit = (dates) => {
  if (!dates || dates.length !== 2) return false
  const d1 = new Date(dates[0]).getTime()
  const d2 = new Date(dates[1]).getTime()
  return Math.ceil((d2 - d1) / (1000 * 60 * 60 * 24)) + 1 > 30
}

const loadLabs = async () => {
  labLoading.value = true
  try {
    const res = await getAllLabs()
    labList.value = res.data || []
  } catch (e) {
    ElMessage.error('加载实验室失败')
  } finally {
    labLoading.value = false
  }
}

const getCategoryGradient = (cat) => {
  const map = {
    '计算机': 'linear-gradient(120deg, #2563eb, #1d4ed8)',
    '物理': 'linear-gradient(120deg, #0ea5e9, #0369a1)',
    '化学': 'linear-gradient(120deg, #10b981, #047857)',
    '电子': 'linear-gradient(120deg, #f59e0b, #d97706)',
  }
  return map[cat] || 'linear-gradient(120deg, #6366f1, #4f46e5)'
}

const getImageUrl = (url) => {
  if (!url) return ''
  if (/^https?:\/\//.test(url)) return url
  return encodeURI(url)
}

const openReserveDialog = (lab) => {
  selectedLab.value = lab
  resetForm()
  reserveVisible.value = true
}

const resetForm = () => {
  form.reserveDate = ''
  form.startTime = '08:00'
  form.endTime = '20:00'
  form.purpose = ''
  dateRange.value = null
  dateMode.value = 'single'
  formRef.value?.clearValidate?.()
}

const submit = async () => {
  if (!selectedLab.value) return ElMessage.warning('请选择实验室')
  if (!form.purpose.trim()) return ElMessage.warning('请填写预约用途')
  if (totalDays.value > 30) return ElMessage.warning('单次预约最长为30天')
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      labId: selectedLab.value.id,
      purpose: form.purpose.trim(),
      startTime: form.startTime || '08:00',
      endTime: form.endTime || '20:00',
    }
    if (dateMode.value === 'single') {
      payload.reserveDate = form.reserveDate
      payload.startDate = form.reserveDate
      payload.endDate = form.reserveDate
    } else {
      payload.startDate = dateRange.value[0]
      payload.endDate = dateRange.value[1]
    }
    const res = await applyReservation(payload)
    const days = res.data || totalDays.value
    ElMessage.success(`预约成功，共 ${days} 天，等待审批`)
    reserveVisible.value = false
    resetForm()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

const openMyReservations = async () => {
  showMyReservations.value = true
  await loadMyReservations()
}

const loadMyReservations = async () => {
  myLoading.value = true
  try {
    const res = await getReservationList({ page: 1, size: 50 })
    myReservations.value = res.data?.records || res.data || []
  } catch (e) {}
  finally { myLoading.value = false }
}

const reserveStatusType = (s) => [undefined, 'warning', 'success', 'danger', 'info'][s] || 'info'
const reserveStatusLabel = (s) => ['待审批', '待审批', '已通过', '已拒绝', '已取消'][s] || '未知'

const handleCancelReservation = async (row) => {
  try {
    await cancelReservation(row.id)
    ElMessage.success('已取消')
    loadMyReservations()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '取消失败')
  }
}

onMounted(loadLabs)
</script>

<style scoped>
.page-container { padding-bottom: 24px; }
.page-hero {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 28px; margin-bottom: 20px;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  border-radius: 20px; color: #fff;
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.25);
}
.hero-left { display: flex; align-items: center; gap: 18px; }
.hero-icon {
  width: 52px; height: 52px; border-radius: 14px;
  background: rgba(255,255,255,0.2);
  display: flex; align-items: center; justify-content: center;
}
.hero-title { margin: 0; font-size: 20px; font-weight: 700; }
.hero-sub { margin: 4px 0 0; font-size: 13px; opacity: 0.85; }

.lab-grid-head {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px;
}
.grid-title { margin: 0; font-size: 16px; font-weight: 600; color: #0f172a; }

.lab-grid {
  display: grid; grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
.lab-card {
  background: #fff; border-radius: 16px; overflow: hidden;
  border: 1px solid #eef2f7;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.04);
  transition: all 0.25s ease; display: flex; flex-direction: column;
}
.lab-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 30px rgba(37, 99, 235, 0.12);
}
.lab-card-cover {
  height: 150px; position: relative;
  display: flex; align-items: center; justify-content: center;
  background: #e2e8f0;
}
.cover-shade {
  position: absolute; inset: 0;
  background: linear-gradient(180deg, transparent 50%, rgba(0,0,0,0.35) 100%);
  pointer-events: none;
}
.cover-tag {
  position: absolute; top: 10px; right: 10px;
  background: rgba(255,255,255,0.25); color: #fff;
  font-size: 12px; padding: 2px 10px; border-radius: 20px;
  backdrop-filter: blur(4px);
}
.lab-card-body { padding: 16px; display: flex; flex-direction: column; flex: 1; }
.lab-title-row { display: flex; align-items: center; gap: 8px; }
.lab-name { margin: 0; font-size: 16px; font-weight: 700; color: #0f172a; }
.lab-code { font-size: 12px; color: #94a3b8; margin-top: 4px; letter-spacing: 0.5px; }
.lab-info-list { margin: 12px 0; display: flex; flex-direction: column; gap: 8px; flex: 1; }
.lab-info-item { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #475569; }
.lab-desc { overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.lab-action { margin-top: 8px; }

.reserve-dialog :deep(.el-dialog__title) { font-weight: 600; color: #1d4ed8; }
.reserve-dialog :deep(.el-dialog__header) { padding-bottom: 12px; border-bottom: 1px solid #f1f5f9; }

.sel-lab {
  display: flex; flex-direction: column; gap: 4px;
  padding: 10px 14px; background: linear-gradient(135deg, #eff6ff, #dbeafe);
  border-radius: 10px; width: 100%;
}
.sl-name { font-weight: 600; color: #1d4ed8; }
.sl-sub { font-size: 12px; color: #3b82f6; }

.time-row { display: flex; align-items: center; gap: 10px; width: 100%; }
.time-sep { color: #94a3b8; }
.days-display { font-size: 14px; color: #475569; }
.days-display b { font-size: 22px; color: #1d4ed8; margin: 0 2px; }
.days-display b + span { color: #64748b; }
.days-warn { color: #ef4444; margin-left: 10px; font-size: 13px; }
.form-hint { font-size: 12px; color: #94a3b8; margin-top: 4px; }

@media (max-width: 700px) {
  .lab-grid { grid-template-columns: 1fr; }
  .lab-grid-head { flex-direction: column; align-items: stretch; gap: 12px; }
}
</style>