<template>
  <div class="page-container">
    <div class="page-hero">
      <div class="hero-left">
        <div class="hero-icon">
          <el-icon :size="26"><Tickets /></el-icon>
        </div>
        <div>
          <h2 class="hero-title">📦 批量申领设备</h2>
          <p class="hero-sub">实验课一次性选择多台设备批量提交申请 · 最长可借30天</p>
        </div>
      </div>
      <div class="hero-right">
        <span class="hr-count">已选 <b>{{ selectedIds.length }}</b> 台</span>
        <el-button :icon="ArrowRight" type="primary" size="large" :disabled="selectedIds.length === 0" @click="showSubmitDialog = true">
          下一步
        </el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input v-model="keyword" placeholder="搜索设备名称 / 编号" clearable style="width: 260px"
        :prefix-icon="Search" @clear="loadDevices" @keyup.enter="loadDevices" />
      <el-select v-model="labFilter" placeholder="全部实验室" clearable style="width: 160px" @change="loadDevices">
        <el-option v-for="l in labList" :key="l.id" :label="l.name" :value="l.id" />
      </el-select>
      <el-button :icon="Refresh" @click="resetFilter">重置</el-button>
      <div class="fb-right">
        <span class="fb-hint">仅显示状态为「空闲」的设备</span>
        <el-button size="small" @click="toggleSelectAll">{{ isAllSelected ? '取消全选' : '全选空闲设备' }}</el-button>
      </div>
    </div>

    <div v-loading="loading" class="device-grid">
      <div v-for="d in deviceList" :key="d.id" class="device-card" :class="{ selected: selectedIds.includes(d.id) }"
        @click="toggleSelect(d)">
        <div class="dc-img" :style="d.imageUrl ? { backgroundImage: `url(${getImageUrl(d.imageUrl)})` } : {}">
          <el-icon v-if="!d.imageUrl" :size="36"><Monitor /></el-icon>
          <span class="dc-status">空闲可借</span>
        </div>
        <div class="dc-body">
          <div class="dc-name">{{ d.name }}</div>
          <div class="dc-code">{{ d.code }}</div>
          <div class="dc-meta">
            <span><el-icon><OfficeBuilding /></el-icon>{{ d.labName }}</span>
            <span><el-icon><User /></el-icon>{{ d.brand || '未标注' }}</span>
          </div>
        </div>
        <div class="dc-check">
          <el-icon :size="20" v-if="selectedIds.includes(d.id)"><CircleCheckFilled /></el-icon>
        </div>
      </div>
      <el-empty v-if="!loading && deviceList.length === 0" description="暂无可借设备" />
    </div>

    <el-dialog v-model="showSubmitDialog" width="560px" :close-on-click-modal="false" class="batch-dialog">
      <template #header>
        <div class="batch-dlg-header">
          <div class="batch-dlg-icon"><el-icon :size="22"><Tickets /></el-icon></div>
          <div>
            <div class="batch-dlg-title">批量申领 · 共 {{ selectedIds.length }} 台设备</div>
            <div class="batch-dlg-sub">填写一次信息即可为所有设备统一提交申请</div>
          </div>
        </div>
      </template>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
        <el-form-item label="借用时间" prop="borrowTime">
          <el-date-picker v-model="form.borrowTime" type="datetime" placeholder="选择开始时间"
            value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" :disabled-date="disablePast" />
        </el-form-item>
        <el-form-item label="预计归还" prop="expectReturnTime">
          <el-date-picker v-model="form.expectReturnTime" type="datetime" placeholder="选择归还时间（最长30天）"
            value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" :disabled-date="disablePast" />
          <div class="form-hint">最长30天 · 已选 <b>{{ borrowDays }}</b> 天</div>
        </el-form-item>
        <el-form-item label="借用目的" prop="purpose">
          <el-input v-model="form.purpose" type="textarea" :rows="3" placeholder="请填写实验课名称、用途等信息" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSubmitDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitBatch">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Tickets, ArrowRight, Search, Refresh, Monitor, OfficeBuilding, User, CircleCheckFilled,
} from '@element-plus/icons-vue'
import { getAllDevices } from '@/api/device'
import { getAllLabs } from '@/api/lab'
import { batchApplyBorrow } from '@/api/borrow'
import { useUserStore } from '@/store/user'
import { getImageUrl } from '@/utils/image'

const userStore = useUserStore()

const loading = ref(false)
const keyword = ref('')
const labFilter = ref(null)
const deviceList = ref([])
const labList = ref([])
const selectedIds = ref([])
const showSubmitDialog = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({
  borrowTime: '',
  expectReturnTime: '',
  purpose: '',
})

const rules = {
  borrowTime: [{ required: true, message: '请选择借用时间', trigger: 'change' }],
  expectReturnTime: [{ required: true, message: '请选择归还时间', trigger: 'change' }],
  purpose: [{ required: true, message: '请填写借用目的', trigger: 'blur' }],
}

