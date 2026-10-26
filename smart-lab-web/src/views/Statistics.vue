<template>
  <div class="page-container">
    <div class="hero-section">
      <div class="hero-content">
        <div class="hero-icon">
          <el-icon :size="32"><DataAnalysis /></el-icon>
        </div>
        <div class="hero-text">
          <h2>数据统计分析</h2>
          <p>全方位洞察实验室设备与使用情况</p>
        </div>
      </div>
      <el-button @click="refreshData" :icon="Refresh" plain>刷新数据</el-button>
    </div>

    <div class="overview-cards">
      <div class="overview-card" v-for="stat in overviewStats" :key="stat.label"
        :style="{ background: stat.gradient }">
        <div class="ov-icon">
          <el-icon :size="24"><component :is="stat.icon" /></el-icon>
        </div>
        <div class="ov-info">
          <span class="ov-num">{{ stat.value }}</span>
          <span class="ov-label">{{ stat.label }}</span>
        </div>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :xs="24" :lg="12">
        <div class="panel">
          <div class="panel-header">
            <div class="panel-title">
              <div class="title-icon" style="background: linear-gradient(135deg, #11998e, #38ef7d);">
                <el-icon :size="16"><Monitor /></el-icon>
              </div>
              <span>设备状态分布</span>
            </div>
            <span class="panel-subtitle">实时概览</span>
          </div>
          <div ref="devicePieChart" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="12">
        <div class="panel">
          <div class="panel-header">
            <div class="panel-title">
              <div class="title-icon" style="background: linear-gradient(135deg, #2563eb, #1d4ed8);">
                <el-icon :size="16"><Grid /></el-icon>
              </div>
              <span>设备分类统计</span>
            </div>
            <span class="panel-subtitle">类别分布</span>
          </div>
          <div ref="categoryChart" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :xs="24" :lg="12">
        <div class="panel">
          <div class="panel-header">
            <div class="panel-title">
              <div class="title-icon" style="background: linear-gradient(135deg, #60a5fa, #2563eb);">
                <el-icon :size="16"><DocumentCopy /></el-icon>
              </div>
              <span>借用状态分布</span>
            </div>
            <span class="panel-subtitle">审批进度</span>
          </div>
          <div ref="borrowPieChart" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="12">
        <div class="panel">
          <div class="panel-header">
            <div class="panel-title">
              <div class="title-icon" style="background: linear-gradient(135deg, #fa709a, #fee140);">
                <el-icon :size="16"><TrendCharts /></el-icon>
              </div>
              <span>设备借用频次 Top 10</span>
            </div>
            <span class="panel-subtitle">热门设备</span>
          </div>
          <div ref="borrowBarChart" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getDeviceStatusStats, getBorrowStats } from '@/api/statistics'
import * as echarts from 'echarts'
import { Monitor, Grid, DocumentCopy, TrendCharts, DataAnalysis, Refresh, CircleCheck, Warning, CircleClose } from '@element-plus/icons-vue'

const devicePieChart = ref(null)
const categoryChart = ref(null)
const borrowPieChart = ref(null)
const borrowBarChart = ref(null)

let charts = []
let rawData = { device: null, borrow: null }

const handleResize = () => charts.forEach(c => c?.resize())

const createChart = (el) => {
  const chart = echarts.init(el)
  charts.push(chart)
  return chart
}

const overviewStats = computed(() => {
  const d = rawData.device || {}
  const b = rawData.borrow || {}
  return [
    { label: '设备总数', value: (d.idle || 0) + (d.borrowed || 0) + (d.repairing || 0) + (d.scrapped || 0), icon: Monitor, gradient: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)' },
    { label: '在用设备', value: d.borrowed || 0, icon: CircleCheck, gradient: 'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)' },
    { label: '待审批', value: b.pending || 0, icon: Warning, gradient: 'linear-gradient(135deg, #60a5fa 0%, #2563eb 100%)' },
    { label: '已报废', value: d.scrapped || 0, icon: CircleClose, gradient: 'linear-gradient(135deg, #434343 0%, #000000 100%)' },
  ]
})

const emptyOption = (text) => ({
  graphic: {
    type: 'text',
    left: 'center',
    top: 'middle',
    style: { text, fill: '#c0c4cc', fontSize: 14 },
  },
})

const pieOption = (data, colors) => {
  const hasData = data.some(i => i.value > 0)
  if (!hasData) {
    return emptyOption('暂无数据')
  }
  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(26,26,46,0.95)',
      border: 'none',
      textStyle: { color: '#fff', fontSize: 12 },
      formatter: '{b}: {c} ({d}%)',
    },
    legend: {
      bottom: 0,
      left: 'center',
      icon: 'circle',
      itemWidth: 8,
      itemHeight: 8,
      itemGap: 14,
      textStyle: { fontSize: 12, color: '#606266' },
      type: data.length > 6 ? 'scroll' : 'plain',
    },
    series: [{
      type: 'pie',
      radius: ['45%', '72%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 3 },
      label: { show: false },
      emphasis: {
        scale: true,
        scaleSize: 8,
        label: { show: true, fontSize: 14, fontWeight: 'bold', formatter: '{b}\n{c}' },
      },
      data: data.map((i, idx) => ({
        value: i.value,
        name: i.name,
        itemStyle: { color: colors ? colors[idx % colors.length] : i.itemStyle?.color },
      })),
    }],
  }
}

