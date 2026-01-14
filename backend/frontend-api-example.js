/**
 * 前端API调用示例
 * 使用 uni.request 调用后端接口
 */

// ==================== 配置 ====================

// API基础URL - 根据实际情况修改
const API_BASE_URL = 'http://localhost:8080/api'

// 通用请求封装
const request = (url, method = 'GET', data = null) => {
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${API_BASE_URL}${url}`,
      method,
      data,
      header: {
        'Content-Type': 'application/json'
      },
      success: (res) => {
        if (res.statusCode === 200 && res.data.code === 200) {
          resolve(res.data.data)
        } else {
          uni.showToast({
            title: res.data.message || '请求失败',
            icon: 'none'
          })
          reject(res.data)
        }
      },
      fail: (err) => {
        uni.showToast({
          title: '网络请求失败',
          icon: 'none'
        })
        reject(err)
      }
    })
  })
}

// ==================== 卡片API ====================

/**
 * 获取所有卡片
 */
export const getAllCards = () => {
  return request('/cards', 'GET')
}

/**
 * 根据类型获取卡片
 * @param {string} type - 卡片类型（lizi 或 gezi）
 */
export const getCardByType = (type) => {
  return request(`/cards/${type}`, 'GET')
}

/**
 * 保存或更新卡片
 * @param {Object} cardData - 卡片数据
 */
export const saveCard = (cardData) => {
  return request('/cards', 'POST', cardData)
}

// 使用示例：
/*
// 在页面的saveCardEdit方法中替换本地存储
const saveCardEdit = async () => {
  if (!editingCard.value.name || !editingCard.value.description) {
    uni.showToast({
      title: '请填写完整信息',
      icon: 'none'
    })
    return
  }

  try {
    // 调用后端API保存卡片
    const savedCard = await saveCard({
      type: editingCardType.value,
      name: editingCard.value.name,
      description: editingCard.value.description,
      avatar: editingCard.value.avatar,
      background: editingCard.value.background,
      enableBackground: editingCard.value.enableBackground
    })

    // 更新本地数据
    if (editingCardType.value === 'lizi') {
      liziCard.value = savedCard
    } else if (editingCardType.value === 'gezi') {
      geziCard.value = savedCard
    }

    uni.showToast({
      title: '保存成功',
      icon: 'success'
    })

    closeEditModal()
  } catch (error) {
    console.error('保存失败', error)
  }
}
*/

// ==================== 李子账单API ====================

/**
 * 计算并保存李子账单
 * @param {Object} billData - 账单数据
 */
export const saveLiziBill = (billData) => {
  return request('/lizi-bills', 'POST', billData)
}

/**
 * 获取指定月份的李子账单
 * @param {number} year - 年份
 * @param {number} month - 月份
 */
export const getLiziBillByMonth = (year, month) => {
  return request(`/lizi-bills/${year}/${month}`, 'GET')
}

/**
 * 获取指定年份的所有李子账单
 * @param {number} year - 年份
 */
export const getLiziBillsByYear = (year) => {
  return request(`/lizi-bills/year/${year}`, 'GET')
}

/**
 * 获取最近的李子账单
 * @param {number} limit - 数量限制
 */
export const getRecentLiziBills = (limit = 10) => {
  return request(`/lizi-bills/recent?limit=${limit}`, 'GET')
}

/**
 * 获取所有李子账单
 */
export const getAllLiziBills = () => {
  return request('/lizi-bills', 'GET')
}

/**
 * 删除李子账单
 * @param {number} id - 账单ID
 */
export const deleteLiziBill = (id) => {
  return request(`/lizi-bills/${id}`, 'DELETE')
}

// 使用示例（在 lizi/index.vue 中）：
/*
// 在 calculateBill 方法中替换本地计算
const calculateBill = async () => {
  // 验证表单
  const totalDays = Number(formData.value.totalDays)
  const ownerDays = Number(formData.value.ownerDays)

  const isTotalDaysValid = validateTotalDays(totalDays)
  const isOwnerDaysValid = validateOwnerDays(ownerDays, totalDays)

  errors.value.totalDays = !isTotalDaysValid
  errors.value.ownerDays = !isOwnerDaysValid

  if (!isTotalDaysValid || !isOwnerDaysValid) {
    showResult.value = false
    return
  }

  try {
    uni.showLoading({ title: '计算中...' })

    // 调用后端API计算并保存账单
    const savedBill = await saveLiziBill({
      month: formData.value.month,
      year: new Date().getFullYear(),
      totalDays: totalDays,
      waterBill: parseFloat(formData.value.waterBill) || 0,
      electricBill: parseFloat(formData.value.electricBill) || 0,
      gasBill: parseFloat(formData.value.gasBill) || 0,
      ownerDays: ownerDays
    })

    // 更新结果显示
    result.value = {
      totalAmount: savedBill.totalAmount,
      ownerAmount: savedBill.ownerAmount,
      remainingAmount: savedBill.remainingAmount,
      sisterAmount: savedBill.sisterAmount,
      datouAmount: savedBill.datouAmount
    }

    showResult.value = true
    uni.hideLoading()

    uni.showToast({
      title: '计算成功',
      icon: 'success'
    })
  } catch (error) {
    uni.hideLoading()
    console.error('计算失败', error)
  }
}
*/

// ==================== 鸽子账单API ====================

/**
 * 计算并保存鸽子账单
 * @param {Object} billData - 账单数据
 */
export const saveGeziBill = (billData) => {
  return request('/gezi-bills', 'POST', billData)
}

/**
 * 获取最近的鸽子账单
 * @param {number} limit - 数量限制
 */
export const getRecentGeziBills = (limit = 10) => {
  return request(`/gezi-bills/recent?limit=${limit}`, 'GET')
}

/**
 * 获取所有鸽子账单
 */
export const getAllGeziBills = () => {
  return request('/gezi-bills', 'GET')
}

/**
 * 删除鸽子账单
 * @param {number} id - 账单ID
 */
export const deleteGeziBill = (id) => {
  return request(`/gezi-bills/${id}`, 'DELETE')
}

// 使用示例（在 gezi/index.vue 中）：
/*
const calculateBill = async () => {
  // 验证表单
  if (!formData.value.splitRule) {
    showResult.value = false
    return
  }

  try {
    uni.showLoading({ title: '计算中...' })

    // 调用后端API计算并保存账单
    const savedBill = await saveGeziBill({
      waterBill: parseFloat(formData.value.waterBill) || 0,
      electricBill: parseFloat(formData.value.electricBill) || 0,
      gasBill: parseFloat(formData.value.gasBill) || 0,
      splitRule: formData.value.splitRule
    })

    // 更新结果显示
    result.value = {
      totalAmount: savedBill.totalAmount,
      liziAmount: savedBill.liziAmount,
      geziAmount: savedBill.geziAmount,
      chunfengAmount: savedBill.chunfengAmount,
      chengziAmount: savedBill.chengziAmount
    }

    showResult.value = true
    uni.hideLoading()

    uni.showToast({
      title: '计算成功',
      icon: 'success'
    })
  } catch (error) {
    uni.hideLoading()
    console.error('计算失败', error)
  }
}
*/

// ==================== 加载历史记录示例 ====================

/**
 * 在页面加载时获取卡片配置
 */
/*
onMounted(async () => {
  // 获取系统信息
  uni.getSystemInfo({
    success: (res) => {
      statusBarHeight.value = res.statusBarHeight || 20
    }
  })

  try {
    // 从后端加载李子卡片配置
    const liziCardData = await getCardByType('lizi')
    if (liziCardData) {
      liziCard.value = liziCardData
    }

    // 从后端加载鸽子卡片配置
    const geziCardData = await getCardByType('gezi')
    if (geziCardData) {
      geziCard.value = geziCardData
    }
  } catch (error) {
    console.error('加载卡片配置失败', error)
  }
})
*/

/**
 * 查看历史账单功能示例
 */
/*
// 在页面中添加查看历史按钮
const viewHistory = async () => {
  try {
    uni.showLoading({ title: '加载中...' })

    // 获取最近的10条账单
    const recentBills = await getRecentLiziBills(10)

    uni.hideLoading()

    // 显示历史账单列表（可以跳转到新页面或显示弹窗）
    uni.navigateTo({
      url: '/pages/history/lizi-history?bills=' + encodeURIComponent(JSON.stringify(recentBills))
    })
  } catch (error) {
    uni.hideLoading()
    console.error('加载历史失败', error)
  }
}
*/

// ==================== 完整集成示例 ====================

/**
 * 将 api.js 文件放在 src/api/ 目录下
 * 然后在需要的页面中导入使用
 */

/*
// 在 src/pages/home/home.vue 中
import { getCardByType, saveCard } from '@/api/api.js'

// 在 src/pages/lizi/index.vue 中
import { saveLiziBill, getRecentLiziBills } from '@/api/api.js'

// 在 src/pages/gezi/index.vue 中
import { saveGeziBill, getRecentGeziBills } from '@/api/api.js'
*/

// ==================== 注意事项 ====================

/**
 * 1. 确保后端服务已启动并可访问
 * 2. 修改 API_BASE_URL 为实际的后端地址
 * 3. 如果是真机调试，需要使用电脑的局域网IP，如 http://192.168.1.100:8080/api
 * 4. 小程序需要在微信公众平台配置服务器域名白名单
 * 5. 开发环境可以在微信开发者工具中勾选"不校验合法域名"选项
 */
