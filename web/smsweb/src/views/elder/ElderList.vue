<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="请输入老人姓名" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="健康状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px">
            <el-option label="良好" value="good" />
            <el-option label="一般" value="normal" />
            <el-option label="较差" value="poor" />
            <el-option label="危重" value="critical" />
          </el-select>
        </el-form-item>
        <el-form-item label="床位">
          <el-input v-model="searchForm.room" placeholder="房间/床位" clearable style="width:120px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top:12px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <b>老人档案列表</b>
          <div>
            <el-button type="primary" :icon="Plus" @click="openDialog()">新增老人</el-button>
            <el-button :icon="Download">导出</el-button>
          </div>
        </div>
      </template>

      <el-table :data="elderList" border stripe v-loading="loading">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column prop="gender" label="性别" width="60" align="center">
          <template #default="{ row }">
            <el-tag :type="row.gender === '男' ? 'primary' : 'danger'" size="small">{{ row.gender }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="70" align="center" />
        <el-table-column prop="room" label="床位" width="100" />
        <el-table-column prop="doctor" label="责任医生" width="100" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="emergencyContact" label="紧急联系人" width="120" />
        <el-table-column prop="checkInDate" label="入住日期" width="110" />
        <el-table-column prop="healthStatus" label="健康状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.healthStatus)" size="small">{{ statusLabel(row.healthStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="160" align="center">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="viewDetail(row)">详情</el-button>
            <el-button size="small" text type="warning" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size"
          :total="total" layout="total, sizes, prev, pager, next" background small @change="loadList" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingRow ? '编辑老人档案' : '新增老人档案'" width="680px" destroy-on-close>
      <el-form ref="dialogFormRef" :model="dialogForm" :rules="dialogRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name"><el-input v-model="dialogForm.name" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="dialogForm.gender">
                <el-radio value="男">男</el-radio>
                <el-radio value="女">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年龄" prop="age"><el-input-number v-model="dialogForm.age" :min="60" :max="120" style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="床位" prop="room"><el-input v-model="dialogForm.room" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone"><el-input v-model="dialogForm.phone" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="紧急联系人"><el-input v-model="dialogForm.emergencyContact" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="责任医生"><el-select v-model="dialogForm.doctor" style="width:100%">
              <el-option v-for="d in doctors" :key="d.id" :label="d.realName" :value="d.realName" />
            </el-select></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="健康状态">
              <el-select v-model="dialogForm.healthStatus" style="width:100%">
                <el-option label="良好" value="good" /><el-option label="一般" value="normal" />
                <el-option label="较差" value="poor" /><el-option label="危重" value="critical" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="既往病史"><el-input v-model="dialogForm.medicalHistory" type="textarea" :rows="2" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注"><el-input v-model="dialogForm.remark" type="textarea" :rows="2" /></el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Download } from '@element-plus/icons-vue'
import { elderApi } from '@/api/elder'
import { userApi } from '@/api/user'

const router = useRouter()
const loading = ref(false)
const dialogVisible = ref(false)
const editingRow = ref(null)
const dialogFormRef = ref()

const searchForm = reactive({ name: '', status: '', room: '' })
const page = reactive({ current: 1, size: 10 })
const total = ref(0)
const elderList = ref([])
const doctors = ref([])

const statusMap = { good: '良好', normal: '一般', poor: '较差', critical: '危重' }
function statusLabel(s) { return statusMap[s] || s }
function statusType(s) { return { good: 'success', normal: 'warning', poor: 'danger', critical: 'danger' }[s] || '' }

const dialogForm = reactive({ name: '', gender: '男', age: 70, room: '', doctor: '', phone: '', emergencyContact: '', healthStatus: 'good', medicalHistory: '', remark: '' })
const dialogRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

async function loadList() {
  loading.value = true
  try {
    const res = await elderApi.list({ page: page.current, size: page.size, name: searchForm.name, healthStatus: searchForm.status, room: searchForm.room })
    elderList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

async function loadDoctors() {
  const res = await userApi.getDoctors()
  doctors.value = res.data || []
}

function handleSearch() { page.current = 1; loadList() }
function handleReset() { Object.assign(searchForm, { name: '', status: '', room: '' }); loadList() }
function viewDetail(row) { router.push(`/elder/${row.id}`) }
function openDialog(row = null) {
  editingRow.value = row
  if (row) Object.assign(dialogForm, { name: row.name, gender: row.gender, age: row.age, room: row.room, doctor: row.doctor, phone: row.phone, emergencyContact: row.emergencyContact, healthStatus: row.healthStatus, medicalHistory: row.medicalHistory, remark: row.remark })
  else Object.assign(dialogForm, { name: '', gender: '男', age: 70, room: '', doctor: '', phone: '', emergencyContact: '', healthStatus: 'good', medicalHistory: '', remark: '' })
  dialogVisible.value = true
}
async function handleSave() {
  const valid = await dialogFormRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    if (editingRow.value) {
      await elderApi.update(editingRow.value.id, dialogForm)
      ElMessage.success('修改成功')
    } else {
      await elderApi.save(dialogForm)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadList()
  } catch {}
}
function handleDelete(row) {
  ElMessageBox.confirm(`确定要删除 "${row.name}" 的档案吗？`, '提示', { type: 'warning' })
    .then(() => elderApi.remove(row.id).then(() => { ElMessage.success('删除成功'); loadList() }))
}

onMounted(() => { loadList(); loadDoctors() })
</script>

<style scoped>
.search-card :deep(.el-card__body) { padding: 16px 20px 0; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
