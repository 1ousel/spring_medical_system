<template>
  <div class="sidebar">
    <div class="sidebar-logo" :class="{ collapsed }">
      <el-icon size="26" color="#1890ff"><FirstAidKit /></el-icon>
      <Transition name="logo-text">
        <span v-if="!collapsed" class="logo-text">医养结合管理系统</span>
      </Transition>
    </div>
    <el-scrollbar class="sidebar-scroll">
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        :collapse-transition="false"
        background-color="transparent"
        text-color="rgba(255,255,255,0.82)"
        active-text-color="#3dd8c8"
        class="sidebar-menu"
        router
      >
        <template v-for="item in menuItems" :key="item.path">
          <el-sub-menu v-if="item.children" :index="item.path">
            <template #title>
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
              <el-icon><component :is="child.icon" /></el-icon>
              <template #title>{{ child.title }}</template>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'

defineProps({ collapsed: Boolean })

const route = useRoute()
const userStore = useUserStore()
const activeMenu = computed(() => route.path)

const allMenuItems = [
  { path: '/dashboard', title: '首页', icon: 'HomeFilled', roles: ['ADMIN', 'DOCTOR', 'NURSE', 'CAREGIVER'] },
  { path: '/elder', title: '老人档案', icon: 'User', roles: ['ADMIN', 'DOCTOR', 'NURSE', 'CAREGIVER'] },
  { path: '/health', title: '健康预警', icon: 'Warning', roles: ['ADMIN', 'DOCTOR', 'NURSE', 'CAREGIVER'] },
  { path: '/assessment', title: '评估报告', icon: 'Document', roles: ['ADMIN', 'DOCTOR', 'NURSE'] },
  { path: '/keygroup', title: '重点人群', icon: 'StarFilled', roles: ['ADMIN', 'DOCTOR', 'NURSE'] },
  { path: '/statistics', title: '报表统计', icon: 'DataAnalysis', roles: ['ADMIN', 'DOCTOR', 'NURSE'] },
  { path: '/device', title: '设备管理', icon: 'Monitor', roles: ['ADMIN', 'DOCTOR'] },
  { path: '/bigdata', title: '大数据决策分析', icon: 'TrendCharts', roles: ['ADMIN', 'DOCTOR'] },
  { path: '/ai-chat', title: '对话大模型', icon: 'ChatDotRound', roles: ['ADMIN', 'DOCTOR', 'NURSE'] },
  {
    path: '/algorithm', title: '算法模块', icon: 'Cpu', roles: ['ADMIN', 'DOCTOR', 'NURSE'],
    children: [
      { path: '/algorithm/detection', title: '目标检测', icon: 'Aim', roles: ['ADMIN', 'DOCTOR'] },
      { path: '/algorithm/classify', title: '图像分类', icon: 'PictureFilled', roles: ['ADMIN', 'DOCTOR'] },
      { path: '/algorithm/tts', title: 'TTS语音合成', icon: 'Headset', roles: ['ADMIN', 'DOCTOR', 'NURSE'] },
      { path: '/algorithm/segmentation', title: '图像分割', icon: 'Grid', roles: ['ADMIN', 'DOCTOR'] }
    ]
  },
  { path: '/elder-account', title: '老人账户管理', icon: 'Avatar', roles: ['ADMIN'] },
  { path: '/doctor-account', title: '医生账户管理', icon: 'Briefcase', roles: ['ADMIN'] },
  { path: '/user-account', title: '用户账号管理', icon: 'Setting', roles: ['ADMIN'] }
]

const menuItems = computed(() => {
  const role = userStore.role
  return allMenuItems
    .filter(item => item.roles.includes(role))
    .map(item => {
      if (item.children) {
        return { ...item, children: item.children.filter(c => c.roles.includes(role)) }
      }
      return item
    })
    .filter(item => !item.children || item.children.length > 0)
})
</script>

<style scoped>
.sidebar { height: 100%; display: flex; flex-direction: column; }
.sidebar-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
  overflow: hidden;
  flex-shrink: 0;
}
.sidebar-logo.collapsed { padding: 0; justify-content: center; }
.logo-text {
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  letter-spacing: 1px;
  text-shadow: 0 8px 24px rgba(15, 181, 195, 0.45);
}
.logo-text-enter-active, .logo-text-leave-active { transition: opacity 0.2s; }
.logo-text-enter-from, .logo-text-leave-to { opacity: 0; }
.sidebar-scroll { flex: 1; overflow: hidden; background: radial-gradient(circle at 15% 20%, rgba(255,255,255,0.06), transparent 45%), linear-gradient(180deg, rgba(15,47,70,0.9) 0%, rgba(15,124,144,0.88) 100%); }
.sidebar-menu {
  border-right: none;
  background: transparent !important;
  padding: 8px 6px 12px;
}
:deep(.el-menu-item), :deep(.el-sub-menu__title) {
  border-radius: 10px;
  margin: 2px 0;
}
:deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(61, 216, 200, 0.35), rgba(15, 181, 195, 0.55)) !important;
  box-shadow: 0 10px 30px rgba(15, 181, 195, 0.18);
  color: #fff !important;
}
:deep(.el-menu-item:hover), :deep(.el-sub-menu__title:hover) {
  background-color: rgba(255,255,255,0.08) !important;
  color: #fff !important;
}
:deep(.el-menu-item.is-active .el-icon), :deep(.el-menu-item.is-active) { color: #fff !important; }
</style>
