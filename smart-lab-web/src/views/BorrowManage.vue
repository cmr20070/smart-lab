        <template>
  <div class="page-container">
    <div class="page-hero">
      <div class="hero-left">
        <div class="hero-icon">
          <el-icon :size="26"><Tickets /></el-icon>
        </div>
        <div>
          <h2 class="hero-title">借用预约管理</h2>
          <p class="hero-sub">{{ heroSubtitle }}</p>
        </div>
      </div>
      <div class="hero-stats">
        <div class="stat-chip clickable" @click="quickFilter = 0; searchForm.status = 0; fetchData()">
          <span class="chip-num pending">{{ stats.pending }}</span>
          <span class="chip-label">待审批</span>
        </div>
        <div class="stat-chip clickable" @click="quickFilter = 1; searchForm.status = 1; fetchData()">
          <span class="chip-num approved">{{ stats.approved }}</span>
          <span class="chip-label">借用中</span>
        </div>
        <div class="stat-chip clickable" @click="quickFilter = 5; searchForm.status = 5; fetchData()">
          <span class="chip-num overdue">{{ stats.overdue }}</span>
          <span class="chip-label">逾期</span>
        </div>
        <div class="stat-chip clickable" @click="quickFilter = 3; searchForm.status = 3; fetchData()">
          <span class="chip-num returned">{{ stats.returned }}</span>
          <span class="chip-label">已归还</span>
        </div>
      </div>
    </div>

    <div class="toolbar-card">
      <div class="toolbar-left">
        <el-radio-group v-model="searchForm.status" @change="fetchData" size="default">
          <el-radio-button :value="''">全部</el-radio-button>
          <el-radio-button :value="0">待审批</el-radio-button>
          <el-radio-button :value="1">借用中</el-radio-button>
          <el-radio-button :value="2">已拒绝</el-radio-button>
          <el-radio-button :value="3">已归还</el-radio-button>
          <el-radio-button :value="5">逾期</el-radio-button>
        </el-radio-group>
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索设备名称 / 编号"
          clearable style="width: 260px"
          :prefix-icon="Search"
          @keyup.enter="fetchData"
          @clear="fetchData"
        />
      </div>
      <div class="toolbar-right">
        <el-button v-if="canExport" plain :icon="Download" @click="handleExport">导出</el-button>
        <el-button v-if="canDelete" plain type="danger" :icon="Delete" @click="handleBatchDelete" :disabled="!selectedBorrows.length">
          批量删除 <span v-if="selectedBorrows.length">({{ selectedBorrows.length }})</span>
        </el-button>
        <el-button type="primary" :icon="Plus" @click="handleApply">申请借用</el-button>
      </div>
    </div>

    <el-card class="table-card" shadow="never">
      <el-table :data="tableData" v-loading="loading" style="width: 100%"
        @selection-change="handleBorrowSelectionChange" ref="borrowTableRef"
        :header-cell-style="{ background: '#fafbfc', color: '#303133', fontWeight: 600 }"
        highlight-current-row>
        <el-table-column v-if="canDelete" type="selection" width="48" />
        <el-table-column label="序号" width="60" align="center">
          <template #default="{ $index }">
            <span class="seq">{{ (page - 1) * size + $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="deviceName" label="设备" min-width="180">
          <template #default="{ row }">
            <div class="device-cell">
              <div class="device-avatar">{{ row.deviceName?.charAt(0) || '?' }}</div>
              <div class="device-info">
                <div class="device-name">{{ row.deviceName }}</div>
                <div class="device-code">{{ row.deviceCode }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column v-if="showUserColumn" prop="username" label="借用人" width="110">
          <template #default="{ row }">
            <div class="user-cell">
              <el-icon><UserFilled /></el-icon>
              <span>{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="借用时间" width="240">
          <template #default="{ row }">
            <div class="time-cell">
              <div><el-icon><Clock /></el-icon> 借出：{{ row.borrowTime }}</div>
              <div :class="{ 'overdue-text': isOverdue(row) }">
                <el-icon><Timer /></el-icon> 归还：{{ row.expectReturnTime }}
                <el-tag v-if="isOverdue(row)" size="small" type="danger" effect="dark" round style="margin-left:4px">已逾期</el-tag>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="purpose" label="借用目的" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="purpose-text">{{ row.purpose || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="canApprove" label="审批信息" width="180">
          <template #default="{ row }">
            <template v-if="row.approverName">
              <div class="approve-cell">
                <div><el-icon><User /></el-icon> {{ row.approverName }}</div>
                <div class="sub-text">{{ row.approveTime }}</div>
              </div>
            </template>
            <span v-else class="sub-text">—</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="dark" round class="status-tag">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right" align="center">
          <template #default="{ row }">
            <template v-if="row.status === 0 && canApprove">
              <el-button size="small" type="success" :icon="Check" circle @click="openApproveDialog(row, 1)" title="通过" />
              <el-button size="small" type="danger" :icon="Close" circle @click="openApproveDialog(row, 2)" title="拒绝" />
            </template>
            <el-tooltip v-if="(row.status === 1 || row.status === 5) && (canApprove || isOwner(row))" content="归还" placement="top">
              <el-button size="small" type="warning" :icon="RefreshRight" circle @click="handleReturn(row)" />
            </el-tooltip>
            <el-tooltip v-if="isOverdue(row) && (isAdmin || isLabAdmin)" content="催促归还" placement="top">
              <el-button size="small" type="danger" :icon="Bell" circle @click="handleRemind(row)" />
            </el-tooltip>
            <el-tooltip v-if="row.status === 0 && isOwner(row)" content="取消申请" placement="top">
              <el-button size="small" :icon="CircleClose" circle @click="handleCancel(row)" />
            </el-tooltip>
            <el-button v-if="canDelete" size="small" type="danger" plain text :icon="Delete" @click="handleDelete(row)">删除</el-button>
            <el-button size="small" text :icon="View" @click="openDetailDialog(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager-bar">
        <span class="pager-info">共 <b>{{ total }}</b> 条记录</span>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
          background
        />
      </div>
    </el-card>

    <!-- 申请借用弹窗 -->
    <el-dialog v-model="dialogVisible" width="640px" :close-on-click-modal="false"
      class="apply-dialog" @close="resetForm" destroy-on-close>
      <template #header>
        <div class="dialog-header">
          <div class="dialog-title-wrap">
            <div class="dialog-icon">
              <el-icon><EditPen /></el-icon>
            </div>
            <div>
              <h3 class="dialog-title">设备借用申请</h3>
              <p class="dialog-sub">请选择设备并填写借用信息</p>
            </div>
          </div>
        </div>
      </template>

      <div class="apply-body">
        <div class="form-section device-section">
          <div class="section-label">
            <el-icon><Monitor /></el-icon>
            <span>选择设备</span>
          </div>
          <el-select v-model="form.deviceId" placeholder="点击选择可借用设备" filterable
            style="width: 100%; height: 44px; border-radius: 10px" size="large">
            <el-option v-for="d in availableDevices" :key="d.id"
              :label="d.name + '（' + d.code + '）'" :value="d.id">
              <div class="device-option">
                <div class="opt-name">{{ d.name }}</div>
                <div class="opt-sub">编号: {{ d.code }} | {{ d.labName || '未分配实验室' }}</div>
              </div>
            </el-option>
          </el-select>

          <div v-if="selectedDevice" class="device-preview">
            <div class="preview-info">
              <div class="preview-name">{{ selectedDevice.name }}</div>
              <div class="preview-grid">
                <div><span class="pl">编号</span>{{ selectedDevice.code }}</div>
                <div><span class="pl">型号</span>{{ selectedDevice.model || '-' }}</div>
                <div><span class="pl">分类</span>{{ selectedDevice.category || '-' }}</div>
                <div><span class="pl">实验室</span>{{ selectedDevice.labName || '-' }}</div>
              </div>
            </div>
            <el-tag type="success" effect="dark" round>可借用</el-tag>
          </div>
        </div>

        <div class="form-section time-section">
          <div class="section-label">
            <el-icon><Timer /></el-icon>
            <span>借用时间</span>
          </div>
          <div class="time-grid">
            <div class="time-item">
              <div class="time-label">借用开始时间 <span class="optional">（不选默认立即）</span></div>
              <el-date-picker
                v-model="form.borrowTime" type="datetime" :disabled-date="disabledPastDate" placeholder="选择开始时间"
                value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm"
                style="width: 100%"
              />
            </div>
            <div class="time-item">
              <div class="time-label">预计归还时间 <span class="required">*</span></div>
              <el-date-picker
                v-model="form.expectReturnTime" type="datetime" :disabled-date="disabledReturnDate" placeholder="选择归还时间"
                value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm"
                style="width: 100%"
              />
            </div>
          </div>
          <div v-if="durationText" class="duration-bar">
            <el-icon><Clock /></el-icon>
            本次借用时长：<b>{{ durationText }}</b>
            <span v-if="durationDays > 20" class="warn">（较长借用，请在目的中详细说明）</span>
          </div>
        </div>

        <div class="form-section detail-section">
          <div class="section-label">
            <el-icon><Document /></el-icon>
            <span>借用详情</span>
          </div>
          <el-form ref="formRef" :model="form" :rules="rules">
            <el-form-item prop="purpose">
              <el-input v-model="form.purpose" type="textarea" :rows="3"
                placeholder="请简要说明借用设备的用途" maxlength="200" show-word-limit />
            </el-form-item>
            <el-form-item>
              <el-input v-model="form.remark" type="textarea" :rows="2"
                placeholder="补充说明（选填）" maxlength="200" show-word-limit />
            </el-form-item>
          </el-form>
        </div>

        <div class="tips-box">
          <el-icon><InfoFilled /></el-icon>
          <div class="tips-text">
            <div>⚠️ 单次借用最长 <b>30 天</b>，每人同时在借不超过 <b>3 台</b></div>
            <div>📌 审批通过后请按时归还，逾期将影响后续借用</div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false" size="large">取消</el-button>
          <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
            <el-icon><Promotion /></el-icon> 提交申请
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 审批弹窗 -->
    <el-dialog v-model="approveVisible" width="480px" :close-on-click-modal="false" destroy-on-close>
      <template #header>
        <div class="dialog-header">
          <div class="dialog-title-wrap">
            <div class="dialog-icon" :class="approveStatus === 1 ? 'icon-ok' : 'icon-no'">
              <el-icon><component :is="approveStatus === 1 ? Check : Close" /></el-icon>
            </div>
            <div>
              <h3 class="dialog-title">{{ approveStatus === 1 ? '审批通过' : '审批拒绝' }}</h3>
              <p class="dialog-sub">{{ approveTarget?.deviceName }} · {{ approveTarget?.username }}</p>
            </div>
          </div>
        </div>
      </template>
      <div style="padding: 20px 24px">
        <div class="approve-summary">
          <div class="summary-row"><span>设备</span><b>{{ approveTarget?.deviceName }}</b></div>
          <div class="summary-row"><span>借用人</span><b>{{ approveTarget?.username }}</b></div>
          <div class="summary-row"><span>借用时间</span>{{ approveTarget?.borrowTime }}</div>
          <div class="summary-row"><span>归还时间</span>{{ approveTarget?.expectReturnTime }}</div>
          <div class="summary-row"><span>借用目的</span>{{ approveTarget?.purpose }}</div>
        </div>
        <el-divider />
        <el-form label-width="90px">
          <el-form-item label="审批备注">
            <el-input v-model="approveRemark" type="textarea" :rows="3"
              :placeholder="approveStatus === 1 ? '可填写通过说明（选填）' : '请填写拒绝原因（选填）'" maxlength="200" show-word-limit />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="approveVisible = false">取消</el-button>
          <el-button :type="approveStatus === 1 ? 'success' : 'danger'" :loading="approving" @click="handleApproveConfirm">
            确认{{ approveStatus === 1 ? '通过' : '拒绝' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" width="520px" destroy-on-close>
      <template #header>
        <div class="dialog-header">
          <div class="dialog-title-wrap">
            <div class="dialog-icon"><el-icon><View /></el-icon></div>
            <div>
              <h3 class="dialog-title">借用详情</h3>
              <p class="dialog-sub">完整的借用申请与审批信息</p>
            </div>
          </div>
        </div>
      </template>
      <div v-if="detailTarget" style="padding: 20px 24px">
        <div class="detail-grid">
          <div class="detail-item"><span>设备名称</span><b>{{ detailTarget.deviceName }}</b></div>
          <div class="detail-item"><span>设备编号</span><b class="mono">{{ detailTarget.deviceCode }}</b></div>
          <div class="detail-item"><span>借用人</span><b>{{ detailTarget.username }}</b></div>
          <div class="detail-item"><span>状态</span>
            <el-tag :type="statusTagType(detailTarget.status)" effect="dark" round>{{ statusLabel(detailTarget.status) }}</el-tag>
          </div>
          <div class="detail-item full"><span>借用目的</span><div>{{ detailTarget.purpose || '-' }}</div></div>
          <div class="detail-item"><span>借出时间</span>{{ detailTarget.borrowTime }}</div>
          <div class="detail-item"><span>预计归还</span>{{ detailTarget.expectReturnTime }}</div>
          <div class="detail-item"><span>实际归还</span>{{ detailTarget.actualReturnTime || '—' }}</div>
          <div class="detail-item full"><span>备注</span><div>{{ detailTarget.remark || '-' }}</div></div>
        </div>
        <div v-if="detailTarget.approverName" class="approve-detail">
          <div class="approve-detail-title">审批信息</div>
          <div class="detail-grid">
            <div class="detail-item"><span>审批人</span><b>{{ detailTarget.approverName }}</b></div>
            <div class="detail-item"><span>审批时间</span>{{ detailTarget.approveTime }}</div>
            <div class="detail-item full"><span>审批备注</span><div>{{ detailTarget.approveRemark || '-' }}</div></div>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- Teacher: Apply for Student Dialog -->
    <el-dialog v-model="teacherStudentDialogVisible" title="代学生申请设备" width="580px" class="modern-dialog">
      <div class="dialog-hero">
        <div class="dialog-hero-icon" style="background: linear-gradient(135deg, #0ea5e9, #2563eb);">
          <el-icon :size="28"><User /></el-icon>
        </div>
        <div>
          <h4>代学生申请设备</h4>
          <p>选择学生后代为提交借用申请</p>
        </div>
      </div>
      <el-form ref="teacherFormRef" :model="teacherForm" :rules="teacherRules" label-width="110px">
        <el-form-item label="选择学生" prop="studentId">
          <el-select v-model="teacherForm.studentId" placeholder="搜索学生姓名/账号" filterable style="width: 100%">
            <el-option v-for="s in studentList" :key="s.id" :label="s.username + ' (' + s.phone + ')'" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择设备" prop="deviceId">
          <el-select v-model="teacherForm.deviceId" placeholder="选择设备" filterable style="width: 100%">
            <el-option v-for="d in availableDevices" :key="d.id" :label="d.name + ' (' + d.code + ')'" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="借用开始时间">
          <el-date-picker v-model="teacherForm.borrowTime" type="datetime" placeholder="不选默认立即" :disabled-date="disablePast" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预计归还" required>
          <el-date-picker v-model="teacherForm.expectReturnTime" type="datetime" placeholder="选择归还时间" :disabled-date="disablePast" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="借用用途" required>
          <el-input v-model="teacherForm.purpose" type="textarea" :rows="2" placeholder="实验课使用、课程项目等..." maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="备注说明">
          <el-input v-model="teacherForm.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="teacherStudentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="teacherSubmitting" @click="handleTeacherApply">提交申请</el-button>
      </template>
    </el-dialog>
    <!-- Teacher Batch Apply Dialog -->
    <el-dialog v-if="isTeacher" v-model="batchDialogVisible" title="批量申领设备" width="720px" class="modern-dialog">
      <div class="dialog-hero">
        <div class="dialog-hero-icon" style="background: linear-gradient(135deg, #1d4ed8, #2563eb);">
          <el-icon :size="28"><Box /></el-icon>
        </div>
        <div>
          <h4>批量申领设备（实验课）</h4>
          <p>添加多台设备一次性提交申请</p>
        </div>
      </div>
      <div class="batch-list">
        <div v-for="(item, idx) in batchList" :key="idx" class="batch-item">
          <el-select v-model="item.deviceId" placeholder="选择设备" filterable style="flex:1" @change="onBatchDeviceChange(idx)">
            <el-option v-for="d in availableDevices.filter(x => !batchList.some((b,i) => i !== idx && b.deviceId === x.id))" :key="d.id" :label="d.name + ' (' + d.code + ')'" :value="d.id" />
          </el-select>
          <el-date-picker v-model="item.expectReturnTime" type="datetime" placeholder="预计归还" :disabled-date="disablePast" value-format="YYYY-MM-DD HH:mm:ss" style="width:200px" />
          <el-input v-model="item.purpose" placeholder="用途" style="flex:1" maxlength="100" />
          <el-button :icon="Delete" circle type="danger" plain @click="batchList.splice(idx, 1)" />
        </div>
        <el-button type="primary" plain :icon="Plus" @click="batchList.push({ deviceId: null, expectReturnTime: '', purpose: '' })">添加设备</el-button>
      </div>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchSubmitting" @click="handleBatchApply">提交批量申领</el-button>
      </template>
    </el-dialog>


  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '@/store/user'
import { getBorrowList, applyBorrow, approveBorrow, returnDevice, cancelBorrow, deleteBorrow, batchApplyBorrow, remindBorrow } from '@/api/borrow'
import { getAllDevices } from '@/api/device'
import { downloadFile } from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Plus, Download, Check, Close, RefreshRight, CircleClose, Delete, View,
  UserFilled, User, Clock, Timer, Monitor, Document, InfoFilled, EditPen, Promotion, Bell
} from '@element-plus/icons-vue'

const userStore = useUserStore()
const role = computed(() => userStore.userInfo?.role)
const userId = computed(() => userStore.userInfo?.id)

const isAdmin = computed(() => role.value === 'ADMIN')
const isLabAdmin = computed(() => role.value === 'LAB_ADMIN')
const isTeacher = computed(() => role.value === 'TEACHER')
const canApprove = computed(() => isAdmin.value || isLabAdmin.value)
const canDelete = computed(() => isAdmin.value)
const canExport = computed(() => !role.value || role.value !== 'STUDENT')
const showUserColumn = computed(() => canApprove.value)

const heroSubtitle = computed(() => {
  if (isAdmin.value) return '管理所有设备借用申请、审批与记录'
  if (isLabAdmin.value) return '审批本实验室设备借用申请，协助归还'
  return '申请设备借用，查看借用进度，按时归还'
})

const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const availableDevices = ref([])
const quickFilter = ref('')

const stats = reactive({ pending: 0, approved: 0, overdue: 0, returned: 0 })
const searchForm = reactive({ status: '', keyword: '' })

const selectedBorrows = ref([])
const borrowTableRef = ref(null)

const handleBorrowSelectionChange = (rows) => { selectedBorrows.value = rows }

const isOverdue = (row) => {
  if (row.status === 3) return false
  if (!row.expectReturnTime) return false
  return new Date(row.expectReturnTime.replace(/-/g, '/')) < new Date()
}

const isOwner = (row) => row.userId === userId.value

const statusTagType = (status) => {
  const map = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'info', 4: 'info', 5: 'danger' }
  return map[status] || 'info'
}
const statusLabel = (status) => {
  const map = { 0: '待审批', 1: '借用中', 2: '已拒绝', 3: '已归还', 4: '已取消', 5: '逾期' }
  return map[status] || '未知'
}

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = reactive({ deviceId: null, borrowTime: '', expectReturnTime: '', purpose: '', remark: '' })

const selectedDevice = computed(() => availableDevices.value.find(d => d.id === form.deviceId))

const durationText = computed(() => {
  if (!form.expectReturnTime) return ''
  const start = form.borrowTime || new Date().toISOString().slice(0, 19).replace('T', ' ')
  const end = form.expectReturnTime
  const s = new Date(start.replace(/-/g, '/'))
  const e = new Date(end.replace(/-/g, '/'))
  if (isNaN(s) || isNaN(e) || e <= s) return ''
  const diffMs = e - s
  const days = Math.floor(diffMs / 86400000)
  const hours = Math.floor((diffMs % 86400000) / 3600000)
  if (days > 0) return hours > 0 ? `${days} 天 ${hours} 小时` : `${days} 天`
  return `${hours} 小时`
})
const durationDays = computed(() => {
  if (!form.expectReturnTime) return 0
  const start = form.borrowTime ? new Date(form.borrowTime.replace(/-/g, '/')) : new Date()
  const end = new Date(form.expectReturnTime.replace(/-/g, '/'))
  if (isNaN(end) || end <= start) return 0
  return Math.ceil((end - start) / 86400000)
})

const rules = {
  deviceId: [{ required: true, message: '请选择设备', trigger: 'change' }],
  expectReturnTime: [{ required: true, message: '请选择预计归还时间', trigger: 'change' }],
  purpose: [{ required: true, message: '请填写借用目的', trigger: 'blur' }, { min: 3, message: '请至少填写3个字', trigger: 'blur' }],
}

const nowDate = () => { const d = new Date(); d.setSeconds(0, 0); return d }
const disabledPastDate = (date) => {
  const today0 = new Date(nowDate().getFullYear(), nowDate().getMonth(), nowDate().getDate())
  const d0 = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  return d0.getTime() < today0.getTime()
}
const disabledReturnDate = (date) => {
  if (!form.borrowTime) return false
  const start = new Date(form.borrowTime.replace(/-/g, '/'))
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const max = new Date(start.getTime() + 30 * 86400000)
  return d.getTime() < start.getTime() || d.getTime() > max.getTime()
}

// ===== 审批弹窗 =====
const approveVisible = ref(false)
const approving = ref(false)
const approveStatus = ref(1)
const approveTarget = ref(null)
const approveRemark = ref('')

const openApproveDialog = (row, status) => {
  approveTarget.value = row
  approveStatus.value = status
  approveRemark.value = ''
  approveVisible.value = true
}
const handleApproveConfirm = async () => {
  approving.value = true
  try {
    await approveBorrow({ borrowId: approveTarget.value.id, status: approveStatus.value, approveRemark: approveRemark.value })
    ElMessage.success(approveStatus.value === 1 ? '已通过 ✓' : '已拒绝')
    approveVisible.value = false
    fetchData(); fetchStats()
  } catch (e) { console.error(e) }
  finally { approving.value = false }
}

// ===== 详情弹窗 =====
const detailVisible = ref(false)
const detailTarget = ref(null)
const openDetailDialog = (row) => { detailTarget.value = row; detailVisible.value = true }

// ===== 数据获取 =====
const fetchStats = async () => {
  try {
    const res = await getBorrowList({ page: 1, size: 2000 })
    const list = res.data?.records || []
    stats.pending = list.filter(r => r.status === 0).length
    stats.approved = list.filter(r => r.status === 1).length
    stats.overdue = list.filter(r => r.status === 5 || (r.status === 1 && isOverdue(r))).length
    stats.returned = list.filter(r => r.status === 3).length
  } catch { /* ignore */ }
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (searchForm.status !== '') params.status = searchForm.status
    if (searchForm.keyword) params.keyword = searchForm.keyword
    const res = await getBorrowList(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

// ===== 操作 =====
const handleExport = () => {
  const params = new URLSearchParams()
  if (searchForm.status != null && searchForm.status !== '') params.append('status', searchForm.status)
  if (searchForm.keyword) params.append('keyword', searchForm.keyword)
  downloadFile('/export/borrow?' + params.toString(), '借用记录')
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`⚠️ 仅管理员可删除。确认删除选中的 ${selectedBorrows.value.length} 条记录？此操作不可恢复。`, '批量删除', { type: 'warning' })
    for (const row of selectedBorrows.value) await deleteBorrow(row.id)
    ElMessage.success('批量删除成功')
    selectedBorrows.value = []
    fetchData()
  } catch { /* cancelled */ }
}

const handleApply = async () => {
  try {
    const res = await getAllDevices()
    availableDevices.value = (res.data || []).filter(d => d.status === 1)
  } catch (e) { console.error(e) }
  resetForm()
  dialogVisible.value = true
}

const resetForm = () => {
  if (formRef.value) formRef.value.resetFields()
  form.deviceId = null; form.borrowTime = ''; form.expectReturnTime = ''; form.purpose = ''; form.remark = ''
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (form.borrowTime && form.expectReturnTime && form.expectReturnTime <= form.borrowTime) {
    ElMessage.warning('归还时间必须晚于借用时间'); return
  }
  submitting.value = true
  try {
    const payload = { ...form }
    if (!payload.borrowTime) delete payload.borrowTime
    await applyBorrow(payload)
    ElMessage.success('申请提交成功，等待审批')
    dialogVisible.value = false
    fetchData(); fetchStats()
  } catch (e) { console.error(e) }
  finally { submitting.value = false }
}

const handleReturn = async (row) => {
  ElMessageBox.confirm('确认归还该设备？归还后设备将恢复空闲状态。', '归还确认', { type: 'info', confirmButtonText: '确认归还', cancelButtonText: '取消' })
    .then(async () => { await returnDevice(row.id); ElMessage.success('归还成功'); fetchData(); fetchStats() })
    .catch(() => {})
}
const handleCancel = async (row) => {
  ElMessageBox.confirm('确认取消该申请？取消后需重新提交。', '提示', { type: 'warning' })
    .then(async () => { await cancelBorrow(row.id); ElMessage.success('已取消'); fetchData(); fetchStats() })
    .catch(() => {})
}
const handleDelete = (row) => {
  ElMessageBox.confirm(`⚠️ 仅管理员可删除。确认删除 "${row.deviceName}" 的借用记录？此操作不可恢复。`, '提示', { type: 'warning' })
    .then(async () => { await deleteBorrow(row.id); ElMessage.success('删除成功'); fetchData(); fetchStats() })
    .catch(() => {})
}

const handleRemind = async (row) => {
  ElMessageBox.confirm(`确认向借用人发送逾期催还提醒？将同时提醒负责教师协助催还。`, '催促归还', { type: 'warning' })
    .then(async () => {
      await remindBorrow(row.id)
      ElMessage.success('已发送催还提醒')
    })
    .catch(() => {})
}

onMounted(() => { fetchData(); fetchStats() })
</script>

<style scoped>
.page-hero {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20px 24px;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  border-radius: 14px; color: #fff;
  box-shadow: 0 4px 14px rgba(102, 126, 234, 0.3);
}
.hero-left { display: flex; align-items: center; gap: 16px; }
.hero-icon {
  width: 52px; height: 52px;
  background: rgba(255,255,255,0.2);
  backdrop-filter: blur(10px);
  border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
}
.hero-title { margin: 0; font-size: 22px; font-weight: 700; letter-spacing: 0.5px; }
.hero-sub { margin: 4px 0 0; font-size: 13px; opacity: 0.85; }
.hero-stats { display: flex; gap: 10px; }
.stat-chip {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 8px 16px;
  background: rgba(255,255,255,0.18);
  backdrop-filter: blur(6px);
  border-radius: 10px; min-width: 78px;
  transition: all 0.2s;
}
.stat-chip.clickable { cursor: pointer; }
.stat-chip.clickable:hover { background: rgba(255,255,255,0.3); transform: translateY(-2px); }
.chip-num { font-size: 20px; font-weight: 700; line-height: 1.2; }
.chip-label { font-size: 12px; opacity: 0.9; margin-top: 2px; }
.chip-num.pending { color: #ffeaa7; }
.chip-num.approved { color: #55efc4; }
.chip-num.overdue { color: #ff7675; }
.chip-num.returned { color: #a29bfe; }

.toolbar-card {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 18px;
  background: #fff;
  border-radius: 12px; border: 1px solid #eef0f5;
}
.toolbar-left { display: flex; align-items: center; gap: 14px; }
.toolbar-right { display: flex; gap: 8px; }

.table-card { flex: 1; border-radius: 12px; border: 1px solid #eef0f5; }
.table-card :deep(.el-card__body) { padding: 0; }

.seq {
  display: inline-block; width: 22px; height: 22px; line-height: 22px;
  border-radius: 50%; background: #f0f4ff; color: #2563eb;
  font-size: 12px; font-weight: 600;
}

.device-cell { display: flex; align-items: center; gap: 10px; }
.device-avatar {
  width: 36px; height: 36px; border-radius: 10px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff; font-weight: 700; font-size: 14px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.device-info { min-width: 0; }
.device-name { font-weight: 500; color: #1f2937; }
.device-code { font-size: 12px; color: #9ca3af; font-family: Consolas, monospace; }

.user-cell { display: flex; align-items: center; gap: 6px; color: #4b5563; }
.user-cell .el-icon { color: #2563eb; }

.time-cell { font-size: 12px; color: #6b7280; line-height: 1.9; }
.time-cell .el-icon { margin-right: 4px; color: #9ca3af; }
.overdue-text { color: #1d4ed8; font-weight: 500; }
.purpose-text { color: #4b5563; }

.approve-cell { font-size: 12px; color: #4b5563; line-height: 1.6; }
.approve-cell .el-icon { margin-right: 3px; color: #2563eb; }
.sub-text { font-size: 12px; color: #9ca3af; }

.status-tag { font-size: 12px; padding: 0 10px; height: 22px; line-height: 20px; }

.pager-bar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 18px; border-top: 1px solid #eef0f5;
}
.pager-info { font-size: 13px; color: #6b7280; }
.pager-info b { color: #1f2937; font-weight: 600; }

/* Dialogs */
.apply-dialog :deep(.el-dialog),
.el-dialog { border-radius: 16px; overflow: hidden; }
.apply-dialog :deep(.el-dialog__header),
.el-dialog :deep(.el-dialog__header) {
  margin: 0; padding: 20px 24px;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  color: #fff;
}
.apply-dialog :deep(.el-dialog__body),
.el-dialog :deep(.el-dialog__body) { padding: 0; }
.apply-dialog :deep(.el-dialog__footer),
.el-dialog :deep(.el-dialog__footer) { padding: 16px 24px; border-top: 1px solid #f0f0f0; }

.dialog-header { display: flex; justify-content: space-between; align-items: center; }
.dialog-title-wrap { display: flex; align-items: center; gap: 14px; }
.dialog-icon {
  width: 42px; height: 42px;
  background: rgba(255,255,255,0.2);
  backdrop-filter: blur(6px);
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
}
.dialog-icon.icon-ok { background: rgba(37, 99, 235, 0.35); }
.dialog-icon.icon-no { background: rgba(29, 78, 216, 0.35); }
.dialog-title { margin: 0; font-size: 17px; font-weight: 700; }
.dialog-sub { margin: 3px 0 0; font-size: 12px; opacity: 0.9; }

.apply-body { padding: 20px 24px; display: flex; flex-direction: column; gap: 18px; }

.section-label {
  display: flex; align-items: center; gap: 6px;
  font-size: 14px; font-weight: 600; color: #1f2937; margin-bottom: 10px;
}
.section-label .el-icon { color: #2563eb; }

.device-preview {
  margin-top: 12px; padding: 14px 16px;
  background: linear-gradient(135deg, #f8faff 0%, #f0f4ff 100%);
  border: 1px solid #e0e7ff; border-radius: 10px;
  display: flex; justify-content: space-between; align-items: flex-start;
}
.preview-info { flex: 1; }
.preview-name { font-size: 15px; font-weight: 600; color: #1f2937; margin-bottom: 8px; }
.preview-grid {
  display: grid; grid-template-columns: repeat(2, 1fr); gap: 6px 16px;
  font-size: 12px; color: #6b7280;
}
.preview-grid .pl { display: inline-block; width: 42px; color: #9ca3af; }

.time-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.time-label { font-size: 12px; color: #6b7280; margin-bottom: 6px; }
.time-label .required { color: #1d4ed8; margin-left: 2px; }
.time-label .optional { color: #9ca3af; font-weight: normal; }

.duration-bar {
  margin-top: 10px; padding: 8px 14px;
  background: #ecfdf5; border: 1px solid #a7f3d0; color: #047857;
  border-radius: 8px; font-size: 13px;
  display: flex; align-items: center; gap: 6px;
}
.duration-bar b { font-weight: 700; margin: 0 2px; }
.duration-bar .warn { color: #b45309; margin-left: 6px; }

.tips-box {
  display: flex; gap: 10px; align-items: flex-start;
  padding: 12px 16px; background: #fffbeb; border: 1px solid #fde68a; border-radius: 10px;
  font-size: 12.5px; color: #92400e;
}
.tips-box .el-icon { margin-top: 1px; color: #0ea5e9; }
.tips-text > div { line-height: 1.8; }
.tips-text b { color: #b45309; }

.dialog-footer { display: flex; justify-content: flex-end; gap: 10px; }

.device-option { }
.opt-name { font-weight: 500; }
.opt-sub { font-size: 12px; color: #9ca3af; }

/* Approve dialog */
.approve-summary { background: #f9fafb; border-radius: 10px; padding: 14px 16px; }
.summary-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 4px 0; font-size: 13px;
}
.summary-row span { color: #6b7280; }
.summary-row b { color: #1f2937; font-weight: 600; }

/* Detail dialog */
.detail-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 12px 20px;
}
.detail-item { display: flex; flex-direction: column; gap: 4px; font-size: 13px; }
.detail-item.full { grid-column: 1 / -1; }
.detail-item span { color: #6b7280; font-size: 12px; }
.detail-item b { color: #1f2937; font-weight: 600; }
.detail-item b.mono { font-family: Consolas, monospace; color: #4b5563; }
.approve-detail {
  margin-top: 16px; padding-top: 16px; border-top: 1px dashed #e5e7eb;
}
.approve-detail-title { font-size: 13px; font-weight: 600; color: #1f2937; margin-bottom: 12px; }
</style>