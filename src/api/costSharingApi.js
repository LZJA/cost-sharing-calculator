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
    getPage: (params = {}) => {
      const { year, month, page = 0, size = 10 } = params;
      let url = `/lizi-bills?page=${page}&size=${size}`;
      if (year) url += `&year=${year}`;
      if (month) url += `&month=${month}`;
      return request(url);
    },
    delete: (id) => request(`/lizi-bills/${id}`, 'DELETE')
  },

  // 鸽子账单相关
  geziBill: {
    save: (data) => request('/gezi-bills', 'POST', data),
    getPage: (params = {}) => {
      const { year, month, page = 0, size = 10 } = params;
      let url = `/gezi-bills?page=${page}&size=${size}`;
      if (year) url += `&year=${year}`;
      if (month) url += `&month=${month}`;
      return request(url);
    },
    delete: (id) => request(`/gezi-bills/${id}`, 'DELETE')
  }
}

/**
 * 使用示例：
 *
 * // 在页面中导入
 * import api from '@/api/costSharingApi.js'
 *
 * // ==================== 卡片相关 ====================
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
 * // 获取所有卡片
 * const cards = await api.card.getAll()
 *
 * // 根据类型获取卡片
 * const liziCard = await api.card.getByType('lizi')
 *
 * // ==================== 李子账单相关 ====================
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
 * // 分页查询账单（推荐使用）
 * const page1 = await api.liziBill.getPage({ page: 0, size: 10 })
 * console.log('账单列表:', page1.content)
 * console.log('总记录数:', page1.totalElements)
 *
 * // 查询2024年的账单
 * const year2024 = await api.liziBill.getPage({ year: 2024, page: 0, size: 10 })
 *
 * // 查询2024年1月的账单
 * const jan2024 = await api.liziBill.getPage({ year: 2024, month: 1, page: 0, size: 10 })
 *
 * // 删除账单
 * await api.liziBill.delete(billId)
 *
 * // ==================== 鸽子账单相关 ====================
 *
 * // 保存鸽子账单
 * const geziBill = await api.geziBill.save({
 *   waterBill: 100.50,
 *   electricBill: 200.75,
 *   gasBill: 80.25,
 *   splitRule: '普通分账',  // 或 '特殊分账'
 *   year: 2024,
 *   month: 1
 * })
 *
 * // 分页查询账单（推荐使用）
 * const geziPage = await api.geziBill.getPage({ page: 0, size: 10 })
 *
 * // 查询2024年的账单
 * const geziYear2024 = await api.geziBill.getPage({ year: 2024, page: 0, size: 10 })
 *
 * // 删除账单
 * await api.geziBill.delete(billId)
 */

