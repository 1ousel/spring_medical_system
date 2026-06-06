<template>
  <div class="page-container">
    <el-row :gutter="12" style="margin-bottom:16px">
      <el-col :span="6" v-for="s in keyStats" :key="s.label">
        <el-card shadow="hover" class="key-stat">
          <el-icon :size="28" :style="{ color: s.color }"><component :is="s.icon" /></el-icon>
          <div class="key-stat-num" :style="{ color: s.color }">{{ s.value }}</div>
          <div class="key-stat-label">{{ s.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <b>重点人群列表</b>
          <el-radio-group v-model="activeCategory" size="small">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="critical">危重</el-radio-button>
            <el-radio-button value="poor">较差</el-radio-button>
            <el-radio-button value="normal">一般</el-radio-button>
            <el-radio-button value="good">良好</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-table :data="filteredList" border stripe>
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column prop="age" label="年龄" width="65" align="center" />
        <el-table-column prop="room" label="床位" width="90" />
        <el-table-column prop="healthStatus" label="健康状态" width="100">
          <template #default="{ row }">
            <el-tag :type="categoryColor(row.healthStatus)" size="small">{{ row.healthStatusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="medicalHistory" label="既往病史" show-overflow-tooltip />
        <el-table-column prop="doctor" label="责任医生" width="100" />
        <el-table-column prop="createTime" label="建档时间" width="160" />
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" type="primary" @click="viewPlan(row)">干预方案</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="planVisible" title="健康干预方案" width="620px">
      <div v-if="currentElder">
        <el-alert :title="`${currentElder.name} - ${currentElder.healthStatusLabel}`" type="warning" show-icon :closable="false" style="margin-bottom:16px" />
        <el-form label-width="100px">
          <el-form-item label="风险评分">
            <el-rate v-model="rateValue" :max="5" disabled allow-half />
            <span style="margin-left:8px;color:#fa8c16">健康状态：{{ currentElder.healthStatusLabel }}</span>
          </el-form-item>
          <el-form-item label="干预措施">
            <el-checkbox-group v-model="measures">
              <el-checkbox value="monitor">加强监测频率</el-checkbox>
              <el-checkbox value="medicine">调整用药方案</el-checkbox>
              <el-checkbox value="diet">饮食干预</el-checkbox>
              <el-checkbox value="rehab">康复训练</el-checkbox>
              <el-checkbox value="psych">心理疏导</el-checkbox>
              <el-checkbox value="family">家属告知</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="干预方案">
            <el-input v-model="planTarget" type="textarea" :rows="3" placeholder="请输入本阶段干预方案" />
          </el-form-item>
          <el-form-item label="下次复评时间">
            <el-date-picker v-model="nextEvalDate" type="date" style="width:100%" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="planVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePlan">保存方案</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { elderApi } from '@/api/elder'

const activeCategory = ref('all')
const planVisible = ref(false)
const currentElder = ref(null)
const rateValue = ref(3)
const measures = ref(['monitor', 'family'])
const planTarget = ref('')
const nextEvalDate = ref('')
const loading = ref(false)
const saving = ref(false)

const healthStatusMap = { good: '良好', normal: '一般', poor: '较差', critical: '危重' }

const keyStats = ref([
  { label: '危重老人', value: 0, icon: 'Warning', color: '#ff4d4f', status: 'critical' },
  { label: '较差老人', value: 0, icon: 'HeartPulse', color: '#fa8c16', status: 'poor' },
  { label: '一般老人', value: 0, icon: 'Aim', color: '#722ed1', status: 'normal' },
  { label: '良好老人', value: 0, icon: 'BedNight', color: '#52c41a', status: 'good' }
])

const allElders = ref([])

const filteredList = computed(() => {
  if (activeCategory.value === 'all') return allElders.value
  return allElders.value.filter(e => e.healthStatus === activeCategory.value)
})

async function loadList() {
  loading.value = true
  try {
    const res = await elderApi.list({ page: 1, size: 100 })
    allElders.value = (res.data?.records || []).map(e => ({
      ...e,
      healthStatusLabel: healthStatusMap[e.healthStatus] || e.healthStatus,
      interventionPlan: e.remark || ''
    }))
    keyStats.value.forEach(s => {
      s.value = allElders.value.filter(e => e.healthStatus === s.status).length
    })
  } finally { loading.value = false }
}

function categoryColor(status) {
  return { critical: 'danger', poor: 'warning', normal: '', good: 'success' }[status] || 'info'
}
function viewPlan(row) {
  currentElder.value = row
  rateValue.value = { critical: 5, poor: 4, normal: 3, good: 2 }[row.healthStatus] || 3
  planTarget.value = row.interventionPlan || ''
  planVisible.value = true
}
async function savePlan() {
  if (!currentElder.value) return
  saving.value = true
  try {
    const { healthStatusLabel, interventionPlan, ...elder } = currentElder.value
    await elderApi.update(currentElder.value.id, { ...elder, remark: planTarget.value })
    ElMessage.success('干预方案已保存')
    planVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.key-stat { text-align: center; }
.key-stat :deep(.el-card__body) { display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 16px; }
.key-stat-num { font-size: 28px; font-weight: 700; line-height: 1; }
.key-stat-label { font-size: 12px; color: #888; }
</style>
