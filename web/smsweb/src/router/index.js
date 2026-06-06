import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn, getUser } from '@/utils/auth'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

NProgress.configure({ showSpinner: false })

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/views/layout/Layout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: '首页', icon: 'HomeFilled', roles: ['ADMIN', 'DOCTOR', 'NURSE', 'CAREGIVER'] }
      },
      {
        path: 'elder',
        name: 'Elder',
        component: () => import('@/views/elder/ElderList.vue'),
        meta: { title: '老人档案', icon: 'User', roles: ['ADMIN', 'DOCTOR', 'NURSE', 'CAREGIVER'] }
      },
      {
        path: 'elder/:id',
        name: 'ElderDetail',
        component: () => import('@/views/elder/ElderDetail.vue'),
        meta: { title: '老人详情', hidden: true, roles: ['ADMIN', 'DOCTOR', 'NURSE', 'CAREGIVER'] }
      },
      {
        path: 'health',
        name: 'HealthAlert',
        component: () => import('@/views/health/HealthAlert.vue'),
        meta: { title: '健康预警', icon: 'Warning', roles: ['ADMIN', 'DOCTOR', 'NURSE', 'CAREGIVER'] }
      },
      {
        path: 'assessment',
        name: 'Assessment',
        component: () => import('@/views/assessment/AssessmentReport.vue'),
        meta: { title: '评估报告', icon: 'Document', roles: ['ADMIN', 'DOCTOR', 'NURSE'] }
      },
      {
        path: 'keygroup',
        name: 'KeyGroup',
        component: () => import('@/views/keygroup/KeyGroup.vue'),
        meta: { title: '重点人群', icon: 'StarFilled', roles: ['ADMIN', 'DOCTOR', 'NURSE'] }
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/statistics/Statistics.vue'),
        meta: { title: '报表统计', icon: 'DataAnalysis', roles: ['ADMIN', 'DOCTOR', 'NURSE'] }
      },
      {
        path: 'device',
        name: 'Device',
        component: () => import('@/views/device/DeviceManage.vue'),
        meta: { title: '设备管理', icon: 'Monitor', roles: ['ADMIN', 'DOCTOR'] }
      },
      {
        path: 'bigdata',
        name: 'BigData',
        component: () => import('@/views/bigdata/BigData.vue'),
        meta: { title: '大数据决策分析', icon: 'TrendCharts', roles: ['ADMIN', 'DOCTOR'] }
      },
      {
        path: 'algorithm/detection',
        name: 'ObjectDetection',
        component: () => import('@/views/algorithm/ObjectDetection.vue'),
        meta: { title: '目标检测', icon: 'Aim', roles: ['ADMIN', 'DOCTOR'], parent: 'algorithm' }
      },
      {
        path: 'algorithm/classify',
        name: 'ImageClassify',
        component: () => import('@/views/algorithm/ImageClassify.vue'),
        meta: { title: '图像分类', icon: 'PictureFilled', roles: ['ADMIN', 'DOCTOR'], parent: 'algorithm' }
      },
      {
        path: 'ai-chat',
        name: 'AiChat',
        component: () => import('@/views/algorithm/AiChat.vue'),
        meta: { title: '对话大模型', icon: 'ChatDotRound', roles: ['ADMIN', 'DOCTOR', 'NURSE'] }
      },
      {
        path: 'algorithm/tts',
        name: 'TextToSpeech',
        component: () => import('@/views/algorithm/TextToSpeech.vue'),
        meta: { title: 'TTS语音合成', icon: 'Headset', roles: ['ADMIN', 'DOCTOR', 'NURSE'] }
      },
      {
        path: 'algorithm/segmentation',
        name: 'ImageSegmentation',
        component: () => import('@/views/algorithm/ImageSegmentation.vue'),
        meta: { title: '图像分割', icon: 'Grid', roles: ['ADMIN', 'DOCTOR'], parent: 'algorithm' }
      },
      {
        path: 'elder-account',
        name: 'ElderAccount',
        component: () => import('@/views/elderaccount/ElderAccount.vue'),
        meta: { title: '老人账户管理', icon: 'Avatar', roles: ['ADMIN'] }
      },
      {
        path: 'doctor-account',
        name: 'DoctorAccount',
        component: () => import('@/views/doctoraccount/DoctorAccount.vue'),
        meta: { title: '医生账户管理', icon: 'Briefcase', roles: ['ADMIN'] }
      },
      {
        path: 'user-account',
        name: 'UserAccount',
        component: () => import('@/views/useraccount/UserAccount.vue'),
        meta: { title: '用户账号管理', icon: 'Setting', roles: ['ADMIN'] }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/Profile.vue'),
        meta: { title: '个人中心', icon: 'UserFilled', hidden: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  NProgress.start()
  document.title = to.meta.title ? `${to.meta.title} - 医养结合管理系统` : '医养结合云端后台管理系统'
  if (to.meta.requiresAuth !== false && !isLoggedIn()) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if (to.path === '/login' && isLoggedIn()) {
    next('/dashboard')
  } else if (to.meta.roles) {
    const user = getUser()
    const role = user?.role || ''
    if (to.meta.roles.includes(role)) {
      next()
    } else {
      next('/dashboard')
    }
  } else {
    next()
  }
})

router.afterEach(() => {
  NProgress.done()
})

export default router
