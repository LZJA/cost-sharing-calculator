/**
 * 前端API服务封装 - 使用 uni.request（备用方案）
 * 需要在微信公众平台配置服务器域名
 */

// API基础URL
const API_BASE_URL = 'http://localhost:8080/api'

/**
 * 通用请求方法 - 使用 uni.request
 */
const request = (url, method = 'GET', data = null) => {
  // 如果是GET请求且有参数，手动拼接到URL
  // 注意：虽然uni.request支持data参数，但在某些场景下（如签名、特定后端要求）可能需要手动拼接
  if (method.toUpperCase() === 'GET' && data) {
    const paramsArray = []
    Object.keys(data).forEach(key => {
      if (data[key] !== null && data[key] !== undefined && data[key] !== '') {
        paramsArray.push(`${key}=${encodeURIComponent(data[key])}`)
      }
    })
    if (paramsArray.length > 0) {
      url += (url.indexOf('?') > -1 ? '&' : '?') + paramsArray.join('&')
      // 拼接到URL后，清空data，避免uni.request再次拼接
      data = null
    }
  }

  return new Promise((resolve, reject) => {
    uni.showLoading({ title: '加载中...' })

    uni.request({
      url: `${API_BASE_URL}${url}`,
      method,
      data,
      header: {
        'Content-Type': 'application/json'
      },
      success: (res) => {
        uni.hideLoading()
        console.log('API响应:', res)

        if (res.statusCode === 200 && res.data.code === 200) {
          resolve(res.data.data)
        } else {
          const errorMsg = res.data.message || '请求失败'
          uni.showToast({
            title: errorMsg,
            icon: 'none',
            duration: 2000
          })
          reject(new Error(errorMsg))
        }
      },
      fail: (err) => {
        uni.hideLoading()
        console.error('API请求失败:', err)
        uni.showToast({
          title: '网络请求失败，请检查网络连接',
          icon: 'none',
          duration: 2000
        })
        reject(err)
      }
    })
  })
}

// ==================== 卡片API ====================

export default {
  // 卡片相关
  card: {
    getAll: () => request('/cards'),
    getByType: (type) => request(`/cards/${type}`),
    save: (data) => request('/cards', 'POST', data)
  },

  // 李子账单相关
  liziBill: {
    save: (data) => request('/lizi-bills', 'POST', data),
    getPage: (params = {}) => request('/lizi-bills', 'GET', params),
    delete: (id) => request(`/lizi-bills/${id}`, 'DELETE')
  },

  // 鸽子账单相关
  geziBill: {
    save: (data) => request('/gezi-bills', 'POST', data),
    getPage: (params = {}) => request('/gezi-bills', 'GET', params),
    delete: (id) => request(`/gezi-bills/${id}`, 'DELETE')
  }
}
