<template>
  <div class="page-container">
    <div class="page-hero">
      <div class="hero-icon">
        <el-icon :size="26"><UserFilled /></el-icon>
      </div>
      <div>
        <h2 class="hero-title">👤 代学生申请</h2>
        <p class="hero-sub">先选设备 → 选学生 → 填用途 · 分步向导代提交借用申请</p>
      </div>
    </div>

    <!-- 步骤① 设备选择 -->
    <div class="sg-card sg-card-full">
      <div class="sg-head">
        <span class="sg-num">1</span>选择设备
        <span class="sg-head-tip">点击设备卡片自动弹出代申请向导</span>
      </div>

      <div class="dev-tools">
        <el-input v-model="keyword" placeholder="搜索设备名称 / 编号 / 分类" clearable :prefix-icon="Search"
          style="width: 320px" @keyup.enter="searchDevices" @clear="searchDevices" />
        <span class="dev-tools-count">空闲设备 <b class="hl">{{ deviceTotal }}</b> 台</span>
      </div>

      <div class="dev-grid" v-loading="deviceLoading">
        <div v-for="d in deviceList" :key="d.id" class="dd-card" :class="{ active: form.deviceId === d.id }"
          @click="pickDevice(d)">
          <div class="dd-cover"
            :style="d.imageUrl ? { backgroundImage: `url(${getImageUrl(d.imageUrl)})` } : {}">
            <el-icon v-if="!d.imageUrl" :size="34"><Monitor /></el-icon>
            <div class="dd-status">
              <el-tag size="small" type="success" effect="dark">空闲</el-tag>
            </div>
            <div v-if="form.deviceId === d.id" class="dd-checked">
              <el-icon :size="22"><Check /></el-icon>
            </div>
          </div>
          <div class="dd-body">
            <div class="dd-name" :title="d.name">{{ d.name }}</div>
            <div class="dd-code">{{ d.code }} · {{ d.category || '-' }}</div>
            <div class="dd-meta">
              <span>{{ d.labName || '未分配实验室' }}</span>
              <template v-if="d.model">· {{ d.model }}</template>
            </div>
            <div class="dd-desc" v-if="d.description" :title="d.description">{{ d.description }}</div>
          </div>
        </div>
        <div v-if="!deviceLoading && deviceList.length === 0" class="dev-empty">
          <el-empty description="暂无空闲设备" />
        </div>
      </div>

      <div class="dev-pager">
        <span class="dev-pager-info">共 <b>{{ deviceTotal }}</b> 台空闲设备</span>
        <el-pagination v-model:current-page="devicePage" v-model:page-size="deviceSize" :total="deviceTotal"
          :page-sizes="[8, 12, 16, 24]" layout="sizes, prev, pager, next" background
          @size-change="loadDevices" @current-change="loadDevices" />
      </div>
    </div>

    <!-- 分步向导弹窗：② 选择学生 → ③ 填写信息 -->
    <el-dialog v-model="wizardVisible" :title="`代申请向导 · 已选：${selectedDevice?.name || ''}`" width="620px"
      top="8vh" class="modern-dialog" destroy-on-close append-to-body>
      <el-steps :active="wizardStep" finish-status="success" align-center class="wizard-steps">
        <el-step title="选择学生" />
        <el-step title="填写信息" />
      </el-steps>

      <!-- 步骤② 学生 -->
      <div v-if="wizardStep === 0" class="wizard-pane">
        <div class="wizard-device">
          <div class="wd-cover" :style="selectedDevice?.imageUrl ? { backgroundImage: `url(${getImageUrl(selectedDevice.imageUrl)})` } : {}">
            <el-icon v-if="!selectedDevice?.imageUrl" :size="22"><Monitor /></el-icon>
          </div>
          <div class="wd-info">
            <div class="wd-name">{{ selectedDevice?.name }}</div>
            <div class="wd-sub">{{ selectedDevice?.code }} · {{ selectedDevice?.category }}</div>
          </div>
        </div>

        <el-form label-position="top" class="wizard-form">
          <el-form-item label="选择学生" required>
            <div class="stu-cards" v-loading="studentLoading">
              <div v-for="s in studentList" :key="s.id" class="stu-card" :class="{ active: form.studentId === s.id }"
                @click="pickStudent(s.id)">
                <div class="stu-avatar-wrap">
                  <el-avatar v-if="s.avatar" :src="getImageUrl(s.avatar)" :size="42" class="stu-avatar-img" />
                  <div v-else class="stu-avatar" :style="{ background: avatarGradient(s.id) }">
                    {{ (s.realName || s.username)?.charAt(0)?.toUpperCase() }}
                  </div>
                </div>
                <div class="stu-main">
                  <span class="stu-name">{{ s.realName || s.username }}</span>
                  <span class="stu-role">学生</span>
                </div>
                <div class="stu-sub">
                  <span class="stu-id-tag">学号</span> {{ s.username }}
                  <span class="stu-phone" v-if="s.phone">· {{ maskPhone(s.phone) }}</span>
                </div>
                <el-icon v-if="form.studentId === s.id" class="stu-check"><Check /></el-icon>
              </div>
              <div v-if="!studentLoading && studentList.length === 0" class="stu-empty">
                <el-empty description="暂无学生用户" :image-size="60" />
              </div>
            </div>
          </el-form-item>
        </el-form>
      </div>

      <!-- 步骤③ 信息 -->
      <div v-if="wizardStep === 1" class="wizard-pane">
        <div class="wizard-stu-summary">
          <el-avatar v-if="selectedStudent?.avatar" :src="getImageUrl(selectedStudent.avatar)" :size="46" class="ws-avatar-img" />
          <div v-else class="ws-avatar" :style="{ background: avatarGradient(selectedStudent?.id) }">
            {{ (selectedStudent?.realName || selectedStudent?.username)?.charAt(0)?.toUpperCase() }}
          </div>
          <div>
            <div class="ws-name">{{ selectedStudent?.realName || selectedStudent?.username }}</div>
            <div class="ws-sub">学号 {{ selectedStudent?.username }}</div>
          </div>
          <el-tag size="small" effect="plain">借用额度 {{ maxBorrow - (studentActiveBorrows || 0) }}/{{ maxBorrow }}</el-tag>
        </div>

        <el-form :model="form" :rules="rules" ref="formRef" label-width="92px" class="wizard-form">
          <el-form-item label="借用时间" prop="borrowTime">
            <el-date-picker v-model="form.borrowTime" type="datetime" placeholder="选择开始时间"
              value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" :disabled-date="disablePast" />
          </el-form-item>
          <el-form-item label="预计归还" prop="expectReturnTime">
            <el-date-picker v-model="form.expectReturnTime" type="datetime" placeholder="最长30天"
              value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" :disabled-date="disablePast" />
          </el-form-item>
          <div class="hint-row">
            当前选中时长：<b :class="borrowDays > 30 ? 'warn' : 'ok'">{{ borrowDays }}</b> 天
            <span v-if="borrowDays > 30" class="warn-txt">⚠ 超过最长 30 天限制</span>
          </div>
          <el-form-item label="借用目的" prop="purpose">
            <el-input v-model="form.purpose" type="textarea" :rows="4" maxlength="200" show-word-limit
              placeholder="请填写用途（如：XX实验课、XX项目开发）" />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="wizardVisible = false">取消</el-button>
        <el-button v-if="wizardStep === 1" @click="wizardStep = 0">上一步</el-button>
        <el-button v-if="wizardStep === 0" type="primary" @click="nextToInfo">下一步</el-button>
        <el-button v-if="wizardStep === 1" type="primary" :loading="submitting" @click="submit">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { UserFilled, Search, Monitor, Check, ArrowRight } from '@element-plus/icons-vue'
