<template>
  <div class="statistics-page" v-loading="loading">
    <!-- 顶部概览卡片 -->
    <el-row :gutter="16" class="stats-cards">
      <el-col :span="4" v-for="card in overviewCards" :key="card.label">
        <el-card shadow="hover" class="stat-card" :style="{ borderTop: `3px solid ${card.color}` }">
          <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第一行图表 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span class="card-title">老人健康状态分布</span></template>
          <div ref="healthChart" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span class="card-title">老人年龄段分布</span></template>
          <div ref="ageChart" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span class="card-title">老人性别比例</span></template>
          <div ref="genderChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第二行图表 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span class="card-title">预警等级分布</span></template>
          <div ref="alertLevelChart" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span class="card-title">预警类型统计</span></template>
          <div ref="alertTypeChart" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span class="card-title">预警处理状态</span></template>
          <div ref="alertStatusChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第三行图表 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span class="card-title">评估类型分布</span></template>
          <div ref="assessTypeChart" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span class="card-title">设备类型与状态</span></template>
          <div ref="deviceChart" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span class="card-title">各医生负责老人数</span></template>
          <div ref="doctorChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { statisticsApi } from '@/api/statistics'

const loading = ref(false)
const data = ref({})

const healthChart = ref(null)
const ageChart = ref(null)
const genderChart = ref(null)
const alertLevelChart = ref(null)
const alertTypeChart = ref(null)
const alertStatusChart = ref(null)
const assessTypeChart = ref(null)
const deviceChart = ref(null)
const doctorChart = ref(null)

const charts = []

const overviewCards = ref([])

const healthStatusMap = { good: '健康', normal: '一般', poor: '较差', critical: '危重' }
const alertLevelMap = { high: '高危', mid: '中危', low: '低危' }
const alertStatusMap = { pending: '待处理', processing: '处理中', handled: '已处理' }
const deviceStatusMap = { normal: '正常', offline: '离线', fault: '故障', maintaining: '维护中' }

async function loadData() {
  loading.value = true
  try {
    const res = await statisticsApi.overview()
    const d = res.data || {}
    data.value = d
    overviewCards.value = [
      { label: '老人总数', value: d.elderCount || 0, color: '#1890ff' },
      { label: '医生数量', value: d.doctorCount || 0, color: '#52c41a' },
      { label: '护士数量', value: d.nurseCount || 0, color: '#faad14' },
      { label: '设备总数', value: d.deviceCount || 0, color: '#722ed1' },
      { label: '预警总数', value: d.alertCount || 0, color: '#f5222d' },
      { label: '评估报告', value: d.assessCount || 0, color: '#13c2c2' }
    ]
    await nextTick()
    renderCharts(d)
  } finally {
    loading.value = false
  }
}

function createChart(el) {
  const c = echarts.init(el)
  charts.push(c)
  return c
}

