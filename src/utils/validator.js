/**
 * 表单验证逻辑
 */
import { CONFIG } from './config.js'

/**
 * 验证总天数
 * @param {number} days - 天数
 * @returns {boolean} 是否有效
 */
export function validateTotalDays(days) {
  const { MIN_DAYS, MAX_DAYS } = CONFIG.VALIDATION
  return Number.isInteger(days) && days >= MIN_DAYS && days <= MAX_DAYS
}

/**
 * 验证房主居住天数
 * @param {number} ownerDays - 房主居住天数
 * @param {number} totalDays - 总天数
 * @returns {boolean} 是否有效
 */
export function validateOwnerDays(ownerDays, totalDays) {
  if (!Number.isInteger(ownerDays) || ownerDays < 0) {
    return false
  }

  if (!Number.isInteger(totalDays) || totalDays <= 0) {
    return false
  }

  return ownerDays <= totalDays
}

