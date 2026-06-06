<template>
  <div class="page-container">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="never" class="profile-card">
          <div style="text-align:center;padding:20px 0">
            <el-avatar :size="88" :style="{ background: '#1890ff', fontSize: '36px', marginBottom: '12px' }">
              {{ avatarText }}
            </el-avatar>
            <div class="p-name">{{ userStore.realName }}</div>
            <el-tag type="primary" style="margin-top:8px">{{ roleLabel(userStore.role) }}</el-tag>
            <el-divider />
            <el-descriptions :column="1" size="small">
              <el-descriptions-item label="账号">{{ userStore.username }}</el-descriptions-item>
              <el-descriptions-item label="部门">{{ userStore.userInfo?.dept || '-' }}</el-descriptions-item>
              <el-descriptions-item label="上次登录">{{ userStore.userInfo?.lastLoginTime || '-' }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card shadow="never">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="基本信息" name="info">
              <el-form ref="infoFormRef" :model="infoForm" label-width="100px" style="max-width:500px;margin-top:12px">
                <el-form-item label="真实姓名"><el-input v-model="infoForm.realName" /></el-form-item>
                <el-form-item label="手机号"><el-input v-model="infoForm.phone" /></el-form-item>
                <el-form-item label="邮箱"><el-input v-model="infoForm.email" /></el-form-item>
                <el-form-item label="所在部门"><el-input v-model="infoForm.dept" /></el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="saveInfo">保存修改</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
            <el-tab-pane label="修改密码" name="password">
              <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px" style="max-width:500px;margin-top:12px">
                <el-form-item label="当前密码" prop="oldPwd">
                  <el-input v-model="pwdForm.oldPwd" type="password" show-password />
                </el-form-item>
                <el-form-item label="新密码" prop="newPwd">
                  <el-input v-model="pwdForm.newPwd" type="password" show-password />
                  <el-progress v-if="pwdForm.newPwd" :percentage="pwdStrength" :stroke-width="6"
                    :color="pwdStrength < 40 ? '#ff4d4f' : pwdStrength < 70 ? '#fa8c16' : '#52c41a'"
                    :show-text="false" style="margin-top:6px" />
                </el-form-item>
                <el-form-item label="确认密码" prop="confirmPwd">
                  <el-input v-model="pwdForm.confirmPwd" type="password" show-password />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="changePwd">修改密码</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
            <el-tab-pane label="操作日志" name="log">
              <el-timeline style="margin-top:12px">
                <el-timeline-item v-for="log in operationLogs" :key="log.id" :timestamp="log.time" placement="top" type="primary">
                  {{ log.action }}
                </el-timeline-item>
              </el-timeline>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { userApi } from '@/api/user'
import { setUser } from '@/utils/auth'

const userStore = useUserStore()
const activeTab = ref('info')
const infoFormRef = ref()
const pwdFormRef = ref()

const avatarText = computed(() => (userStore.realName || userStore.username || 'U').charAt(0).toUpperCase())
function roleLabel(role) {
  return { ADMIN: '系统管理员', DOCTOR: '医生', NURSE: '护士', CAREGIVER: '护工' }[role] || role || '-'
}

const infoForm = reactive({
  realName: userStore.realName || '',
  phone: '',
  email: '',
  dept: userStore.userInfo?.dept || ''
})

const pwdForm = reactive({ oldPwd: '', newPwd: '', confirmPwd: '' })
const pwdStrength = computed(() => {
  const p = pwdForm.newPwd
  if (!p) return 0
  let score = 0
  if (p.length >= 8) score += 25
  if (/[A-Z]/.test(p)) score += 25
  if (/[0-9]/.test(p)) score += 25
  if (/[^a-zA-Z0-9]/.test(p)) score += 25
  return score
})

const pwdRules = {
  oldPwd: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPwd: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  confirmPwd: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: (rule, val, cb) => val === pwdForm.newPwd ? cb() : cb(new Error('两次密码不一致')), trigger: 'blur' }
  ]
}

const operationLogs = ref([])

async function loadUserInfo() {
  try {
    const id = userStore.userInfo?.id
    if (!id) return
    const res = await userApi.getById(id)
    const u = res.data || {}
    infoForm.realName = u.realName || infoForm.realName
    infoForm.phone = u.phone || ''
    infoForm.email = u.email || ''
    infoForm.dept = u.dept || infoForm.dept
    userStore.userInfo = { ...userStore.userInfo, ...u }
    setUser(userStore.userInfo)
    operationLogs.value = [{ id: 1, action: '最后登录', time: u.lastLoginTime || '-' }]
  } catch {}
}

async function saveInfo() {
  const valid = await infoFormRef.value?.validate().catch(() => false)
  if (valid === false) return
  try {
    const id = userStore.userInfo?.id
    await userApi.update(id, { realName: infoForm.realName, phone: infoForm.phone, email: infoForm.email, dept: infoForm.dept })
    userStore.userInfo = { ...userStore.userInfo, realName: infoForm.realName, phone: infoForm.phone, email: infoForm.email, dept: infoForm.dept }
    setUser(userStore.userInfo)
    ElMessage.success('个人信息已更新')
  } catch {}
}

async function changePwd() {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    const id = userStore.userInfo?.id
    await userApi.changePassword(id, { oldPwd: pwdForm.oldPwd, newPwd: pwdForm.newPwd })
    ElMessage.success('密码修改成功，请重新登录')
    Object.assign(pwdForm, { oldPwd: '', newPwd: '', confirmPwd: '' })
    setTimeout(() => { userStore.logout(); window.location.href = '/login' }, 1500)
  } catch {}
}

onMounted(loadUserInfo)
</script>

<style scoped>
.p-name { font-size: 20px; font-weight: 700; margin-top: 12px; }
</style>
