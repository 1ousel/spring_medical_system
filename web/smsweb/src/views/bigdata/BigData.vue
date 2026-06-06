<template>
  <div class="bigdata-page" v-loading="loading">
    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span class="card-title">老人健康状态综合雷达图</span></template>
          <div ref="radarChart" class="chart-box-lg"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span class="card-title">预警趋势热力分析</span></template>
          <div ref="heatmapChart" class="chart-box-lg"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span class="card-title">医疗资源配置分析</span></template>
          <div ref="resourceChart" class="chart-box-lg"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span class="card-title">评估等级对比分析</span></template>
          <div ref="assessChart" class="chart-box-lg"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span class="card-title">设备运行状态监控</span></template>
          <div ref="deviceGaugeChart" class="chart-box-lg"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span class="card-title">决策建议</span></template>
          <div class="suggestion-box">
            <el-alert v-for="(s, i) in suggestions" :key="i" :title="s.title" :description="s.desc" :type="s.type" show-icon :closable="false" class="suggestion-item" />
          </div>
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
const charts = []
const radarChart = ref(null)
const heatmapChart = ref(null)
const resourceChart = ref(null)
const assessChart = ref(null)
const deviceGaugeChart = ref(null)
const suggestions = ref([])

function createChart(el) { const c = echarts.init(el); charts.push(c); return c }

async function loadData() {
  loading.value = true
  try {
    const res = await statisticsApi.overview()
    const d = res.data || {}
    await nextTick()
    renderCharts(d)
    generateSuggestions(d)
  } finally { loading.value = false }
}

function generateSuggestions(d) {
  const list = []
  const healthDist = d.healthDistribution || {}
  const criticalCount = healthDist.critical || 0
  const poorCount = healthDist.poor || 0
  if (criticalCount > 0) list.push({ type: 'error', title: `当前有 ${criticalCount} 位危重老人`, desc: '建议立即安排专人看护并通知家属，定期更新健康记录。' })
  if (poorCount > 0) list.push({ type: 'warning', title: `当前有 ${poorCount} 位健康状态较差老人`, desc: '建议增加巡查频率，安排定期医生会诊。' })

  const alertStatusDist = d.alertStatusDistribution || {}
  const pendingAlerts = alertStatusDist.pending || 0
  if (pendingAlerts > 3) list.push({ type: 'error', title: `有 ${pendingAlerts} 条预警待处理`, desc: '未处理预警过多，建议增加值班医护力量，及时响应。' })
  else if (pendingAlerts > 0) list.push({ type: 'warning', title: `有 ${pendingAlerts} 条预警待处理`, desc: '请安排医护人员尽快处理。' })

  const deviceStatusDist = d.deviceStatusDistribution || {}
  const faultDevices = deviceStatusDist.fault || 0
  const offlineDevices = deviceStatusDist.offline || 0
  if (faultDevices > 0 || offlineDevices > 0) list.push({ type: 'warning', title: `${faultDevices} 台故障 / ${offlineDevices} 台离线设备`, desc: '建议安排维修人员检查，确保监测覆盖率。' })

  const doctorElderDist = d.doctorElderDistribution || {}
  const maxLoad = Math.max(...Object.values(doctorElderDist), 0)
  if (maxLoad > 5) list.push({ type: 'info', title: '部分医生负责老人数较多', desc: `最高负责 ${maxLoad} 人，建议均衡分配责任老人。` })

  if (list.length === 0) list.push({ type: 'success', title: '系统运行正常', desc: '各项指标良好，暂无需要关注的异常。' })
  suggestions.value = list
}

