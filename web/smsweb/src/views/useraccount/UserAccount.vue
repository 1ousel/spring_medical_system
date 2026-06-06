<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <b>用户账号管理</b>
          <div style="display:flex;gap:8px">
            <el-input v-model="keyword" placeholder="搜索姓名/账号" clearable size="small" style="width:180px" @change="loadList" />
            <el-button type="primary" size="small" :icon="Plus" @click="openDialog()">新增用户</el-button>
          </div>
        </div>
      </template>
      <el-table :data="users" border stripe v-loading="loading">
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="username" label="账号" width="130" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="roleType(row.role)">{{ roleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dept" label="部门" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column prop="enabled" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="toggleUser(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="160" align="center">
          <template #default="{ row }">
            <el-button text size="small" type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button text size="small" type="warning" @click="resetPwd(row)">重置密码</el-button>
            <el-button text size="small" type="danger" @click="deleteUser(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;margin-top:16px">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size"
          :total="total" layout="total, prev, pager, next" background small @change="loadList" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editRow ? '编辑用户' : '新增用户'" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" label-width="90px" :rules="rules">
        <el-form-item label="真实姓名" prop="realName"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="登录账号" prop="username"><el-input v-model="form.username" :disabled="!!editRow" /></el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width:100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="医生" value="DOCTOR" />
            <el-option label="护士" value="NURSE" />
            <el-option label="护工" value="CAREGIVER" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门"><el-input v-model="form.dept" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item v-if="!editRow" label="初始密码">
          <el-input v-model="form.password" type="password" show-password placeholder="留空则默认为 123456" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { userApi } from '@/api/user'

const dialogVisible = ref(false)
const editRow = ref(null)
const formRef = ref()
const loading = ref(false)
const keyword = ref('')
const form = reactive({ realName: '', username: '', role: '', dept: '', phone: '', email: '', password: '' })
const rules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}
const users = ref([])
const total = ref(0)
const page = reactive({ current: 1, size: 10 })

async function loadList() {
  loading.value = true
  try {
    const res = await userApi.list({ page: page.current, size: page.size, keyword: keyword.value })
    users.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function roleType(r) { return { 'ADMIN': 'danger', 'DOCTOR': 'primary', 'NURSE': 'success', 'CAREGIVER': 'info' }[r] || '' }
function roleLabel(r) { return { 'ADMIN': '管理员', 'DOCTOR': '医生', 'NURSE': '护士', 'CAREGIVER': '护工' }[r] || r }

async function toggleUser(row) {
  try {
    await userApi.toggle(row.id)
    ElMessage.success(`用户已${row.enabled ? '启用' : '禁用'}`)
  } catch { loadList() }
}

function openDialog(row = null) {
  editRow.value = row
  if (row) Object.assign(form, { realName: row.realName, username: row.username, role: row.role, dept: row.dept, phone: row.phone, email: row.email, password: '' })
  else Object.assign(form, { realName: '', username: '', role: '', dept: '', phone: '', email: '', password: '' })
  dialogVisible.value = true
}

async function saveUser() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    if (editRow.value) {
      await userApi.update(editRow.value.id, form)
      ElMessage.success('修改成功')
    } else {
      await userApi.save(form)
      ElMessage.success(form.password ? '新增成功' : '新增成功，初始密码为 123456')
    }
    dialogVisible.value = false
    loadList()
  } catch {}
}

function resetPwd(row) {
  ElMessageBox.confirm(`确定重置 "${row.realName}" 的密码为 123456？`, '提示', { type: 'warning' })
    .then(() => userApi.resetPassword(row.id).then(() => ElMessage.success('密码已重置为：123456')))
}

function deleteUser(row) {
  if (row.username === 'admin') return ElMessage.warning('不能删除超级管理员')
  ElMessageBox.confirm(`确定删除用户 "${row.realName}"？`, '提示', { type: 'warning' })
    .then(() => userApi.remove(row.id).then(() => { ElMessage.success('删除成功'); loadList() }))
}

onMounted(loadList)
</script>
<style scoped></style>
