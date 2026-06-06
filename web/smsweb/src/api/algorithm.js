import request from '@/utils/request'

export const algorithmApi = {
  detect: (payload) => request.post('/algorithm/detect', payload, { timeout: 60000 }),
  classify: (payload) => request.post('/algorithm/image-classify', payload),
  segment: (payload) => request.post('/algorithm/segment', payload),
  tts: (payload) => request.post('/algorithm/tts', payload, { timeout: 120000 }),
  serviceStatus: () => request.get('/algorithm/service/status'),
  startService: () => request.post('/algorithm/service/start', {}, { timeout: 200000 }),
  stopService: () => request.post('/algorithm/service/stop', {}, { timeout: 30000 })
}
