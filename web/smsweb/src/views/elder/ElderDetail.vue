<template>
  <div class="page-container">
    <div class="page-header">
      <el-button :icon="ArrowLeft" @click="$router.back()">返回</el-button>
      <h2>老人档案详情</h2>
    </div>
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card shadow="never" class="profile-card">
          <div class="profile-avatar">
            <el-avatar :size="80" :style="{ background: elder.gender === '女' ? '#f06292' : '#1890ff', fontSize: '32px' }">
              {{ elder.name?.charAt(0) }}
            </el-avatar>
          </div>
          <div class="profile-name">{{ elder.name }}</div>
          <div class="profile-sub">{{ elder.gender }} · {{ elder.age }}岁 · {{ elder.room }}</div>
          <el-divider />
          <el-descriptions :column="1" size="small">
            <el-descriptions-item label="责任医生">{{ elder.doctor }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ elder.phone }}</el-descriptions-item>
            <el-descriptions-item label="紧急联系人">{{ elder.emergencyContact }}</el-descriptions-item>
            <el-descriptions-item label="入住日期">{{ elder.checkInDate }}</el-descriptions-item>
            <el-descriptions-item label="健康状态">
              <el-tag :type="elder.healthStatus === 'good' ? 'success' : elder.healthStatus === 'critical' ? 'danger' : 'warning'" size="small">{{ elder.healthStatusLabel }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="既往病史">{{ elder.medicalHistory || '无' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card shadow="never">
          <el-tabs>
            <el-tab-pane label="健康档案" name="health">
              <el-descriptions title="最新体检数据" :column="3" border>
                <el-descriptions-item label="血压">120/80 mmHg</el-descriptions-item>
                <el-descriptions-item label="心率">72 次/分</el-descriptions-item>
                <el-descriptions-item label="血糖">5.8 mmol/L</el-descriptions-item>
                <el-descriptions-item label="体温">36.5 °C</el-descriptions-item>
                <el-descriptions-item label="体重">58 kg</el-descriptions-item>
                <el-descriptions-item label="血氧">97%</el-descriptions-item>
              </el-descriptions>
              <el-divider />
              <h4>近期体征趋势</h4>
              <VChart :option="vitalChartOption" style="height:200px;margin-top:12px" autoresize />
            </el-tab-pane>
            <el-tab-pane label="预警记录" name="alerts">
              <el-timeline>
                <el-timeline-item v-for="alert in alertHistory" :key="alert.id"
                  :type="alert.level === 'high' ? 'danger' : 'warning'"
                  :timestamp="alert.createTime" placement="top">
                  <el-card shadow="never" size="small">
                    <b>{{ alert.alertType }}</b> — {{ alert.description }}
                    <el-tag :type="alert.status === 'handled' ? 'success' : 'danger'" size="small" style="margin-left:8px">
                      {{ alert.status === 'handled' ? '已处理' : '未处理' }}
                    </el-tag>
                  </el-card>
                </el-timeline-item>
              </el-timeline>
            </el-tab-pane>
            <el-tab-pane label="评估报告" name="assessments">
              <el-table :data="assessments" border size="small">
                <el-table-column prop="assessDate" label="评估日期" width="120" />
                <el-table-column prop="assessType" label="评估类型" />
                <el-table-column prop="score" label="评估得分" width="90" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.score >= 80 ? 'success' : row.score >= 60 ? 'warning' : 'danger'">{{ row.score }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="evaluator" label="评估人" width="100" />
                <el-table-column prop="conclusion" label="评估结论" show-overflow-tooltip />
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { TooltipComponent, GridComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { elderApi } from '@/api/elder'
import { alertApi } from '@/api/alert'
import { assessmentApi } from '@/api/assessment'

use([LineChart, TooltipComponent, GridComponent, LegendComponent, CanvasRenderer])

const route = useRoute()
const elder = ref({})
const alertHistory = ref([])
const assessments = ref([])

const statusMap = { good: '良好', normal: '一般', poor: '较差', critical: '危重' }

async function loadData() {
  const id = route.params.id
  try {
    const [elderRes, alertRes, assessRes] = await Promise.all([
      elderApi.getById(id),
      alertApi.list({ elderId: id, page: 1, size: 20 }),
      assessmentApi.list({ elderId: id, page: 1, size: 20 })
    ])
    elder.value = elderRes.data || {}
    elder.value.healthStatusLabel = statusMap[elder.value.healthStatus] || elder.value.healthStatus
    alertHistory.value = alertRes.data?.records || []
    assessments.value = assessRes.data?.records || []
  } catch {}
}

onMounted(loadData)

const vitalChartOption = {
  tooltip: { trigger: 'axis' },
  legend: { data: ['收缩压', '舒张压', '心率'] },
  grid: { left: 30, right: 10, bottom: 20, top: 30, containLabel: true },
  xAxis: { type: 'category', data: ['1月', '2月', '3月', '4月', '5月', '6月'] },
  yAxis: { type: 'value' },
  series: [
    { name: '收缩压', type: 'line', smooth: true, data: [125, 130, 122, 128, 120, 118] },
    { name: '舒张压', type: 'line', smooth: true, data: [78, 82, 76, 80, 75, 74] },
    { name: '心率', type: 'line', smooth: true, data: [72, 74, 71, 73, 70, 72] }
  ]
}
</script>

<style scoped>
.page-header { display: flex; align-items: center; gap: 16px; margin-bottom: 20px; }
.profile-card { text-align: center; }
.profile-avatar { margin-bottom: 12px; }
.profile-name { font-size: 20px; font-weight: 700; color: #1a1a2e; }
.profile-sub { color: #888; font-size: 13px; margin-top: 4px; }
</style>
