<template>
  <div class="page-container">
    <el-row :gutter="12" style="margin-bottom:16px">
      <el-col :span="6" v-for="s in deviceStats" :key="s.label">
        <el-card shadow="hover" class="device-stat">
          <el-icon :size="24" :style="{ color: s.color }"><component :is="s.icon" /></el-icon>
          <div class="d-num" :style="{ color: s.color }">{{ s.value }}</div>
          <div class="d-label">{{ s.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <b>设备列表</b>
          <div style="display:flex;gap:8px">
            <el-select v-model="filterType" clearable placeholder="设备类型" style="width:130px" size="small" @change="loadList">
              <el-option v-for="t in deviceTypes" :key="t" :label="t" :value="t" />
            </el-select>
            <el-select v-model="filterStatus" clearable placeholder="状态" style="width:110px" size="small" @change="loadList">
              <el-option label="正常" value="normal" /><el-option label="离线" value="offline" />
              <el-option label="故障" value="fault" /><el-option label="维护中" value="maintaining" />
            </el-select>
            <el-button type="primary" size="small" :icon="Plus" @click="openDialog()">新增设备</el-button>
          </div>
        </div>
      </template>
      <el-table :data="devices" border stripe v-loading="loading">
        <el-table-column prop="deviceCode" label="设备编号" width="110" />
        <el-table-column prop="name" label="设备名称" width="140" />
        <el-table-column prop="type" label="设备类型" width="120" />
        <el-table-column prop="location" label="安装位置" width="110" />
        <el-table-column prop="bindElder" label="绑定老人" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.bindElder" type="primary" size="small">{{ row.bindElder }}</el-tag>
            <span v-else style="color:#aaa">未绑定</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="运行状态" width="90" align="center">
          <template #default="{ row }">
            <el-badge :type="statusBadge(row.status)" is-dot />
            <span style="margin-left:6px">{{ statusLabel(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="battery" label="电量" width="100" align="center">
          <template #default="{ row }">
            <el-progress :percentage="row.battery" :stroke-width="8" :show-text="false"
              :color="row.battery > 50 ? '#52c41a' : row.battery > 20 ? '#fa8c16' : '#ff4d4f'"
              style="width:60px;display:inline-block" />
            <span style="margin-left:4px;font-size:12px">{{ row.battery }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastSyncTime" label="最后同步" width="160" />
        <el-table-column label="操作" fixed="right" width="160" align="center">
          <template #default="{ row }">
            <el-button text size="small" type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button text size="small" type="warning" @click="bindElder(row)">绑定</el-button>
            <el-button text size="small" type="danger" @click="deleteDevice(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;margin-top:16px">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size"
          :total="total" layout="total, prev, pager, next" background small @change="loadList" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editRow ? '编辑设备' : '新增设备'" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-form-item label="设备名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="设备类型" required>
          <el-select v-model="form.type" style="width:100%">
            <el-option v-for="t in deviceTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备编号"><el-input v-model="form.deviceCode" /></el-form-item>
        <el-form-item label="安装位置"><el-input v-model="form.location" /></el-form-item>
        <el-form-item label="运行状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="正常" value="normal" /><el-option label="离线" value="offline" />
            <el-option label="故障" value="fault" /><el-option label="维护中" value="maintaining" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDevice">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { deviceApi } from '@/api/device'

const filterType = ref('')
const filterStatus = ref('')
const dialogVisible = ref(false)
const editRow = ref(null)
const formRef = ref()
const loading = ref(false)
const form = reactive({ name: '', type: '', deviceCode: '', location: '', status: 'normal' })
const page = reactive({ current: 1, size: 10 })
const total = ref(0)

const deviceTypes = ['智能手环', '摄像头', '血压仪', '血糖仪', '床垫传感器', '门禁感应器']
const statusLabelMap = { normal: '正常', offline: '离线', fault: '故障', maintaining: '维护中' }

const deviceStats = ref([
  { label: '设备总数', value: 0, color: '#1890ff', icon: 'Monitor' },
  { label: '正常运行', value: 0, color: '#52c41a', icon: 'CircleCheck' },
  { label: '离线/故障', value: 0, color: '#ff4d4f', icon: 'WarningFilled' },
  { label: '待维护', value: 0, color: '#fa8c16', icon: 'Tools' }
])
const devices = ref([])

async function loadList() {
  loading.value = true
  try {
    const res = await deviceApi.list({ page: page.current, size: page.size, type: filterType.value, status: filterStatus.value })
    devices.value = res.data?.records || []
    total.value = res.data?.total || 0
    deviceStats.value[0].value = total.value
    deviceStats.value[1].value = devices.value.filter(d => d.status === 'normal').length
    deviceStats.value[2].value = devices.value.filter(d => d.status === 'offline' || d.status === 'fault').length
    deviceStats.value[3].value = devices.value.filter(d => d.status === 'maintaining').length
  } finally { loading.value = false }
}

function statusBadge(s) { return { normal: 'success', offline: 'info', fault: 'danger', maintaining: 'warning' }[s] || 'info' }
function statusLabel(s) { return statusLabelMap[s] || s }

function openDialog(row = null) {
  editRow.value = row
  if (row) Object.assign(form, { name: row.name, type: row.type, deviceCode: row.deviceCode, location: row.location, status: row.status })
  else Object.assign(form, { name: '', type: '', deviceCode: '', location: '', status: 'normal' })
  dialogVisible.value = true
}
async function saveDevice() {
  try {
    if (editRow.value) {
      await deviceApi.update(editRow.value.id, form)
      ElMessage.success('修改成功')
    } else {
      await deviceApi.save(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadList()
  } catch {}
}
function bindElder(row) { ElMessage.info('绑定功能：请在老人档案中操作') }
function deleteDevice(row) {
  ElMessageBox.confirm(`确定删除设备 "${row.name}"？`, '提示', { type: 'warning' })
    .then(() => deviceApi.remove(row.id).then(() => { ElMessage.success('删除成功'); loadList() }))
}

onMounted(loadList)
</script>

<style scoped>
.device-stat :deep(.el-card__body) { display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 16px; }
.d-num { font-size: 26px; font-weight: 700; line-height: 1; }
.d-label { font-size: 12px; color: #888; }
</style>
