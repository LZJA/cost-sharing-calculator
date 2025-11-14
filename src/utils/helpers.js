/**
 * 工具函数
 */
import { CONFIG } from './config.js'

/**
 * 获取上一个月
 * @returns {Object} { month: number, year: number }
 */
export function getLastMonth() {
  const now = new Date()
  const currentMonth = now.getMonth() // 0-11
  let lastMonthValue
  let year = now.getFullYear()
  
  if (currentMonth === 0) {
    lastMonthValue = 12
    year = year - 1
  } else {
    lastMonthValue = currentMonth
  }
  
  return { month: lastMonthValue, year }
}

/**
 * 获取指定月份的天数
 * @param {number} month - 月份 (1-12)
 * @param {number} year - 年份
 * @returns {number} 天数
 */
export function getDaysInMonth(month, year) {
  return new Date(year, month, 0).getDate()
}

/**
 * 格式化金额
 * @param {number|string} amount - 金额
 * @param {number} decimals - 小数位数
 * @returns {string} 格式化后的金额字符串
 */
export function formatAmount(amount, decimals = 2) {
  const numAmount = parseFloat(amount)
  if (isNaN(numAmount)) return '0.00'
  return numAmount.toFixed(decimals)
}

/**
 * 获取月份中文名称
 * @param {number} monthIndex - 月份索引 (1-12)
 * @returns {string} 中文月份名称
 */
export function getMonthCn(monthIndex) {
  if (monthIndex && monthIndex >= 1 && monthIndex <= 12) {
    return `${CONFIG.MONTH_CN_MAP[monthIndex]}月`
  }
  return ''
}