import { getAllUsers } from '@/api/user'
import { getDeviceList } from '@/api/device'
import { applyBorrow } from '@/api/borrow'
import { getImageUrl } from '@/utils/image'

const studentLoading = ref(false)
const deviceLoading = ref(false)
const studentList = ref([])
const deviceList = ref([])
const devicePage = ref(1)
const deviceSize = ref(8)
const deviceTotal = ref(0)
const keyword = ref('')
const submitting = ref(false)
const formRef = ref(null)
const studentActiveBorrows = ref(0)
const maxBorrow = 3

const AVATAR_GRADIENTS = [
  'linear-gradient(135deg, #2563eb, #1d4ed8)',
  'linear-gradient(135deg, #0ea5e9, #3b82f6)',
  'linear-gradient(135deg, #7c3aed, #2563eb)',
  'linear-gradient(135deg, #0891b2, #2563eb)',
  'linear-gradient(135deg, #1e40af, #0ea5e9)',
]
const avatarGradient = (id) => AVATAR_GRADIENTS[((id ?? 0) % AVATAR_GRADIENTS.length + AVATAR_GRADIENTS.length) % AVATAR_GRADIENTS.length]
const maskPhone = (p) => (typeof p === 'string' && p.length === 11 ? p.slice(0, 3) + '****' + p.slice(7) : p || '')

