/** 微信登录与统一请求。账号身份只由服务端使用登录凭证确认。 */
const API_BASE_URL = 'https://cost.higheq.top/api'
const TOKEN_KEY = 'cost-sharing-session-v1'
let loginPromise = null
let loadingCount = 0
let pendingErrorMessage = ''

function requestBaseUrl() {
  // 发行包固定使用公网；develop 不是“本机模拟器”的可靠标识。
  if (process.env.NODE_ENV !== 'development') return API_BASE_URL
  try {
    const device = typeof wx.getDeviceInfo === 'function' ? wx.getDeviceInfo() : wx.getSystemInfoSync()
    if (device.platform === 'devtools' && wx.getAccountInfoSync()?.miniProgram?.envVersion === 'develop') {
      return 'http://localhost:8080/api'
    }
  } catch (_) { /* 无法确认本机环境时使用公网接口。 */ }
  return API_BASE_URL
}

function networkFailure(nativeError, method, url, baseUrl) {
  const nativeMessage = String(nativeError?.errMsg || 'request:fail')
    .replace(/https?:\/\/[^\s"'<>]+/gi, '[request URL omitted]')
    .replace(/Bearer\s+[^\s]+/gi, 'Bearer [omitted]')
    .slice(0, 300)
  const reason = /not in.*domain|合法域名/i.test(nativeMessage) ? 'domain'
    : /ssl|tls|cert|handshake/i.test(nativeMessage) ? 'tls'
    : /dns|resolve|NAME_NOT_RESOLVED/i.test(nativeMessage) ? 'dns'
    : /timeout|TIMED_OUT/i.test(nativeMessage) ? 'timeout' : 'connection'
  const error = new Error('网络连接失败，请稍后重试')
  error.networkReason = reason
  error.nativeMessage = nativeMessage
  // 不记录请求参数、完整路径、微信 code、会话令牌或邀请令牌。
  const detail = { client: 'network-20261113', reason, nativeMessage, method, baseUrl,
    resource: url.split('?')[0].split('/')[1] || 'unknown' }
  try { console.error('[api:network]', detail) } catch (_) {}
  try { wx.getRealtimeLogManager?.().error('api_network_failure', detail) } catch (_) {}
  return error
}

function transport(url, method = 'GET', data = null, token = '') {
  if (method === 'GET' && data) {
    const params = Object.entries(data).filter(([, value]) => value !== null && value !== undefined && value !== '')
      .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`).join('&')
    if (params) url += `${url.includes('?') ? '&' : '?'}${params}`
    data = null
  }
  return new Promise((resolve, reject) => {
    const baseUrl = requestBaseUrl()
    const header = { 'Content-Type': 'application/json' }
    if (token) header.Authorization = `Bearer ${token}`
    const options = {
      method, data, header,
      success: (res) => {
        if (res.statusCode === 200 && res.data?.code === 200) resolve(res.data.data)
        else {
          const error = new Error(res.data?.message || '请求失败，请稍后重试')
          error.status = res.statusCode === 200 ? res.data?.code : res.statusCode
          reject(error)
        }
      },
      fail: (error) => reject(networkFailure(error, method, url, baseUrl))
    }
    uni.request({ ...options, url: `${baseUrl}${url}` })
  })
}

// 冷启动重新确认当前微信身份，避免开发者工具切换账号时沿用旧缓存。
export function startAccountSession() {
  uni.removeStorageSync(TOKEN_KEY)
}

export function ensureLogin() {
  if (loginPromise) return loginPromise
  const token = uni.getStorageSync(TOKEN_KEY)
  if (token) return Promise.resolve(token)
  loginPromise = (async () => {
    const code = await new Promise((resolve, reject) => {
      wx.login({ success: (res) => res.code ? resolve(res.code) : reject(new Error('微信登录失败，请重试')),
        fail: () => reject(new Error('微信登录失败，请重试')) })
    })
    const result = await transport('/auth/login', 'POST', { code })
    uni.setStorageSync(TOKEN_KEY, result.token)
    return result.token
  })().finally(() => { loginPromise = null })
  return loginPromise
}

async function request(url, method = 'GET', data = null) {
  if (loadingCount++ === 0) uni.showLoading({ title: '加载中…' })
  try {
    const token = await ensureLogin()
    try { return await transport(url, method, data, token) }
    catch (error) {
      if (error.status !== 401) throw error
      // 并发请求只有一次重新登录，避免旧请求清掉刚生成的新会话。
      if (uni.getStorageSync(TOKEN_KEY) === token) uni.removeStorageSync(TOKEN_KEY)
      return await transport(url, method, data, await ensureLogin())
    }
  } catch (error) {
    pendingErrorMessage = error.message || '请求失败，请重试'
    throw error
  } finally {
    if (--loadingCount === 0) {
      uni.hideLoading()
      // 微信的 toast 会关闭 loading，必须先完成 hideLoading 再显示错误。
      if (pendingErrorMessage) {
        const message = pendingErrorMessage
        pendingErrorMessage = ''
        uni.showToast({ title: message, icon: 'none', duration: 2500 })
      }
    }
  }
}

export default {
  auth: {
    me: () => request('/auth/me'),
    rename: (displayName) => request('/auth/me', 'PUT', { displayName }),
    logout: async () => { await request('/auth/session', 'DELETE'); uni.removeStorageSync(TOKEN_KEY) }
  },
  calculator: {
    getAll: () => request('/calculators'),
    getById: (id) => request(`/calculators/${id}`),
    create: (data) => request('/calculators', 'POST', data),
    update: (id, data) => request(`/calculators/${id}`, 'PUT', data),
    delete: (id) => request(`/calculators/${id}`, 'DELETE')
  },
  companion: {
    list: (id) => request(`/calculators/${id}/companions`),
    invite: (id) => request(`/calculators/${id}/invitations`, 'POST'),
    revoke: (id, inviteId) => request(`/calculators/${id}/invitations/${inviteId}`, 'DELETE'),
    remove: (id, accountId) => request(`/calculators/${id}/companions/${accountId}`, 'DELETE'),
    preview: (token) => request(`/invitations/${encodeURIComponent(token)}`),
    accept: (token) => request(`/invitations/${encodeURIComponent(token)}/accept`, 'POST')
  },
  bill: {
    preview: (data) => request('/bills/preview', 'POST', data),
    save: (data) => request('/bills', 'POST', data),
    getPage: (params = {}) => request('/bills', 'GET', params),
    delete: (id) => request(`/bills/${id}`, 'DELETE')
  }
}
