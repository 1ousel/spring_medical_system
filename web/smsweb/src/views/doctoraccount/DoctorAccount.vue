<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <b>医生账户管理</b>
          <div style="display:flex;gap:8px">
            <el-input v-model="keyword" placeholder="搜索姓名/账号" clearable size="small" style="width:180px" @change="loadList" />
            <el-button type="primary" size="small" :icon="Plus" @click="openDialog()">新增医生</el-button>
          </div>
        </div>
      </template>
      <el-table :data="doctors" border stripe v-loading="loading">
        <el-table-column prop="realName" label="真实姓名" width="100" />
        <el-table-column prop="username" label="登录账号" width="120" />
        <el-table-column prop="dept" label="所属科室" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column prop="enabled" label="账户状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="toggleStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="180" align="center">
          <template #default="{ row }">
            <el-button text size="small" type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button text size="small" type="warning" @click="resetPassword(row)">重置密码</el-button>
            <el-button text size="small" type="danger" @click="deleteDoctor(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editRow ? '编辑医生账户' : '新增医生账户'" width="540px" destroy-on-close>
      <el-form ref="formRef" :model="form" label-width="100px" :rules="rules">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="真实姓名" prop="realName"><el-input v-model="form.realName" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="登录账号" prop="username"><el-input v-model="form.username" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属部门">
              <el-input v-model="form.dept" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
          </el-col>
          <el-col :span="24" v-if="!editRow">
            <el-form-item label="初始密码"><el-input v-model="form.password" type="password" show-password /></el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDoctor">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { userApi } from '@/api/user'

const keyword = ref('')
const dialogVisible = ref(false)
const editRow = ref(null)
const formRef = ref()
const loading = ref(false)
const page = reactive({ current: 1, size: 10 })
const total = ref(0)

const form = reactive({ realName: '', username: '', dept: '', phone: '', email: '', password: '', role: 'DOCTOR' })
const rules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '格式不正确', trigger: 'blur' }]
}

const doctors = ref([])

async function loadList() {
  loading.value = true
  try {
    const res = await userApi.list({ page: page.current, size: page.size, role: 'DOCTOR', keyword: keyword.value })
    doctors.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

async function toggleStatus(row) {
  try {
    await userApi.toggle(row.id)
    ElMessage.success(`账户已${row.enabled ? '启用' : '禁用'}`)
  } catch { loadList() }
}
function openDialog(row = null) {
  editRow.value = row
  if (row) Object.assign(form, { realName: row.realName, username: row.username, dept: row.dept, phone: row.phone, email: row.email, password: '', role: 'DOCTOR' })
  else Object.assign(form, { realName: '', username: '', dept: '', phone: '', email: '', password: '', role: 'DOCTOR' })
  dialogVisible.value = true
}
async function saveDoctor() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    if (editRow.value) {
      await userApi.update(editRow.value.id, form)
      ElMessage.success('修改成功')
    } else {
      await userApi.save(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadList()
  } catch {}
}
function resetPassword(row) {
  ElMessageBox.confirm(`确定重置 "${row.realName}" 的密码？`, '提示', { type: 'warning' })
    .then(() => userApi.resetPassword(row.id).then(() => ElMessage.success('密码已重置为：123456')))
}
function deleteDoctor(row) {
  ElMessageBox.confirm(`确定删除医生 "${row.realName}"？`, '提示', { type: 'warning' })
    .then(() => userApi.remove(row.id).then(() => { ElMessage.success('删除成功'); loadList() }))
}

onMounted(loadList)
</script>
<style scoped></style>
