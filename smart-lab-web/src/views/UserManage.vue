<template>
  <div class="page-container">
    <div class="hero-section">
      <div class="hero-content">
        <div class="hero-icon">
          <el-icon :size="32"><User /></el-icon>
        </div>
        <div class="hero-text">
          <h2>用户管理中心</h2>
          <p>管理系统内所有用户账户，分配角色权限</p>
        </div>
      </div>
      <el-button type="primary" @click="handleAdd" :icon="Plus">新增用户</el-button>
    </div>

    <div class="role-stats">
      <div class="role-stat admin">
        <div class="role-icon">
          <el-icon :size="20"><Lock /></el-icon>
        </div>
        <div class="role-detail">
          <span class="role-num">{{ roleCounts.ADMIN || 0 }}</span>
          <span class="role-name">管理员</span>
        </div>
      </div>
      <div class="role-stat lab-admin">
        <div class="role-icon">
          <el-icon :size="20"><Monitor /></el-icon>
        </div>
        <div class="role-detail">
          <span class="role-num">{{ roleCounts.LAB_ADMIN || 0 }}</span>
          <span class="role-name">实验室管理员</span>
        </div>
      </div>
      <div class="role-stat teacher">
        <div class="role-icon">
          <el-icon :size="20"><Reading /></el-icon>
        </div>
        <div class="role-detail">
          <span class="role-num">{{ roleCounts.TEACHER || 0 }}</span>
          <span class="role-name">教师</span>
        </div>
      </div>
      <div class="role-stat student">
        <div class="role-icon">
          <el-icon :size="20"><User /></el-icon>
        </div>
        <div class="role-detail">
          <span class="role-num">{{ roleCounts.STUDENT || 0 }}</span>
          <span class="role-name">学生</span>
        </div>
      </div>
    </div>

    <el-card class="search-card">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="">
          <el-input v-model="searchForm.keyword" placeholder="搜索用户名/姓名/手机号" clearable style="width: 280px">
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.role" placeholder="全部角色" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="管理员" value="ADMIN" />
            <el-option label="实验室管理员" value="LAB_ADMIN" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="学生" value="STUDENT" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData" :icon="Search">查询</el-button>
          <el-button @click="resetSearch" :icon="Refresh">重置</el-button>
        </el-form-item>
      </el-form>

      <div v-if="selectedUsers.length" class="batch-bar">
        <el-checkbox
          :model-value="isUserAllSelected"
          :indeterminate="isUserIndeterminate"
          @change="handleUserSelectAll"
        >
          全选
        </el-checkbox>
        <el-button size="small" @click="handleUserInvertSelect">反选</el-button>
        <span class="batch-count">已选 {{ selectedUsers.length }} 项</span>
        <el-button size="small" type="danger" @click="handleBatchDelete" plain :icon="Delete">批量删除</el-button>
        <el-button size="small" @click="selectedUsers = []">取消选择</el-button>
      </div>
    </el-card>

    <el-card class="table-card">
      <div class="table-toolbar">
        <div class="table-info">
          <span class="info-label">用户列表</span>
          <span class="info-count">共 {{ total }} 位用户</span>
        </div>
      </div>

      <el-table v-loading="loading" :data="tableData" @selection-change="handleUserSelectionChange" ref="userTableRef"
        :header-cell-style="{ background: '#f8f9fc', color: '#303133', fontWeight: '600', borderBottom: '1px solid #ebeef5' }"
        :cell-style="{ borderBottom: '1px solid #f0f0f0' }"
        :empty-text="''">
        <template #empty>
          <div class="empty-wrapper">
            <el-icon :size="64" color="#dcdfe6"><User /></el-icon>
            <p>暂无用户数据</p>
            <el-button type="primary" size="small" @click="handleAdd">添加第一位用户</el-button>
          </div>
        </template>
        <el-table-column type="selection" width="45" />
        <el-table-column label="序号" width="65" align="center">
          <template #default="{ $index }">
            {{ (page - 1) * size + $index + 1 }}
          </template>
        </el-table-column>
        <el-table-column label="用户信息" min-width="200">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="user-avatar" :class="'avatar-' + (row.role || 'STUDENT')">
                <el-avatar v-if="row.avatar" :src="row.avatar" :size="40" />
                <el-avatar v-else :size="40" :icon="UserFilled" />
              </div>
              <div class="user-detail">
                <span class="user-name">{{ row.username }}</span>
                <span class="user-role-tag" :class="'role-' + row.role">{{ roleLabel(row.role) }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="联系方式" width="150">
          <template #default="{ row }">
            <div class="contact-cell">
              <el-icon :size="14" color="#909399"><Phone /></el-icon>
              <span>{{ row.phone || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="contact-cell">
              <el-icon :size="14" color="#909399"><Message /></el-icon>
              <span>{{ row.email || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="账户状态" width="120" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              active-text="启用"
              inactive-text="禁用"
              inline-prompt
              @change="(val) => handleToggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleEdit(row)" :icon="Edit">编辑</el-button>
            <el-button size="small" type="danger" link @click="handleDelete(row)" :icon="Delete">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchData"
          @current-change="fetchData"
          background
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close class="modern-dialog">
      <div class="dialog-hero" :class="'hero-' + form.role">
        <div class="dialog-hero-icon">
          <el-icon :size="28"><component :is="roleIcon(form.role)" /></el-icon>
        </div>
        <div>
          <h4>{{ dialogTitle }}</h4>
          <p>{{ isEdit ? '修改用户信息和权限' : '填写基本信息，创建新用户' }}</p>
        </div>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="头像" style="text-align: center">
          <el-upload
            class="avatar-uploader"
            action=""
            :show-file-list="false"
            :before-upload="beforeAvatarUpload"
            :http-request="handleAvatarUpload"
          >
            <img v-if="dialogAvatarUrl" :src="dialogAvatarUrl" class="avatar-preview" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="avatar-tip">点击上传头像</div>
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :placeholder="isEdit ? '可修改用户名' : '请输入用户名'" />
        </el-form-item>
        <el-form-item label="密码" :prop="isEdit ? '' : 'password'">
          <el-input v-model="form.password" type="password" :placeholder="isEdit ? '留空不修改密码' : '至少6位'" show-password />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="选填" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="选填" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="form.role" class="role-radio-group">
            <el-radio value="ADMIN" class="role-radio role-ADMIN">管理员</el-radio>
            <el-radio value="LAB_ADMIN" class="role-radio role-LAB_ADMIN">实验室管理</el-radio>
            <el-radio value="TEACHER" class="role-radio role-TEACHER">教师</el-radio>
            <el-radio value="STUDENT" class="role-radio role-STUDENT">学生</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ isEdit ? '保存修改' : '创建用户' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { getUserList, addUser, updateUser, deleteUser, toggleUserStatus, uploadAvatar } from '@/api/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, UserFilled, Search, Refresh, Delete, Edit, User, Lock, Monitor, Reading, Phone, Message } from '@element-plus/icons-vue'

const roleIconMap = {
  ADMIN: Lock,
  LAB_ADMIN: Monitor,
  TEACHER: Reading,
  STUDENT: UserFilled
}

const roleIcon = (role) => roleIconMap[role] || UserFilled

const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const roleCounts = ref({})

const searchForm = reactive({ keyword: '', role: '' })

const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const isEdit = ref(false)
const editId = ref(null)
const submitting = ref(false)
const formRef = ref(null)
const dialogAvatarUrl = ref('')

const selectedUsers = ref([])
const userTableRef = ref(null)

const isUserAllSelected = computed(() =>
  tableData.value.length > 0 && selectedUsers.value.length === tableData.value.length
)
const isUserIndeterminate = computed(() =>
  selectedUsers.value.length > 0 && selectedUsers.value.length < tableData.value.length
)

const handleUserSelectionChange = (rows) => {
  selectedUsers.value = rows
}

const handleUserSelectAll = (checked) => {
  if (checked) {
    tableData.value.forEach(row => userTableRef.value?.toggleRowSelection(row, true))
  } else {
    userTableRef.value?.clearSelection()
  }
}

const handleUserInvertSelect = () => {
  tableData.value.forEach(row => {
    userTableRef.value?.toggleRowSelection(row, !selectedUsers.value.includes(row))
  })
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${selectedUsers.value.length} 个用户?`, '批量删除', { type: 'warning' })
    for (const row of selectedUsers.value) {
      await deleteUser(row.id)
    }
    ElMessage.success('批量删除成功')
    selectedUsers.value = []
    fetchData()
  } catch (e) { /* cancelled */ }
}

const form = reactive({
  username: '',
  password: '',
  phone: '',
  email: '',
  role: 'STUDENT',
  avatar: '',
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度需为3-20位', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_\u4e00-\u9fa5]+$/, message: '用户名只能包含字母、数字、下划线和中文', trigger: 'blur' },
  ],
  password: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [
    {
      pattern: /^$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/,
      message: '邮箱格式不正确',
      trigger: 'blur',
    },
  ],
}

const roleLabel = (role) => {
  const map = { ADMIN: '管理员', LAB_ADMIN: '实验室管理员', TEACHER: '教师', STUDENT: '学生' }
  return map[role] || '未知'
}

const fetchRoleCounts = async () => {
  const counts = {}
  for (const role of ['ADMIN', 'LAB_ADMIN', 'TEACHER', 'STUDENT']) {
    try {
      const res = await getUserList({ page: 1, size: 1, role })
      counts[role] = res.data.total || 0
    } catch { counts[role] = 0 }
  }
  roleCounts.value = counts
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getUserList({ page: page.value, size: size.value, ...searchForm })
    tableData.value = res.data.records
    total.value = res.data.total
    fetchRoleCounts()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.role = ''
  fetchData()
}

const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过2MB')
    return false
  }
  return true
}

const handleAvatarUpload = async (options) => {
  try {
    const res = await uploadAvatar(options.file)
    form.avatar = res.data.url
    dialogAvatarUrl.value = res.data.url
    ElMessage.success('头像上传成功')
  } catch (e) {
    ElMessage.error('头像上传失败')
  }
}

const handleAdd = () => {
  isEdit.value = false
  editId.value = null
  dialogTitle.value = '新增用户'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  editId.value = row.id
  dialogTitle.value = '编辑用户'
  Object.assign(form, {
    username: row.username,
    password: '',
    phone: row.phone || '',
    email: row.email || '',
    role: row.role,
    avatar: row.avatar || '',
  })
  dialogAvatarUrl.value = row.avatar || ''
  dialogVisible.value = true
}

const resetForm = () => {
  if (formRef.value) formRef.value.resetFields()
  form.username = ''
  form.password = ''
  form.phone = ''
  form.email = ''
  form.role = 'STUDENT'
  form.avatar = ''
  dialogAvatarUrl.value = ''
}

const handleSubmit = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch((errors) => {
    if (errors) {
      const fields = Object.keys(errors)
      if (fields.length) {
        ElMessage.warning('请完善必填信息：' + fields.join('、'))
      }
    }
    return false
  })
  if (!valid) return

  submitting.value = true
  try {
    const data = { ...form }
    if (isEdit.value && !data.password) delete data.password
    if (isEdit.value) {
      await updateUser(editId.value, data)
      ElMessage.success('更新成功')
    } else {
      await addUser(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleToggleStatus = async (row, val) => {
  try {
    await toggleUserStatus(row.id, val ? 1 : 0)
    ElMessage.success('状态更新成功')
    fetchData()
  } catch (e) {
    console.error(e)
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该用户吗？', '提示', { type: 'warning' })
    .then(async () => {
      await deleteUser(row.id)
      ElMessage.success('删除成功')
      fetchData()
    })
    .catch(() => {})
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
  background: linear-gradient(135deg, #1d4ed8 0%, #0ea5e9 100%);
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

.hero-section .el-button {
  border-radius: 10px;
  font-weight: 500;
  position: relative;
  z-index: 1;
}

.role-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.role-stat {
  background: #fff;
  border-radius: 14px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.role-stat:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.1);
}

.role-stat::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
}

.role-stat.admin::before { background: linear-gradient(180deg, #2563eb, #1e40af); }
.role-stat.lab-admin::before { background: linear-gradient(180deg, #3b82f6, #0ea5e9); }
.role-stat.teacher::before { background: linear-gradient(180deg, #60a5fa, #2563eb); }
.role-stat.student::before { background: linear-gradient(180deg, #2563eb, #1d4ed8); }

.role-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.role-stat.admin .role-icon { background: linear-gradient(135deg, #2563eb, #1e40af); }
.role-stat.lab-admin .role-icon { background: linear-gradient(135deg, #3b82f6, #0ea5e9); }
.role-stat.teacher .role-icon { background: linear-gradient(135deg, #60a5fa, #2563eb); }
.role-stat.student .role-icon { background: linear-gradient(135deg, #2563eb, #1d4ed8); }

.role-detail {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.role-num {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  line-height: 1;
}

.role-name {
  font-size: 12px;
  color: #909399;
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

.batch-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%);
  border: 1px solid #fed7aa;
  border-radius: 10px;
  margin-top: 14px;
}

.batch-count {
  color: #ea580c;
  font-size: 13px;
  font-weight: 600;
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

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
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

.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  flex-shrink: 0;
  border-radius: 50%;
  padding: 0;
  background: transparent;
  overflow: visible;
}

.user-avatar.avatar-ADMIN { }
.user-avatar.avatar-LAB_ADMIN { }
.user-avatar.avatar-TEACHER { }
.user-avatar.avatar-STUDENT { }

.user-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-name {
  font-weight: 600;
  color: #1a1a2e;
}

.user-role-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
  width: fit-content;
}

.user-role-tag.role-ADMIN { background: #fde2e2; color: #d10000; }
.user-role-tag.role-LAB_ADMIN { background: #e1f3d8; color: #529b2e; }
.user-role-tag.role-TEACHER { background: #faecd8; color: #c68c06; }
.user-role-tag.role-STUDENT { background: #ecf5ff; color: #409eff; }

.contact-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
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
  border-radius: 12px;
  margin-bottom: 20px;
}

.dialog-hero-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
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

.dialog-hero.hero-ADMIN {
  background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
}
.dialog-hero.hero-ADMIN .dialog-hero-icon {
  background: linear-gradient(135deg, #2563eb, #1e40af);
}

.dialog-hero.hero-LAB_ADMIN {
  background: linear-gradient(135deg, #ecfeff 0%, #cffafe 100%);
}
.dialog-hero.hero-LAB_ADMIN .dialog-hero-icon {
  background: linear-gradient(135deg, #3b82f6, #0ea5e9);
}

.dialog-hero.hero-TEACHER {
  background: linear-gradient(135deg, #fdf2f8 0%, #fce7f3 100%);
}
.dialog-hero.hero-TEACHER .dialog-hero-icon {
  background: linear-gradient(135deg, #60a5fa, #2563eb);
}

.dialog-hero.hero-STUDENT {
  background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%);
}
.dialog-hero.hero-STUDENT .dialog-hero-icon {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
}

.avatar-uploader :deep(.el-upload) {
  border: 2px dashed #2563eb;
  border-radius: 50%;
  cursor: pointer;
  width: 90px;
  height: 90px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  transition: all 0.3s;
  background: #eff6ff;
}

.avatar-uploader :deep(.el-upload:hover) {
  border-color: #409EFF;
  background: #ecf5ff;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
}

.avatar-preview {
  width: 90px;
  height: 90px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-tip {
  text-align: center;
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}

.role-radio-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.role-radio {
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid #dcdfe6;
  transition: all 0.3s;
}

.role-radio.is-checked {
  font-weight: 600;
}

.role-radio.role-ADMIN.is-checked {
  border-color: #2563eb;
  background: #fef2f2;
  color: #2563eb;
}

.role-radio.role-LAB_ADMIN.is-checked {
  border-color: #3b82f6;
  background: #ecfeff;
  color: #3b82f6;
}

.role-radio.role-TEACHER.is-checked {
  border-color: #2563eb;
  background: #fdf2f8;
  color: #2563eb;
}

.role-radio.role-STUDENT.is-checked {
  border-color: #2563eb;
  background: #f5f3ff;
  color: #2563eb;
}

.el-pagination :deep(.btn-prev),
.el-pagination :deep(.btn-next),
.el-pagination :deep(.el-pager li) {
  border-radius: 8px;
}
</style>