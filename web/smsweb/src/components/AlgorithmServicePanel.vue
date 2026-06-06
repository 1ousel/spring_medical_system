<template>
  <div class="service-panel">
    <div class="service-info">
      <span class="service-label">Python 算法服务</span>
      <el-tag :type="statusTagType" size="small">{{ statusText }}</el-tag>
      <span v-if="serviceStatus.pid" class="service-meta">PID: {{ serviceStatus.pid }}</span>
      <span v-if="serviceStatus.startedByJava" class="service-meta">由 Java 管理</span>
      <span v-if="speechHint" class="service-meta">{{ speechHint }}</span>
    </div>
    <div class="service-actions">
      <el-button
        type="primary"
        size="small"
        :loading="starting"
        :disabled="serviceStatus.running || starting"
        @click="handleStart"
      >
        启动服务
      </el-button>
      <el-button
        type="danger"
        size="small"
        plain
        :loading="stopping"
        :disabled="!serviceStatus.running || stopping"
        @click="handleStop"
      >
        停止服务
      </el-button>
      <el-button size="small" :loading="refreshing" @click="refreshStatus">刷新状态</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { algorithmApi } from '@/api/algorithm'

const emit = defineEmits(['status-change'])

const serviceStatus = ref({
  running: false,
  httpHealthy: false,
  processAlive: false,
  startedByJava: false,
  pid: null
})
const starting = ref(false)
const stopping = ref(false)
const refreshing = ref(false)

const statusText = computed(() => {
  if (starting.value) return '启动中...'
  if (serviceStatus.value.running) return '运行中'
  return '已停止'
})

const statusTagType = computed(() => {
  if (starting.value) return 'warning'
  return serviceStatus.value.running ? 'success' : 'info'
})

const speechHint = computed(() => {
  if (!serviceStatus.value.running) return ''
  return 'TTS 可用（Edge TTS）'
})

async function refreshStatus() {
  refreshing.value = true
  try {
    const res = await algorithmApi.serviceStatus()
    serviceStatus.value = res.data || {}
    emit('status-change', serviceStatus.value)
  } catch (e) {
    ElMessage.error('获取算法服务状态失败')
  } finally {
    refreshing.value = false
  }
}

async function handleStart() {
  starting.value = true
  try {
    const res = await algorithmApi.startService()
    serviceStatus.value = { ...serviceStatus.value, ...(res.data || {}), running: true }
    emit('status-change', serviceStatus.value)
    ElMessage.success(res.data?.message || '算法服务启动成功')
  } catch (e) {
    ElMessage.error(e.message || '启动算法服务失败')
    await refreshStatus()
  } finally {
    starting.value = false
  }
}

async function handleStop() {
  stopping.value = true
  try {
    const res = await algorithmApi.stopService()
    serviceStatus.value = res.data || { running: false }
    emit('status-change', serviceStatus.value)
    ElMessage.success(res.data?.message || '算法服务已停止')
  } catch (e) {
    ElMessage.error(e.message || '停止算法服务失败')
    await refreshStatus()
  } finally {
    stopping.value = false
  }
}

onMounted(refreshStatus)

defineExpose({ refreshStatus, serviceStatus })
</script>

<style scoped>
.service-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 12px 16px;
  margin-bottom: 16px;
  border: 1px solid rgba(15, 124, 144, 0.12);
  border-radius: 12px;
  background: rgba(15, 181, 195, 0.06);
}
.service-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.service-label {
  font-weight: 600;
  color: #0f1f2b;
}
.service-meta {
  font-size: 12px;
  color: #64748b;
}
.service-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
