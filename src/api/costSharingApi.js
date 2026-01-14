/**
 * 前端API服务封装
 * 建议将此文件放在 src/api/costSharingApi.js
 */

// API基础URL - 根据环境配置
const getBaseUrl = () => {
  // 开发环境
  if (process.env.NODE_ENV === 'development') {
    return 'http://localhost:8080/api'
  }
  // 生产环境 - 修改为你的实际域名
  return 'https://your-domain.com/api'
}

const API_BASE_URL = getBaseUrl()

/**
 * 通用请求方法
 */
const request = (url, method = 'GET', data = null) => {
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
    getByMonth: (year, month) => request(`/lizi-bills/${year}/${month}`),
    getByYear: (year) => request(`/lizi-bills/year/${year}`),
    getRecent: (limit = 10) => request(`/lizi-bills/recent?limit=${limit}`),
    getAll: () => request('/lizi-bills'),
    delete: (id) => request(`/lizi-bills/${id}`, 'DELETE')
  },

  // 鸽子账单相关
  geziBill: {
    save: (data) => request('/gezi-bills', 'POST', data),
    getRecent: (limit = 10) => request(`/gezi-bills/recent?limit=${limit}`),
    getAll: () => request('/gezi-bills'),
    delete: (id) => request(`/gezi-bills/${id}`, 'DELETE')
  }
}

/**
 * 使用示例：
 *
 * // 在页面中导入
 * import api from '@/api/costSharingApi.js'
 *
 * // 保存卡片
 * const card = await api.card.save({
 *   type: 'lizi',
 *   name: '李子的分账计算器',
 *   description: '分账让生活更简单',
 *   avatar: '🍐',
 *   background: '',
 *   enableBackground: true
 * })
 *
 * // 保存李子账单
 * const bill = await api.liziBill.save({
 *   month: 1,
 *   year: 2024,
 *   totalDays: 31,
 *   waterBill: 100.50,
 *   electricBill: 200.75,
 *   gasBill: 80.25,
 *   ownerDays: 15
 * })
 *
 * // 获取历史账单
 * const recentBills = await api.liziBill.getRecent(10)
 */
