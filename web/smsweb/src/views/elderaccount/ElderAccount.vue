<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <b>老人账户管理</b>
          <div style="display:flex;gap:8px">
            <el-input v-model="searchKey" placeholder="搜索姓名/账号" clearable style="width:200px" size="small" @change="handleSearch" @clear="handleSearch" />
            <el-button type="primary" size="small" :icon="Plus" @click="openDialog()">新增账户</el-button>
          </div>
        </div>
      </template>
      <el-table :data="accounts" border stripe v-loading="loading">
        <el-table-column prop="name" label="老人姓名" width="100" />
        <el-table-column prop="idCard" label="身份证号" width="160" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="room" label="床位" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column prop="emergencyContact" label="紧急联系人" width="120" />
        <el-table-column label="操作" fixed="right" width="180" align="center">
          <template #default="{ row }">
            <el-button text size="small" type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button text size="small" type="danger" @click="deleteAccount(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size"
          :total="total" layout="total, prev, pager, next" background small @change="loadList" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editRow ? '编辑账户' : '新增账户'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" label-width="100px" :rules="rules">
        <el-form-item label="老人姓名" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="form.age" :min="60" :max="120" style="width:100%" />
        </el-form-item>
        <el-form-item label="健康状态" prop="healthStatus">
          <el-select v-model="form.healthStatus" style="width:100%">
            <el-option label="良好" value="good" />
            <el-option label="一般" value="normal" />
            <el-option label="较差" value="poor" />
            <el-option label="危重" value="critical" />
          </el-select>
        </el-form-item>
        <el-form-item label="身份证号"><el-input v-model="form.idCard" /></el-form-item>
        <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="床位"><el-input v-model="form.room" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAccount">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { elderApi } from '@/api/elder'

const searchKey = ref('')
const dialogVisible = ref(false)
const editRow = ref(null)
const formRef = ref()
const loading = ref(false)
const page = reactive({ current: 1, size: 10 })
const total = ref(0)

const defaultForm = () => ({
  name: '',
  gender: '男',
  age: 70,
  healthStatus: 'good',
  idCard: '',
  phone: '',
  room: ''
})

const form = reactive(defaultForm())
const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  age: [{ required: true, message: '请输入年龄', trigger: 'blur' }],
  healthStatus: [{ required: true, message: '请选择健康状态', trigger: 'change' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

const accounts = ref([])

function handleSearch() {
  page.current = 1
  loadList()
}

async function loadList() {
  loading.value = true
  try {
    const res = await elderApi.list({ page: page.current, size: page.size, name: searchKey.value })
    accounts.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

function openDialog(row = null) {
  editRow.value = row
  if (row) {
    Object.assign(form, {
      name: row.name,
      gender: row.gender || '男',
      age: row.age ?? 70,
      healthStatus: row.healthStatus || 'good',
      idCard: row.idCard || '',
      phone: row.phone || '',
      room: row.room || ''
    })
  } else {
    Object.assign(form, defaultForm())
  }
  dialogVisible.value = true
}

async function saveAccount() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    if (editRow.value) {
      await elderApi.update(editRow.value.id, { ...editRow.value, ...form })
      ElMessage.success('修改成功')
    } else {
      await elderApi.save({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadList()
  } catch {}
}

function deleteAccount(row) {
  ElMessageBox.confirm(`确定删除 "${row.name}" 的档案？`, '提示', { type: 'warning' })
    .then(() => elderApi.remove(row.id).then(() => { ElMessage.success('删除成功'); loadList() }))
}

onMounted(loadList)
</script>
<style scoped>
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