function renderCharts(d) {
  // 健康状态饼图
  if (healthChart.value) {
    const c = createChart(healthChart.value)
    c.setOption({
      tooltip: { trigger: 'item' },
      color: ['#52c41a', '#1890ff', '#faad14', '#f5222d'],
      series: [{
        type: 'pie', radius: ['40%', '70%'],
        label: { formatter: '{b}: {c}人' },
        data: Object.entries(d.healthDistribution || {}).map(([k, v]) => ({ name: healthStatusMap[k] || k, value: v }))
      }]
    })
  }

  // 年龄段柱状图
  if (ageChart.value) {
    const c = createChart(ageChart.value)
    const ageDist = d.ageDistribution || {}
    c.setOption({
      tooltip: { trigger: 'axis' },
      color: ['#1890ff'],
      xAxis: { type: 'category', data: Object.keys(ageDist) },
      yAxis: { type: 'value', name: '人数' },
      series: [{ type: 'bar', data: Object.values(ageDist), barWidth: 40, itemStyle: { borderRadius: [4, 4, 0, 0] } }]
    })
  }

  // 性别饼图
  if (genderChart.value) {
    const c = createChart(genderChart.value)
    c.setOption({
      tooltip: { trigger: 'item' },
      color: ['#1890ff', '#ff85c0'],
      series: [{
        type: 'pie', radius: '65%',
        label: { formatter: '{b}: {c}人 ({d}%)' },
        data: Object.entries(d.genderDistribution || {}).map(([k, v]) => ({ name: k, value: v }))
      }]
    })
  }

  // 预警等级
  if (alertLevelChart.value) {
    const c = createChart(alertLevelChart.value)
    c.setOption({
      tooltip: { trigger: 'item' },
      color: ['#f5222d', '#faad14', '#52c41a'],
      series: [{
        type: 'pie', radius: ['40%', '70%'], roseType: 'area',
        label: { formatter: '{b}: {c}条' },
        data: Object.entries(d.alertLevelDistribution || {}).map(([k, v]) => ({ name: alertLevelMap[k] || k, value: v }))
      }]
    })
  }

  // 预警类型
  if (alertTypeChart.value) {
    const c = createChart(alertTypeChart.value)
    const typeDist = d.alertTypeDistribution || {}
    c.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 80 },
      xAxis: { type: 'value', name: '次数' },
      yAxis: { type: 'category', data: Object.keys(typeDist) },
      series: [{ type: 'bar', data: Object.values(typeDist), barWidth: 20, itemStyle: { color: '#ff7a45', borderRadius: [0, 4, 4, 0] } }]
    })
  }

  // 预警状态
  if (alertStatusChart.value) {
    const c = createChart(alertStatusChart.value)
    c.setOption({
      tooltip: { trigger: 'item' },
      color: ['#f5222d', '#faad14', '#52c41a'],
      series: [{
        type: 'pie', radius: '65%',
        label: { formatter: '{b}: {c}条' },
        data: Object.entries(d.alertStatusDistribution || {}).map(([k, v]) => ({ name: alertStatusMap[k] || k, value: v }))
      }]
    })
  }

  // 评估类型
  if (assessTypeChart.value) {
    const c = createChart(assessTypeChart.value)
    const atDist = d.assessTypeDistribution || {}
    c.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 120 },
      xAxis: { type: 'value', name: '份数' },
      yAxis: { type: 'category', data: Object.keys(atDist), axisLabel: { width: 100, overflow: 'truncate' } },
      series: [{ type: 'bar', data: Object.values(atDist), barWidth: 20, itemStyle: { color: '#13c2c2', borderRadius: [0, 4, 4, 0] } }]
    })
  }

  // 设备类型+状态
  if (deviceChart.value) {
    const c = createChart(deviceChart.value)
    c.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      color: ['#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1'],
      series: [{
        type: 'pie', radius: ['35%', '60%'], center: ['50%', '45%'],
        label: { formatter: '{b}: {c}' },
        data: [
          ...Object.entries(d.deviceTypeDistribution || {}).map(([k, v]) => ({ name: k, value: v }))
        ]
      }]
    })
  }

  // 医生负责老人
  if (doctorChart.value) {
    const c = createChart(doctorChart.value)
    const dd = d.doctorElderDistribution || {}
    c.setOption({
      tooltip: { trigger: 'axis' },
      color: ['#722ed1'],
      xAxis: { type: 'category', data: Object.keys(dd), axisLabel: { rotate: 15 } },
      yAxis: { type: 'value', name: '人数', minInterval: 1 },
      series: [{ type: 'bar', data: Object.values(dd), barWidth: 35, itemStyle: { borderRadius: [4, 4, 0, 0] } }]
    })
  }
}

function handleResize() { charts.forEach(c => c.resize()) }

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach(c => c.dispose())
})
</script>

<style scoped>
.statistics-page { padding: 0; }
.stats-cards { margin-bottom: 16px; }
.stat-card { text-align: center; }
.stat-value { font-size: 28px; font-weight: 700; }
.stat-label { font-size: 13px; color: #999; margin-top: 4px; }
.chart-row { margin-bottom: 16px; }
.chart-box { height: 280px; }
.card-title { font-weight: 600; }
</style>
