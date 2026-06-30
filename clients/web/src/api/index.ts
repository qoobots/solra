import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

let isRefreshing = false
let refreshSubscribers: ((token: string) => void)[] = []

function onRefreshed(token: string) {
  refreshSubscribers.forEach(cb => cb(token))
  refreshSubscribers = []
}

function addRefreshSubscriber(cb: (token: string) => void) {
  refreshSubscribers.push(cb)
}

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('solra_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (res) => res.data,
  async (err) => {
    const originalRequest = err.config

    if (err.response?.status === 401 && !originalRequest._retry) {
      const refreshToken = localStorage.getItem('solra_refresh_token')
      if (refreshToken && !isRefreshing) {
        originalRequest._retry = true
        isRefreshing = true
        try {
          const res = await axios.post('/api/auth/v1/refresh', {
            refreshToken,
          })
          const newToken = res.data.accessToken
          localStorage.setItem('solra_token', newToken)
          if (res.data.refreshToken) {
            localStorage.setItem('solra_refresh_token', res.data.refreshToken)
          }
          onRefreshed(newToken)
          isRefreshing = false
          originalRequest.headers.Authorization = `Bearer ${newToken}`
          return api(originalRequest)
        } catch {
          isRefreshing = false
          localStorage.removeItem('solra_token')
          localStorage.removeItem('solra_refresh_token')
          window.location.hash = '#/login'
          return Promise.reject(err)
        }
      } else if (isRefreshing) {
        return new Promise((resolve) => {
          addRefreshSubscriber((token: string) => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            resolve(api(originalRequest))
          })
        })
      } else {
        localStorage.removeItem('solra_token')
        localStorage.removeItem('solra_refresh_token')
        window.location.hash = '#/login'
      }
    }
    return Promise.reject(err)
  },
)

export default api