const wizardVisible = ref(false)
const wizardStep = ref(0)

const form = reactive({
  studentId: null,
  deviceId: null,
  borrowTime: '',
  expectReturnTime: '',
  purpose: '',
})

const rules = {
  studentId: [{ required: true, message: '请选择学生', trigger: 'change' }],
  deviceId: [{ required: true, message: '请选择设备', trigger: 'change' }],
  borrowTime: [{ required: true, message: '请选择借用时间', trigger: 'change' }],
  expectReturnTime: [{ required: true, message: '请选择归还时间', trigger: 'change' }],
  purpose: [{ required: true, message: '请填写借用目的', trigger: 'blur' }],
}

const selectedDevice = computed(() => deviceList.value.find(d => d.id === form.deviceId))
const selectedStudent = computed(() => studentList.value.find(s => s.id === form.studentId))

const borrowDays = computed(() => {
  if (!form.borrowTime || !form.expectReturnTime) return 0
  const d1 = new Date(form.borrowTime).getTime()
  const d2 = new Date(form.expectReturnTime).getTime()
  return Math.max(0, Math.ceil((d2 - d1) / (1000 * 60 * 60 * 24)))
})

const disablePast = (date) => {
  const now = new Date()
  return date.getTime() < now.setHours(0, 0, 0, 0)
}

const loadStudents = async () => {
  studentLoading.value = true
  try {
    const res = await getAllUsers('STUDENT')
    studentList.value = res.data || []
  } catch (e) {
    ElMessage.error('加载学生列表失败')
  } finally {
    studentLoading.value = false
  }
}

const loadDevices = async () => {
  deviceLoading.value = true
  try {
    const params = { page: devicePage.value, size: deviceSize.value, status: 1, keyword: keyword.value || undefined }
    const res = await getDeviceList(params)
    deviceList.value = res.data?.records || []
    deviceTotal.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error('加载设备失败')
  } finally {
    deviceLoading.value = false
  }
}

const searchDevices = () => {
  devicePage.value = 1
  loadDevices()
}

const pickDevice = (d) => {
  form.deviceId = d.id
  openWizard()
}

const pickStudent = (id) => {
  form.studentId = id
}

const openWizard = () => {
  if (!form.deviceId) return ElMessage.warning('请先选择设备')
  wizardStep.value = 0
  wizardVisible.value = true
  if (studentList.value.length === 0) loadStudents()
}

const nextToInfo = async () => {
  if (!form.studentId) return ElMessage.warning('请先选择学生')
  wizardStep.value = 1
  await nextTick()
  formRef.value?.clearValidate()
}