const barOption = (topDevices) => {
  if (!topDevices || topDevices.length === 0) {
    return emptyOption('暂无借用记录')
  }
  const maxCount = Math.max(...topDevices.map(d => d.count), 1)
  const axisMax = maxCount <= 1 ? 2 : Math.ceil(maxCount * 1.2)
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(26,26,46,0.95)',
      border: 'none',
      textStyle: { color: '#fff', fontSize: 12 },
      formatter: (params) => `${params[0].name}<br/>借用次数: <b>${params[0].value}</b>`,
    },
    grid: { left: 100, right: 50, top: 10, bottom: 10 },
    xAxis: {
      type: 'value',
      min: 0,
      max: axisMax,
      minInterval: 1,
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f0f2f5', type: 'dashed' } },
      axisLabel: { color: '#909399', fontSize: 11 },
    },
    yAxis: {
      type: 'category',
      data: topDevices.map(d => d.name),
      inverse: true,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: '#606266',
        fontSize: 12,
        width: 85,
        overflow: 'truncate',
      },
    },
    series: [{
      type: 'bar',
      data: topDevices.map((d, idx) => ({
        value: d.count,
        itemStyle: {
          borderRadius: [0, 8, 8, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: idx === 0 ? '#fa709a' : '#60a5fa' },
            { offset: 1, color: idx === 0 ? '#fee140' : '#fa5788' },
          ]),
        },
      })),
      barWidth: 18,
      label: { show: true, position: 'right', color: '#fa5788', fontSize: 13, fontWeight: 600 },
    }],
  }
}

const initCharts = async () => {
  await nextTick()

  try {
    const [deviceRes, borrowRes] = await Promise.all([getDeviceStatusStats(), getBorrowStats()])
    const deviceData = deviceRes.data
    const borrowData = borrowRes.data
    rawData.device = deviceData
    rawData.borrow = borrowData

    createChart(devicePieChart.value).setOption(pieOption([
      { value: deviceData.idle || 0, name: '空闲' },
      { value: deviceData.borrowed || 0, name: '借出' },
      { value: deviceData.repairing || 0, name: '维修中' },
      { value: deviceData.pendingDiscard || 0, name: '待审核' },
      { value: deviceData.pendingFinal || 0, name: '待终审' },
      { value: deviceData.scrapped || 0, name: '已报废' },
    ], ['#38ef7d', '#3b82f6', '#faad14', '#909399', '#b37feb', '#ff4d4f']))

    const categoryData = deviceData.categoryDistribution || {}
    const catColors = ['#2563eb', '#43e97b', '#60a5fa', '#fa709a', '#11998e', '#fa8c16']
    createChart(categoryChart.value).setOption(pieOption(
      Object.entries(categoryData).map(([name, value]) => ({ name, value })),
      catColors,
    ))

    createChart(borrowPieChart.value).setOption(pieOption([
      { value: borrowData.pending || 0, name: '待审批' },
      { value: borrowData.approved || 0, name: '借用中' },
      { value: borrowData.rejected || 0, name: '已拒绝' },
      { value: borrowData.returned || 0, name: '已归还' },
      { value: borrowData.cancelled || 0, name: '已取消' },
      { value: borrowData.overdue || 0, name: '逾期' },
    ], ['#faad14', '#43e97b', '#ff4d4f', '#3b82f6', '#c0c4cc', '#ff7a45']))

    createChart(borrowBarChart.value).setOption(barOption(borrowData.topDevices || []))
  } catch (e) {
    console.error(e)
  }
}

const refreshData = async () => {
  charts.forEach(c => c?.dispose())
  charts = []
  await initCharts()
  ElMessage.success('数据已刷新')
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  initCharts()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach(c => c?.dispose())
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
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
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
  background: radial-gradient(circle, rgba(255,255,255,0.15) 0%, transparent 70%);
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
  background: rgba(255,255,255,0.2);
  border-color: rgba(255,255,255,0.3);
  color: #fff;
  position: relative;
  z-index: 1;
}

.overview-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.overview-card {
  border-radius: 14px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  color: #fff;
  box-shadow: 0 4px 15px rgba(0,0,0,0.1);
  transition: all 0.3s ease;
}

.overview-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0,0,0,0.15);
}

.ov-icon {
  width: 48px;
  height: 48px;
  background: rgba(255,255,255,0.2);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  backdrop-filter: blur(10px);
}

.ov-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ov-num {
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
}

.ov-label {
  font-size: 13px;
  opacity: 0.9;
}

.panel {
  background: #fff;
  border-radius: 16px;
  padding: 22px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  height: 100%;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
}

.title-icon {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.panel-subtitle {
  font-size: 12px;
  color: #c0c4cc;
}

.chart-box {
  height: 340px;
  width: 100%;
}
</style>