function renderCharts(d) {
  // 雷达图 - 各维度综合评分
  if (radarChart.value) {
    const c = createChart(radarChart.value)
    const healthDist = d.healthDistribution || {}
    const total = d.elderCount || 1
    c.setOption({
      tooltip: {},
      radar: {
        indicator: [
          { name: '健康率', max: 100 },
          { name: '预警处理率', max: 100 },
          { name: '评估覆盖率', max: 100 },
          { name: '设备在线率', max: 100 },
          { name: '医护配比', max: 100 }
        ]
      },
      series: [{
        type: 'radar',
        data: [{
          value: [
            Math.round(((healthDist.good || 0) / total) * 100),
            Math.round((((d.alertStatusDistribution || {}).handled || 0) / Math.max(d.alertCount || 1, 1)) * 100),
            Math.min(Math.round(((d.assessCount || 0) / total) * 100), 100),
            Math.round((((d.deviceStatusDistribution || {}).normal || 0) / Math.max(d.deviceCount || 1, 1)) * 100),
            Math.min(Math.round(((d.doctorCount || 0) + (d.nurseCount || 0)) / total * 100), 100)
          ],
          name: '综合指标',
          areaStyle: { opacity: 0.3 }
        }]
      }]
    })
  }

  // 热力图 - 预警类型 × 等级
  if (heatmapChart.value) {
    const c = createChart(heatmapChart.value)
    const alertTypes = Object.keys(d.alertTypeDistribution || {})
    const levels = ['low', 'mid', 'high']
    const levelNames = ['低危', '中危', '高危']
    // 生成模拟热力数据
    const heatData = []
    alertTypes.forEach((type, i) => {
      levels.forEach((lv, j) => {
        heatData.push([i, j, Math.floor(Math.random() * 5) + 1])
      })
    })
    c.setOption({
      tooltip: { formatter: p => `${alertTypes[p.value[0]]}<br/>${levelNames[p.value[1]]}：${p.value[2]}次` },
      grid: { left: 80, bottom: 60 },
      xAxis: { type: 'category', data: alertTypes, axisLabel: { rotate: 20 } },
      yAxis: { type: 'category', data: levelNames },
      visualMap: { min: 0, max: 6, orient: 'horizontal', left: 'center', bottom: 0, inRange: { color: ['#e6f7ff', '#1890ff', '#f5222d'] } },
      series: [{ type: 'heatmap', data: heatData, label: { show: true } }]
    })
  }

  // 资源配置分析 - 堆叠柱状图
  if (resourceChart.value) {
    const c = createChart(resourceChart.value)
    const doctorDist = d.doctorElderDistribution || {}
    const doctors = Object.keys(doctorDist)
    const elderCounts = Object.values(doctorDist)
    // 生成模拟设备绑定数
    const deviceCounts = elderCounts.map(() => Math.floor(Math.random() * 3) + 1)
    c.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['负责老人', '绑定设备'] },
      xAxis: { type: 'category', data: doctors },
      yAxis: { type: 'value', name: '数量' },
      series: [
        { name: '负责老人', type: 'bar', stack: 'total', data: elderCounts, itemStyle: { color: '#1890ff' } },
        { name: '绑定设备', type: 'bar', stack: 'total', data: deviceCounts, itemStyle: { color: '#52c41a' } }
      ]
    })
  }

  // 评估等级分析 - 漏斗图
  if (assessChart.value) {
    const c = createChart(assessChart.value)
    const assessLevelDist = d.assessLevelDistribution || {}
    const levelOrder = ['优', '良', '中', '差']
    c.setOption({
      tooltip: { trigger: 'item' },
      color: ['#52c41a', '#1890ff', '#faad14', '#f5222d'],
      series: [{
        type: 'funnel', left: '10%', width: '80%', sort: 'none',
        label: { formatter: '{b}: {c}份' },
        data: levelOrder.map(l => ({ name: l, value: assessLevelDist[l] || 0 }))
      }]
    })
  }

  // 设备仪表盘
  if (deviceGaugeChart.value) {
    const c = createChart(deviceGaugeChart.value)
    const deviceStatusDist = d.deviceStatusDistribution || {}
    const total = d.deviceCount || 1
    const onlineRate = Math.round(((deviceStatusDist.normal || 0) / total) * 100)
    c.setOption({
      series: [
        {
          type: 'gauge', radius: '80%',
          axisLine: { lineStyle: { width: 20, color: [[0.6, '#f5222d'], [0.8, '#faad14'], [1, '#52c41a']] } },
          pointer: { itemStyle: { color: 'auto' } },
          axisTick: { distance: -20, length: 6 },
          splitLine: { distance: -20, length: 20 },
          detail: { formatter: '{value}%', fontSize: 24, offsetCenter: [0, '70%'] },
          data: [{ value: onlineRate, name: '设备在线率' }]
        }
      ]
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
.bigdata-page { padding: 0; }
.chart-row { margin-bottom: 16px; }
.chart-box-lg { height: 340px; }
.card-title { font-weight: 600; }
.suggestion-box { max-height: 340px; overflow-y: auto; display: flex; flex-direction: column; gap: 12px; }
.suggestion-item { border-radius: 8px; }
</style>
