import request from '@/utils/request'

export const statisticsApi = {
  overview: () => request.get('/statistics/overview')
}