const submit = async () => {
  if (borrowDays.value > 30) return ElMessage.warning('最长借用期为30天')
  await formRef.value.validate()
  submitting.value = true
  try {
    await applyBorrow({
      studentId: form.studentId,
      studentName: selectedStudent.value?.username,
      deviceId: form.deviceId,
      borrowTime: form.borrowTime,
      expectReturnTime: form.expectReturnTime,
      purpose: '[教师代申请] ' + form.purpose.trim(),
    })
    ElMessage.success('代申请已提交，等待审批')
    wizardVisible.value = false
    reset()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

const reset = () => {
  form.studentId = null
  form.deviceId = null
  form.borrowTime = ''
  form.expectReturnTime = ''
  form.purpose = ''
}

onMounted(() => {
  loadStudents()
  loadDevices()
})
</script>

<style scoped>
.page-container { padding-bottom: 24px; }
.page-hero {
  display: flex; align-items: center; gap: 18px;
  padding: 20px 28px; margin-bottom: 20px;
  background: linear-gradient(135deg, #0ea5e9 0%, #0369a1 100%);
  border-radius: 20px; color: #fff;
  box-shadow: 0 8px 24px rgba(14, 165, 233, 0.25);
}
.hero-icon {
  width: 52px; height: 52px; border-radius: 14px;
  background: rgba(255,255,255,0.2);
  display: flex; align-items: center; justify-content: center;
}
.hero-title { margin: 0; font-size: 20px; font-weight: 700; }
.hero-sub { margin: 4px 0 0; font-size: 13px; opacity: 0.85; }

.sg-card {
  background: #fff; border-radius: 16px; padding: 20px 22px;
  border: 1px solid #f1f5f9;
}
.sg-card-full { margin-bottom: 16px; }
.sg-head {
  display: flex; align-items: center; gap: 10px;
  font-size: 15px; font-weight: 600; color: #0f172a; margin-bottom: 16px;
}
.sg-num {
  width: 26px; height: 26px; border-radius: 8px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff; font-size: 13px; font-weight: 700;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.sg-head-tip { font-size: 12px; font-weight: 400; color: #94a3b8; }

.dev-tools {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  margin-bottom: 14px;
}
.dev-tools-count { font-size: 13px; color: #64748b; }
.dev-tools-count .hl { color: #2563eb; font-size: 16px; }

.dev-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px;
}
.dd-card {
  border: 1px solid #e2e8f0; border-radius: 14px; overflow: hidden; cursor: pointer;
  background: #fff; transition: all .18s;
}
.dd-card:hover { border-color: #2563eb; box-shadow: 0 6px 18px rgba(37, 99, 235, .12); transform: translateY(-2px); }
.dd-card.active { border-color: #2563eb; box-shadow: 0 0 0 2px rgba(37, 99, 235, .2); }
.dd-cover {
  position: relative; height: 120px;
  background: #f1f5f9 center/contain no-repeat;
  display: flex; align-items: center; justify-content: center; color: #b0b8c4;
}
.dd-status { position: absolute; top: 8px; left: 8px; }
.dd-checked {
  position: absolute; top: 8px; right: 8px; width: 26px; height: 26px; border-radius: 50%;
  background: #2563eb; color: #fff; display: flex; align-items: center; justify-content: center;
  box-shadow: 0 2px 8px rgba(37, 99, 235, .4);
}
.dd-body { padding: 12px 14px 14px; }
.dd-name { font-size: 15px; font-weight: 600; color: #0f172a; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.dd-code { font-size: 12px; color: #94a3b8; margin-top: 2px; }
.dd-meta { font-size: 12px; color: #64748b; margin-top: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.dd-desc {
  margin-top: 6px; font-size: 12px; color: #94a3b8; line-height: 1.4;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.dev-empty { grid-column: 1 / -1; }

.dev-pager {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 2px 0;
}
.dev-pager-info { font-size: 13px; color: #64748b; }
.dev-pager-info b { color: #2563eb; }

.wizard-steps { margin-bottom: 20px; }
.wizard-pane { min-height: 260px; }
.wizard-form { margin-top: 6px; }

.wizard-device {
  display: flex; align-items: center; gap: 12px; padding: 12px 14px;
  background: #f0f9ff; border-radius: 12px; margin-bottom: 16px;
}
.wd-cover {
  width: 54px; height: 54px; border-radius: 10px; flex-shrink: 0;
  background: #dbeafe center/contain no-repeat;
  display: flex; align-items: center; justify-content: center; color: #2563eb;
}
.wd-name { font-size: 15px; font-weight: 600; color: #0f172a; }
.wd-sub { font-size: 12px; color: #64748b; margin-top: 2px; }

.stu-cards { width: 100%; max-height: 320px; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; }
.stu-card {
  position: relative; display: flex; align-items: center; gap: 12px;
  padding: 12px 14px; border: 1px solid #e2e8f0; border-radius: 12px; cursor: pointer;
  transition: all .15s;
}
.stu-card:hover { border-color: #2563eb; background: #f8faff; }
.stu-card.active { border-color: #2563eb; background: #eff6ff; box-shadow: 0 0 0 2px rgba(37,99,235,.15); }
.stu-avatar {
  width: 40px; height: 40px; border-radius: 50%; flex-shrink: 0;
  background: linear-gradient(135deg, #2563eb, #1d4ed8); color: #fff;
  display: flex; align-items: center; justify-content: center; font-size: 16px; font-weight: 600;
}
.stu-avatar-wrap { flex-shrink: 0; }
.stu-avatar-wrap :deep(.el-avatar) { border-radius: 50%; border: 2px solid #fff; box-shadow: 0 2px 6px rgba(15,23,42,.15); }
.stu-main { display: flex; align-items: center; gap: 6px; }
.stu-name { font-size: 14px; font-weight: 600; color: #0f172a; }
.stu-role {
  font-size: 11px; color: #2563eb; background: #eff6ff;
  padding: 1px 6px; border-radius: 20px;
}
.stu-sub { font-size: 12px; color: #94a3b8; margin-top: 2px; display: flex; align-items: center; gap: 4px; }
.stu-id-tag {
  font-size: 11px; color: #64748b; background: #f1f5f9;
  padding: 1px 5px; border-radius: 4px;
}
.stu-phone { color: #94a3b8; }
.stu-check { position: absolute; right: 14px; color: #2563eb; }
.stu-empty { width: 100%; }

.wizard-stu-summary {
  display: flex; align-items: center; gap: 12px; padding: 12px 14px;
  background: #f0f9ff; border-radius: 12px; margin-bottom: 12px;
}
.ws-avatar {
  width: 42px; height: 42px; border-radius: 50%; flex-shrink: 0;
  background: linear-gradient(135deg, #2563eb, #1d4ed8); color: #fff;
  display: flex; align-items: center; justify-content: center; font-size: 16px; font-weight: 600;
}
.ws-avatar-img { border-radius: 50%; border: 2px solid #fff; box-shadow: 0 2px 6px rgba(15,23,42,.15); }
.ws-name { font-size: 15px; font-weight: 600; color: #0f172a; }
.ws-sub { font-size: 12px; color: #64748b; margin-top: 2px; }

.hint-row { margin: -4px 0 16px 92px; font-size: 13px; color: #64748b; }
.hint-row .ok { color: #2563eb; font-size: 15px; }
.hint-row .warn { color: #ef4444; font-size: 15px; }
.warn-txt { color: #ef4444; margin-left: 8px; }

@media (max-width: 1200px) {
  .dev-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 900px) {
  .dev-grid { grid-template-columns: repeat(2, 1fr); }
  .hint-row { margin-left: 0; }
}
</style>