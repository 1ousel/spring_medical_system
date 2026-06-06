import { defineStore } from 'pinia'
import { getToken, setToken, removeToken, getUser, setUser } from '@/utils/auth'
import request from '@/utils/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    userInfo: getUser() || null
  }),
  getters: {
    isLoggedIn: state => !!state.token,
    username: state => state.userInfo?.username || '',
    realName: state => state.userInfo?.realName || '',
    role: state => state.userInfo?.role || ''
  },
  actions: {
    async login(credentials) {
      try {
        const res = await request.post('/auth/login', credentials)
        this.token = res.data.token
        this.userInfo = res.data.userInfo
        setToken(res.data.token)
        setUser(res.data.userInfo)
        return res
      } catch (err) {
        return Promise.reject(err)
      }
    },
    logout() {
      this.token = ''
      this.userInfo = null
      removeToken()
    }
  }
})
