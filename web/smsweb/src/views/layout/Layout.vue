<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="layout-aside">
      <Sidebar :collapsed="isCollapsed" />
    </el-aside>
    <el-container class="layout-main">
      <el-header class="layout-header">
        <Header :collapsed="isCollapsed" @toggle="isCollapsed = !isCollapsed" />
      </el-header>
      <el-main class="layout-content">
        <RouterView v-slot="{ Component }">
          <Transition name="fade" mode="out-in">
            <component :is="Component" />
          </Transition>
        </RouterView>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'

const isCollapsed = ref(false)
</script>

<style scoped>
.layout-container {
  height: 100vh;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(15, 181, 195, 0.08), transparent 40%),
    linear-gradient(200deg, rgba(63, 216, 200, 0.06), transparent 42%),
    #f4fbff;
}
.layout-aside {
  background: radial-gradient(circle at 20% 20%, rgba(255, 255, 255, 0.08), transparent 50%),
    linear-gradient(180deg, #0f2f46 0%, #0f7c90 100%);
  transition: width 0.25s ease;
  overflow: hidden;
  flex-shrink: 0;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 10px 0 40px rgba(12, 47, 70, 0.2);
}
.layout-main {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.layout-header {
  height: 60px;
  padding: 0;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid rgba(15, 124, 144, 0.08);
  box-shadow: 0 8px 26px rgba(15, 124, 144, 0.1);
  backdrop-filter: blur(10px);
  flex-shrink: 0;
}
.layout-content {
  flex: 1;
  overflow-y: auto;
  background: radial-gradient(circle at 70% 20%, rgba(63, 216, 200, 0.05), transparent 35%),
    radial-gradient(circle at 10% 40%, rgba(15, 124, 144, 0.06), transparent 30%),
    linear-gradient(180deg, rgba(245, 251, 255, 0.9) 0%, rgba(235, 246, 249, 0.95) 100%);
  padding: 20px;
}
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
