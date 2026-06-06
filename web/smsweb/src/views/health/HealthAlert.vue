<template>
  <div class="page-container">
    <el-row :gutter="12" style="margin-bottom:16px">
      <el-col :span="6" v-for="s in alertStats" :key="s.label">
        <el-card shadow="hover" class="alert-stat">
          <div class="alert-stat-inner">
            <span class="stat-num" :style="{ color: s.color }">{{ s.value }}</span>
            <span class="stat-label">{{ s.label }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <b>健康预警列表</b>
          <div style="display:flex;gap:8px">
            <el-select v-model="filterLevel" placeholder="预警级别" clearable style="width:120px" size="small" @change="loadAlerts">
              <el-option label="高危" value="high" /><el-option label="中危" value="mid" /><el-option label="低危" value="low" />
            </el-select>
            <el-select v-model="filterStatus" placeholder="处理状态" clearable style="width:120px" size="small" @change="loadAlerts">
              <el-option label="未处理" value="pending" /><el-option label="处理中" value="processing" /><el-option label="已处理" value="handled" />
            </el-select>
            <el-button size="small" type="primary" @click="loadAlerts">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="alerts" border stripe v-loading="loading">
        <el-table-column prop="id" label="预警编号" width="90" align="center" />
        <el-table-column prop="elderName" label="老人姓名" width="100" />
        <el-table-column prop="room" label="床位" width="90" />
        <el-table-column prop="alertType" label="预警类型" width="120">
          <template #default="{ row }">
            <el-tag :type="typeColor(row.alertType)" size="small">{{ row.alertType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="危险等级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.level === 'high' ? 'danger' : row.level === 'mid' ? 'warning' : 'info'" size="small">
              {{ row.level === 'high' ? '高危' : row.level === 'mid' ? '中危' : '低危' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="预警描述" show-overflow-tooltip />
        <el-table-column prop="deviceId" label="设备编号" width="110" />
        <el-table-column prop="createTime" label="预警时间" width="160" />
        <el-table-column prop="status" label="处理状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'handled' ? 'success' : row.status === 'processing' ? 'warning' : 'danger'" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="150" align="center">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="viewAlert(row)">详情</el-button>
            <el-button size="small" text type="success" :disabled="row.status === 'handled'" @click="handleProcess(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size"
          :total="total" layout="total, prev, pager, next" background small @change="loadAlerts" />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="预警详情" width="560px">
      <el-descriptions :column="2" border v-if="currentAlert">
        <el-descriptions-item label="老人姓名">{{ currentAlert.elderName }}</el-descriptions-item>
        <el-descriptions-item label="床位">{{ currentAlert.room }}</el-descriptions-item>
        <el-descriptions-item label="预警类型">{{ currentAlert.alertType }}</el-descriptions-item>
        <el-descriptions-item label="危险等级">
          <el-tag :type="currentAlert.level === 'high' ? 'danger' : currentAlert.level === 'mid' ? 'warning' : 'info'">
            {{ currentAlert.level === 'high' ? '高危' : currentAlert.level === 'mid' ? '中危' : '低危' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="预警时间" :span="2">{{ currentAlert.createTime }}</el-descriptions-item>
        <el-descriptions-item label="预警描述" :span="2">{{ currentAlert.description }}</el-descriptions-item>
        <el-descriptions-item label="处理备注" :span="2">
          <el-input v-model="processRemark" type="textarea" :rows="2" placeholder="请输入处理备注" />
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="confirmProcess">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { alertApi } from '@/api/alert'

const loading = ref(false)
const filterLevel = ref('')
const filterStatus = ref('')
const detailVisible = ref(false)
const currentAlert = ref(null)
const processRemark = ref('')
const page = reactive({ current: 1, size: 10 })
const total = ref(0)

const statusLabelMap = { pending: '未处理', processing: '处理中', handled: '已处理' }
const alertStats = ref([
  { label: '预警总数', value: 0, color: '#1890ff' },
  { label: '高危预警', value: 0, color: '#ff4d4f' },
  { label: '未处理', value: 0, color: '#fa8c16' },
  { label: '已处理', value: 0, color: '#52c41a' }
])
const alerts = ref([])

async function loadAlerts() {
  loading.value = true
  try {
    const res = await alertApi.list({ page: page.current, size: page.size, level: filterLevel.value, status: filterStatus.value })
    alerts.value = res.data?.records || []
    total.value = res.data?.total || 0
    alertStats.value[0].value = total.value
    alertStats.value[1].value = alerts.value.filter(a => a.level === 'high').length
    alertStats.value[2].value = alerts.value.filter(a => a.status === 'pending').length
    alertStats.value[3].value = alerts.value.filter(a => a.status === 'handled').length
  } finally { loading.value = false }
}

function statusLabel(s) { return statusLabelMap[s] || s }
function typeColor(type) {
  return { '跌倒检测': 'danger', '心率异常': 'warning', '血压异常': 'danger', '长时间卧床': 'info', '体温异常': 'warning', '血糖异常': 'warning' }[type] || 'info'
}
function viewAlert(row) { currentAlert.value = row; processRemark.value = ''; detailVisible.value = true }
function handleProcess(row) { currentAlert.value = row; processRemark.value = ''; detailVisible.value = true }
async function confirmProcess() {
  if (currentAlert.value) {
    try {
      await alertApi.process(currentAlert.value.id, processRemark.value)
      ElMessage.success('处理成功')
      detailVisible.value = false
      loadAlerts()
    } catch {}
  }
}

onMounted(loadAlerts)
</script>

<style scoped>
.alert-stat { border-radius: 8px; }
.alert-stat-inner { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.stat-num { font-size: 28px; font-weight: 700; line-height: 1; }
.stat-label { font-size: 12px; color: #888; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
