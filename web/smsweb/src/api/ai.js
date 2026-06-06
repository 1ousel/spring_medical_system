import request from '@/utils/request'

export const aiApi = {
  chat: (data) => request.post('/ai/chat', data, { timeout: 60000 })
}
