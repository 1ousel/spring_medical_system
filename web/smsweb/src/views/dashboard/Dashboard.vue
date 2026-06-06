<template>
  <div class="dashboard">
    <div class="page-header">
      <div>
        <p class="eyebrow">Medical Care Overview</p>
        <h2>系统首页</h2>
      </div>
      <span class="date-text">{{ dateText }}</span>
    </div>

    <el-row :gutter="16" class="stat-cards">
      <el-col :span="6" v-for="card in statCards" :key="card.title">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div>
              <div class="stat-value">{{ card.value }}</div>
              <div class="stat-title">{{ card.title }}</div>
            </div>
            <div class="stat-icon" :style="{ background: card.color + '20', color: card.color }">
              <el-icon size="28"><component :is="card.icon" /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header><b>近7日健康预警趋势</b></template>
          <VChart :option="lineChartOption" style="height:280px" autoresize />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><b>预警类型分布</b></template>
          <VChart :option="pieChartOption" style="height:280px" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header><b>老人年龄分布</b></template>
          <VChart :option="barChartOption" style="height:260px" autoresize />
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <b>最新健康预警</b>
              <el-button size="small" text type="primary" @click="$router.push('/health')">查看全部</el-button>
            </div>
          </template>
          <el-table :data="recentAlerts" size="small" :show-header="true">
            <el-table-column prop="elderName" label="老人" width="80" />
            <el-table-column prop="type" label="预警类型" width="100">
              <template #default="{ row }">
                <el-tag :type="row.level === 'high' ? 'danger' : row.level === 'mid' ? 'warning' : 'info'" size="small">{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="time" label="时间" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === '已处理' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { dashboardApi } from '@/api/dashboard'
import { alertApi } from '@/api/alert'

use([LineChart, PieChart, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

const PIE_COLORS = ['#0fb5c3', '#12c48b', '#5c7cfa', '#ffb347', '#ff6b6b', '#722ed1']

const dateText = computed(() => new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }))

const statCards = ref([
  { title: '在院老人总数', value: '-', icon: 'User', color: '#0fb5c3' },
  { title: '注册医生数', value: '-', icon: 'Briefcase', color: '#12c48b' },
  { title: '今日预警数', value: '-', icon: 'Warning', color: '#ff9f43' },
  { title: '本月评估次数', value: '-', icon: 'Document', color: '#5c7cfa' }
])

const recentAlerts = ref([])

const statusLabelMap = { pending: '未处理', processing: '处理中', handled: '已处理' }

const lineChartOption = ref({
  tooltip: { trigger: 'axis' },
  legend: { data: [] },
  grid: { left: 30, right: 20, bottom: 30, top: 40, containLabel: true },
  xAxis: { type: 'category', data: [] },
  yAxis: { type: 'value', minInterval: 1 },
  series: []
})

const pieChartOption = ref({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0, itemWidth: 12, itemHeight: 12, textStyle: { fontSize: 12 } },
  series: [{
    type: 'pie',
    radius: ['40%', '65%'],
    center: ['50%', '42%'],
    data: []
  }]
})

const barChartOption = ref({
  tooltip: { trigger: 'axis' },
  grid: { left: 30, right: 10, bottom: 30, top: 20, containLabel: true },
  xAxis: { type: 'category', data: [] },
  yAxis: { type: 'value', minInterval: 1 },
  series: [{
    type: 'bar',
    barWidth: '50%',
    data: [],
    itemStyle: { color: '#0f9fb3', borderRadius: [10, 10, 0, 0] },
    emphasis: { itemStyle: { color: '#0fb5c3' } }
  }]
})

function applyChartData(charts) {
  if (!charts) return

  const trend = charts.alertTrend || {}
  const dates = trend.dates || []
  const series = (trend.series || []).map((s, idx) => ({
    name: s.name,
    type: 'line',
    smooth: true,
    data: s.data || [],
    itemStyle: { color: PIE_COLORS[idx % PIE_COLORS.length] },
    areaStyle: { color: `${PIE_COLORS[idx % PIE_COLORS.length]}14` }
  }))
  lineChartOption.value = {
    ...lineChartOption.value,
    legend: { data: series.map(s => s.name) },
    xAxis: { type: 'category', data: dates },
    series
  }

  const pieData = (charts.alertTypePie || []).map((item, idx) => ({
    ...item,
    itemStyle: { color: PIE_COLORS[idx % PIE_COLORS.length] }
  }))
  pieChartOption.value = {
    ...pieChartOption.value,
    series: [{ ...pieChartOption.value.series[0], data: pieData }]
  }

  const ageDist = charts.ageDistribution || {}
  const ageLabels = Object.keys(ageDist)
  barChartOption.value = {
    ...barChartOption.value,
    xAxis: { type: 'category', data: ageLabels },
    series: [{ ...barChartOption.value.series[0], data: ageLabels.map(k => ageDist[k]) }]
  }
}

async function loadStats() {
  try {
    const res = await dashboardApi.stats()
    const d = res.data || {}
    statCards.value[0].value = d.elderCount ?? 0
    statCards.value[1].value = d.doctorCount ?? 0
    statCards.value[2].value = d.todayAlertCount ?? 0
    statCards.value[3].value = d.monthAssessCount ?? 0
    applyChartData(d.charts)
  } catch {}
}

async function loadRecentAlerts() {
  try {
    const res = await alertApi.list({ page: 1, size: 5 })
    recentAlerts.value = (res.data?.records || []).map(a => ({
      elderName: a.elderName,
      type: a.alertType,
      level: a.level,
      time: a.createTime?.substring(11, 16) || '',
      status: statusLabelMap[a.status] || a.status
    }))
  } catch {}
}

onMounted(() => { loadStats(); loadRecentAlerts() })
</script>

<style scoped>
.dashboard { }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
.page-header h2 { font-size: 24px; color: #0f1f2b; margin: 4px 0 0; letter-spacing: 0.3px; }
.eyebrow { font-size: 12px; color: #5b707e; text-transform: uppercase; letter-spacing: 1px; margin: 0; }
.date-text { color: #5b707e; font-size: 13px; }
.stat-cards { margin-bottom: 16px; }
.stat-card { border-radius: 14px; background: linear-gradient(135deg, rgba(255,255,255,0.96), rgba(245, 251, 255, 0.95)); }
.stat-content { display: flex; justify-content: space-between; align-items: center; }
.stat-value { font-size: 32px; font-weight: 700; color: #0f1f2b; line-height: 1; letter-spacing: 0.2px; }
.stat-title { font-size: 13px; color: #5b707e; margin: 6px 0 4px; }
.stat-icon { width: 60px; height: 60px; border-radius: 14px; display: flex; align-items: center; justify-content: center; box-shadow: 0 18px 38px rgba(12, 125, 150, 0.18); }
.chart-row { margin-bottom: 16px; }
</style>
