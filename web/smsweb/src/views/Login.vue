<template>
  <div class="login-container">
    <div class="login-bg">
      <div class="login-bg-circle c1"></div>
      <div class="login-bg-circle c2"></div>
      <div class="login-bg-circle c3"></div>
      <div class="login-bg-circle c4"></div>
    </div>
    <div class="login-particles">
      <span v-for="n in 20" :key="n" class="particle" :style="particleStyle(n)"></span>
    </div>
    <div class="login-box" :class="{ 'show': boxVisible }">
      <div class="login-left">
        <div class="brand">
          <div class="brand-icon">
            <el-icon size="52" color="#fff"><FirstAidKit /></el-icon>
          </div>
          <h1>医养结合</h1>
          <p>云端后台管理系统</p>
        </div>
        <ul class="features">
          <li v-for="(feat, idx) in features" :key="idx" :style="{ animationDelay: `${0.8 + idx * 0.15}s` }">
            <span class="feat-icon"><el-icon><Check /></el-icon></span>
            <span>{{ feat }}</span>
          </li>
        </ul>
        <div class="login-left-footer">
          <span>Powered by Spring Boot & Vue 3</span>
        </div>
      </div>
      <div class="login-right">
        <div class="login-form-header">
          <h2>欢迎登录</h2>
          <p>Medical Care Cloud Management System</p>
        </div>
        <el-form ref="formRef" :model="form" :rules="rules" class="login-form" @keyup.enter="handleLogin">
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              size="large"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>
          <div class="login-options">
            <el-checkbox v-model="rememberMe">记住我</el-checkbox>
          </div>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            <span v-if="!loading">登 录</span>
            <span v-else>正在登录...</span>
          </el-button>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const rememberMe = ref(false)
const boxVisible = ref(false)

const features = [
  '老人健康档案全面管理',
  '实时智能健康预警监控',
  '多维度评估报告分析',
  '重点人群精准干预'
]

const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }]
}

function particleStyle(n) {
  const left = Math.random() * 100
  const delay = Math.random() * 8
  const duration = 6 + Math.random() * 6
  const size = 2 + Math.random() * 4
  return {
    left: `${left}%`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`,
    width: `${size}px`,
    height: `${size}px`
  }
}

async function handleLogin() {
  await formRef.value.validate(async valid => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login({ username: form.username, password: form.password })
      ElMessage.success('登录成功，正在跳转...')
      const redirect = route.query.redirect || '/dashboard'
      router.push(redirect)
    } catch (err) {
      ElMessage.error(err?.message || '用户名或密码错误')
    } finally {
      loading.value = false
    }
  })
}

onMounted(() => {
  setTimeout(() => { boxVisible.value = true }, 100)
})
</script>

<style scoped>
.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e8f8fb 0%, #d6f3f8 45%, #c6edf3 100%);
  position: relative;
  overflow: hidden;
}

.login-bg { position: absolute; inset: 0; pointer-events: none; }
.login-bg-circle {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(15, 181, 195, 0.12), transparent 70%);
  animation: float 12s ease-in-out infinite;
}
.c1 { width: 620px; height: 620px; top: -220px; left: -150px; animation-delay: 0s; }
.c2 { width: 460px; height: 460px; bottom: -140px; right: 60px; animation-delay: -3s; }
.c3 { width: 260px; height: 260px; top: 32%; right: -40px; animation-delay: -6s; }
.c4 { width: 320px; height: 320px; bottom: 18%; left: 12%; animation-delay: -9s; }

@keyframes float {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-30px) scale(1.05); }
}

.login-particles { position: absolute; inset: 0; pointer-events: none; overflow: hidden; }
.particle {
  position: absolute;
  bottom: -10px;
  border-radius: 50%;
  background: rgba(255,255,255,0.3);
  animation: rise linear infinite;
}
@keyframes rise {
  0% { transform: translateY(0) scale(1); opacity: 0; }
  10% { opacity: 1; }
  90% { opacity: 0.5; }
  100% { transform: translateY(-100vh) scale(0.5); opacity: 0; }
}

.login-box {
  display: flex;
  width: 920px;
  min-height: 540px;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 26px 68px rgba(12, 125, 150, 0.22), 0 0 0 1px rgba(15, 124, 144, 0.08);
  position: relative;
  z-index: 1;
  opacity: 0;
  transform: translateY(30px) scale(0.97);
  transition: all 0.7s cubic-bezier(0.16, 1, 0.3, 1);
}
.login-box.show {
  opacity: 1;
  transform: translateY(0) scale(1);
}

.login-left {
  width: 400px;
  background: linear-gradient(165deg, rgba(15, 181, 195, 0.22) 0%, rgba(15, 124, 144, 0.55) 100%);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255,255,255,0.16);
  border-right: none;
  border-radius: 20px 0 0 20px;
  padding: 52px 40px 32px;
  display: flex;
  flex-direction: column;
  color: #fff;
  position: relative;
  overflow: hidden;
}
.login-left::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.03'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
  pointer-events: none;
}

.brand { text-align: center; margin-bottom: 36px; }
.brand-icon {
  width: 80px; height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0fb5c3, #0f7c90);
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 16px;
  box-shadow: 0 12px 32px rgba(15, 124, 144, 0.35);
  animation: pulse-icon 3s ease-in-out infinite;
}
@keyframes pulse-icon {
  0%, 100% { box-shadow: 0 10px 26px rgba(15, 181, 195, 0.42); }
  50% { box-shadow: 0 10px 42px rgba(15, 181, 195, 0.6); }
}
.brand h1 { font-size: 30px; font-weight: 700; margin: 0 0 4px; letter-spacing: 6px; }
.brand p { font-size: 13px; opacity: 0.7; letter-spacing: 3px; }

.features { list-style: none; display: flex; flex-direction: column; gap: 16px; flex: 1; padding: 0; }
.features li {
  display: flex; align-items: center; gap: 12px;
  font-size: 14px; opacity: 0;
  animation: fadeInLeft 0.5s ease forwards;
}
@keyframes fadeInLeft {
  from { opacity: 0; transform: translateX(-20px); }
  to { opacity: 0.9; transform: translateX(0); }
}
.feat-icon {
  width: 28px; height: 28px;
  border-radius: 50%;
  background: rgba(61, 216, 200, 0.2);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.feat-icon .el-icon { color: #0fb5c3; font-size: 14px; }

.login-left-footer {
  font-size: 11px; opacity: 0.4; text-align: center; padding-top: 16px;
  border-top: 1px solid rgba(255,255,255,0.1);
}

.login-right {
  flex: 1;
  background: #fff;
  padding: 52px 48px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.login-form-header { margin-bottom: 36px; }
.login-form-header h2 { font-size: 28px; font-weight: 700; color: #0f1f2b; margin: 0; }
.login-form-header p { color: #5b707e; margin-top: 6px; font-size: 13px; letter-spacing: 0.5px; }

.login-form .el-form-item { margin-bottom: 22px; }
.login-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 4px 12px;
  box-shadow: 0 0 0 1px rgba(15, 124, 144, 0.18) inset;
  transition: all 0.3s;
}
.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #0fb5c3 inset;
}
.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(15,181,195,0.2), 0 0 0 1px #0fb5c3 inset;
}

.login-options { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  letter-spacing: 6px;
  border-radius: 10px;
  background: linear-gradient(135deg, #0fb5c3, #0f7c90);
  border: none;
  transition: all 0.3s;
}
.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(15, 124, 144, 0.3);
}
.login-btn:active { transform: translateY(0); }
</style>