const disablePast = (date) => {
  const now = new Date()
  return date.getTime() < now.setHours(0, 0, 0, 0)
}

const borrowDays = computed(() => {
  if (!form.borrowTime || !form.expectReturnTime) return 0
  const d1 = new Date(form.borrowTime).getTime()
  const d2 = new Date(form.expectReturnTime).getTime()
  return Math.ceil((d2 - d1) / (1000 * 60 * 60 * 24))
})

const isAllSelected = computed(() =>
  deviceList.value.length > 0 && deviceList.value.every(d => selectedIds.value.includes(d.id))
)

const loadDevices = async () => {
  loading.value = true
  try {
    const params = { status: 1, keyword: keyword.value || undefined, labId: labFilter.value || undefined }
    const res = await getAllDevices(params)
    deviceList.value = res.data?.records || res.data || []
  } catch (e) {
    ElMessage.error('加载设备失败')
  } finally {
    loading.value = false
  }
}

const loadLabs = async () => {
  try {
    const res = await getAllLabs()
    labList.value = res.data || []
  } catch (e) {}
}

const resetFilter = () => {
  keyword.value = ''
  labFilter.value = null
  loadDevices()
}

const toggleSelect = (d) => {
  const idx = selectedIds.value.indexOf(d.id)
  if (idx > -1) selectedIds.value.splice(idx, 1)
  else selectedIds.value.push(d.id)
}

const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedIds.value = []
  } else {
    selectedIds.value = deviceList.value.map(d => d.id)
  }
}

const submitBatch = async () => {
  if (borrowDays.value > 30) {
    ElMessage.warning('单次借用最长为30天')
    return
  }
  await formRef.value.validate()

  submitting.value = true
  try {
    const list = selectedIds.value.map(id => ({
      deviceId: id,
      borrowTime: form.borrowTime,
      expectReturnTime: form.expectReturnTime,
      purpose: form.purpose,
    }))
    await batchApplyBorrow(list)
    ElMessage.success(`成功提交 ${list.length} 台设备申请`)
    showSubmitDialog.value = false
    selectedIds.value = []
    form.borrowTime = ''
    form.expectReturnTime = ''
    form.purpose = ''
    loadDevices()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadLabs()
  loadDevices()
})
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
.hero-right { display: flex; align-items: center; gap: 16px; }
.hr-count { font-size: 14px; opacity: 0.9; }
.hr-count b { font-size: 22px; font-weight: 700; margin: 0 2px; }

.filter-bar {
  background: #fff; border-radius: 14px; padding: 16px 20px;
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 16px; flex-wrap: wrap;
  border: 1px solid #f1f5f9;
}
.fb-right { margin-left: auto; display: flex; align-items: center; gap: 12px; }
.fb-hint { font-size: 12px; color: #94a3b8; }

.device-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}
.device-card {
  background: #fff; border-radius: 14px; overflow: hidden;
  cursor: pointer; transition: all 0.25s ease;
  border: 2px solid transparent; position: relative;
}
.device-card:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(15,23,42,0.08); }
.device-card.selected { border-color: #2563eb; box-shadow: 0 8px 24px rgba(37,99,235,0.2); }
.dc-img {
  height: 120px; background: linear-gradient(135deg, #dbeafe, #bfdbfe);
  display: flex; align-items: center; justify-content: center; color: #2563eb;
  background-size: cover; background-position: center; position: relative;
}
.dc-status {
  position: absolute; top: 10px; left: 10px;
  background: rgba(34,197,94,0.9); color: #fff;
  font-size: 11px; padding: 3px 10px; border-radius: 20px; font-weight: 500;
}
.dc-body { padding: 14px 16px; }
.dc-name { font-size: 15px; font-weight: 600; color: #0f172a; }
.dc-code { font-size: 12px; color: #94a3b8; margin-top: 2px; font-family: Consolas, monospace; }
.dc-meta { display: flex; gap: 14px; margin-top: 10px; font-size: 12px; color: #64748b; }
.dc-meta .el-icon { margin-right: 3px; font-size: 13px; vertical-align: -2px; }
.dc-check {
  position: absolute; top: 10px; right: 10px;
  color: #2563eb; opacity: 0; transform: scale(0.5);
  transition: all 0.25s ease;
}
.device-card.selected .dc-check { opacity: 1; transform: scale(1); }

.batch-dlg-header { display: flex; align-items: center; gap: 14px; }
.batch-dlg-icon {
  width: 44px; height: 44px; border-radius: 12px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff; display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.batch-dlg-title { font-size: 16px; font-weight: 600; color: #0f172a; }
.batch-dlg-sub { font-size: 12px; color: #94a3b8; margin-top: 2px; }
.form-hint { font-size: 12px; color: #94a3b8; margin-top: 4px; }
.form-hint b { color: #2563eb; }
</style>