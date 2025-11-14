/**
 * 费用计算逻辑
 */
import { CONFIG } from './config.js'

/**
 * 计算费用分摊
 * @param {Object} formData - 表单数据
 * @returns {Object} 计算结果
 */
export function calculateCostSharing(formData) {
  const { totalDays, ownerDays, waterBill, electricBill, gasBill } = formData
  const { OWNER_RATIO_DIVISOR } = CONFIG.CALCULATION
  
  // 计算总费用
  const totalAmount = (waterBill || 0) + (electricBill || 0) + (gasBill || 0)
  
  // 计算房主应承担的费用
  const ownerRatio = ownerDays / totalDays
  const ownerAmount = (totalAmount * ownerRatio) / OWNER_RATIO_DIVISOR
  
  // 计算剩余费用
  const remainingAmount = totalAmount - ownerAmount
  
  // 计算剩余费用的分摊（谢林珠和张锦豪均摊）
  const sisterAmount = remainingAmount / 2
  const datouAmount = remainingAmount / 2
  
  return {
    totalAmount,
    ownerAmount,
    remainingAmount,
    sisterAmount,
    datouAmount
  }
}

