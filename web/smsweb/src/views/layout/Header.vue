<template>
  <div class="header">
    <div class="header-left">
      <el-button text @click="$emit('toggle')" class="collapse-btn">
        <el-icon size="20"><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
      </el-button>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="header-right">
      <el-badge :value="alertCount" class="alert-badge">
        <el-button text circle @click="$router.push('/health')">
          <el-icon size="20"><Bell /></el-icon>
        </el-button>
      </el-badge>
      <el-divider direction="vertical" />
      <el-dropdown @command="handleCommand" trigger="click">
        <div class="user-info">
          <el-avatar size="32" :style="{ background: '#1890ff' }">{{ avatarText }}</el-avatar>
          <span class="username">{{ userStore.realName || userStore.username }}</span>
          <el-icon size="14" style="color:#999"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><UserFilled /></el-icon> 个人中心
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon> 退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'

defineProps({ collapsed: Boolean })
defineEmits(['toggle'])

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const alertCount = 5

const currentTitle = computed(() => route.meta?.title || '')
const avatarText = computed(() => (userStore.realName || userStore.username || 'U').charAt(0).toUpperCase())

function handleCommand(cmd) {
  if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' }).then(() => {
      userStore.logout()
      router.push('/login')
    })
  }
}
</script>

<style scoped>
.header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px 0 8px;
}
.header-left { display: flex; align-items: center; gap: 12px; }
.collapse-btn { padding: 8px; }
.header-right { display: flex; align-items: center; gap: 12px; }
.alert-badge :deep(.el-badge__content) { top: 4px; right: 4px; }
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 10px;
  background: rgba(15, 124, 144, 0.08);
  transition: all 0.25s ease;
}
.user-info:hover {
  background: rgba(15, 124, 144, 0.14);
  box-shadow: 0 10px 26px rgba(15, 124, 144, 0.15);
}
.username { font-size: 14px; color: #0f1f2b; font-weight: 600; }
</style>
