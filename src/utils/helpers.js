/**
 * 工具函数
 */
import { CONFIG } from './config.js'
import dayjs from 'dayjs'

/**
 * 获取上一个月
 * @returns {Object} { month: number, year: number }
 */
export function getLastMonth() {
  const now = dayjs()
  const currentMonth = now.month() + 1 // dayjs month is 0-11, convert to 1-12
  let lastMonthValue
  let year = now.year()

  if (currentMonth === 1) {
    lastMonthValue = 12
    year = year - 1
  } else {
    lastMonthValue = currentMonth - 1
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
  return dayjs(`${year}-${month}`).daysInMonth()
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

