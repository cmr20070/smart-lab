<template>
  <div class="page-container">
    <div class="page-hero">
      <div class="hero-left">
        <div class="hero-icon">
          <el-icon :size="26"><Files /></el-icon>
        </div>
        <div>
          <h2 class="hero-title">报废管理</h2>
          <p class="hero-sub">审核实验室管理员提交的设备报废申请</p>
        </div>
      </div>
      <div class="hero-actions">
        <el-button size="small" @click="fetchData">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
      </div>
    </div>

    <el-card class="content-card" shadow="never">
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column label="序号" width="70" align="center">
          <template #default="{ $index }">
            <span class="seq-num">{{ (page - 1) * size + $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="设备" min-width="200">
          <template #default="{ row }">
            <div class="name-cell">
              <div class="name-text">{{ row.name }}</div>
              <div class="name-id">{{ row.code }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="所属实验室" min-width="160">
          <template #default="{ row }">
            {{ labName(row.labId) }}
          </template>
        </el-table-column>
        <el-table-column label="分类" width="140">
          <template #default="{ row }">{{ row.category || '-' }}</template>
        </el-table-column>
        <el-table-column label="型号" width="140">
          <template #default="{ row }">{{ row.model || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="160">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="180">
          <template #default="{ row }">{{ row.updateTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="danger"
              :disabled="row.status !== 6"
              @click="handleApprove(row)"
            >
              <el-icon><CircleCheck /></el-icon>通过报废
            </el-button>
            <el-button size="small" @click="handleReject(row)">
              <el-icon><CircleClose /></el-icon>驳回
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        class="pagination"
        background
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        :total="total"
        :page-size="size"
        :current-page="page"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />

      <el-empty v-if="!loading && total === 0" description="当前没有待审核的报废申请">
        <el-icon :size="48" color="#dcdfe6"><Files /></el-icon>
      </el-empty>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDiscardList, approveFinalDiscard, rejectDiscard } from '@/api/device'
import { getAllLabs } from '@/api/lab'
import { Files, Refresh, CircleCheck, CircleClose } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const labs = ref([])

const statusLabel = (s) => {
  if (s === 4) return '待实验室管理员审核'
  if (s === 6) return '待管理员终审'
  return '待审核'
}

const statusTag = (s) => (s === 6 ? 'warning' : 'info')

const labName = (id) => {
  const lab = labs.value.find((l) => l.id === id)
  return lab ? lab.name : (id ? '实验室#' + id : '-')
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getDiscardList({ page: page.value, size: size.value })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    ElMessage.error('加载报废申请失败')
  } finally {
    loading.value = false
  }
}

const loadLabs = async () => {
  try {
    const res = await getAllLabs()
    labs.value = res.data || []
  } catch (e) {}
}

const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认通过设备「${row.name}」的报废申请？通过后设备将报废，相关待审批借用与维修将自动处理。`,
      '报废终审',
      { type: 'warning' }
    )
    await approveFinalDiscard(row.id)
    ElMessage.success('已通过报废申请')
    fetchData()
  } catch (e) {}
}

const handleReject = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认驳回设备「${row.name}」的报废申请？设备将恢复为空闲状态。`,
      '驳回报废',
      { type: 'warning' }
    )
    await rejectDiscard(row.id)
    ElMessage.success('已驳回报废申请')
    fetchData()
  } catch (e) {}
}

const handlePageChange = (p) => {
  page.value = p
  fetchData()
}

const handleSizeChange = (s) => {
  size.value = s
  page.value = 1
  fetchData()
}

onMounted(() => {
  loadLabs()
  fetchData()
})
</script>

<style scoped>
.page-container {
  min-height: 100%;
}
.page-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border-radius: 12px;
  margin-bottom: 16px;
}
.hero-left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.hero-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}
.hero-title {
  margin: 0;
  font-size: 18px;
  color: #1e293b;
}
.hero-sub {
  margin: 4px 0 0;
  font-size: 13px;
  color: #64748b;
}
.content-card {
  border-radius: 12px;
}
.seq-num {
  color: #64748b;
  font-size: 13px;
}
.name-cell {
  display: flex;
  flex-direction: column;
}
.name-text {
  font-weight: 500;
  color: #1e293b;
}
.name-id {
  font-size: 12px;
  color: #94a3b8;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>