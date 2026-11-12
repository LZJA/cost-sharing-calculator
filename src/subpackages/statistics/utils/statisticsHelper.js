/**
 * 统计分析工具函数
 * 用于处理账单数据，计算统计指标，生成图表数据
 */

import api from "@/api/costSharingApi.js";
import dayjs from "dayjs";
import { getBillFees } from "@/utils/formula.js";

/**
 * 获取统计数据
 * @param {Number} calculatorId - 计算器 ID
 * @param {Object} filter - { startYear, startMonth, endYear, endMonth }
 * @returns {Promise<Array>} 账单数据数组
 */
export async function fetchStatisticsData(calculatorId, filter) {
  try {
    const result = await api.bill.getPage({
      calculatorId,
      ...filter,
      page: 0,
      size: 1000, // 足够大的数字以获取所有数据
    });

    return result.content || [];
  } catch (error) {
    console.error("获取统计数据失败", error);
    return [];
  }
}

/**
 * 处理账单数据为统计格式
 * @param {Array} bills - 账单数据数组
 * @returns {Object} 处理后的统计数据
 */
export function processBillData(bills) {
  if (!bills || bills.length === 0) {
    return {
      timeline: [],
      feeSeries: [],
      sortedBills: [],
      totalAmounts: [],
      splitData: {},
    };
  }

  // 1. 按年月排序（升序）
  const sortedBills = [...bills].sort((a, b) => {
    if (a.year !== b.year) return a.year - b.year;
    return a.month - b.month;
  });

  // 2. 提取时间序列
  const timeline = sortedBills.map((b) => `${b.year}.${b.month}`);

  // 3. 提取各类费用序列
  const feeRows = sortedBills.map(getBillFees);
  const definitions = new Map();
  feeRows.forEach((fees) => fees.forEach((fee) => definitions.set(fee.key, fee)));
  const feeSeries = [...definitions.values()].map((fee, index) => ({
    key: fee.key,
    name: fee.name,
    color: FEE_COLORS[index % FEE_COLORS.length],
    data: feeRows.map((fees) => Number(fees.find((item) => item.key === fee.key)?.amount) || 0),
  }));
  const totalAmounts = sortedBills.map((b) => b.totalAmount || 0);

  // 4. 按账单里记录的人员汇总分摊数据，名字用当时存下来的
  const splitData = {};
  sortedBills.forEach((bill) => {
    (bill.shares || []).forEach((share) => {
      if (!splitData[share.key]) splitData[share.key] = [];
    });
  });
  Object.keys(splitData).forEach((key) => {
    splitData[key] = sortedBills.map(
      (bill) => (bill.shares || []).find((share) => share.key === key)?.amount || 0
    );
  });

  return {
    timeline,
    feeSeries,
    totalAmounts,
    splitData,
    sortedBills, // 返回排序后的账单数据，用于其他计算
  };
}

/**
 * 计算核心统计指标
 * @param {Array} bills - 账单数据数组
 * @returns {Object} 核心指标
 */
export function calculateCoreMetrics(bills) {
  if (!bills || bills.length === 0) {
    return {
      totalExpense: 0,
      avgMonthly: 0,
      maxMonth: { month: "-", amount: 0 },
      minMonth: { month: "-", amount: 0 },
    };
  }

  const totalExpense = bills.reduce(
    (sum, b) => sum + (b.totalAmount || 0),
    0
  );
  const avgMonthly = totalExpense / bills.length;

  // 找出最高和最低月份
  const maxBill = bills.reduce(
    (max, b) =>
      b.totalAmount > max.totalAmount ? b : max,
    bills[0]
  );
  const minBill = bills.reduce(
    (min, b) =>
      b.totalAmount < min.totalAmount ? b : min,
    bills[0]
  );

  return {
    totalExpense,
    avgMonthly,
    maxMonth: {
      month: `${maxBill.year}.${maxBill.month}`,
      amount: maxBill.totalAmount,
    },
    minMonth: {
      month: `${minBill.year}.${minBill.month}`,
      amount: minBill.totalAmount,
    },
  };
}

/**
 * 计算同比和环比
 * @param {Object} currentData - 当前月数据
 * @param {Object} previousData - 上月数据
 * @param {Object} lastYearData - 去年同月数据
 * @returns {Object} 同比环比数据
 */
export function calculateComparison(currentData, previousData, lastYearData) {
  const calculateRate = (current, previous) => {
    if (!previous || !previous.totalAmount || previous.totalAmount === 0)
      return 0;
    return ((current.totalAmount - previous.totalAmount) /
            previous.totalAmount *
            100
    ).toFixed(1);
  };

  // 环比：与上月对比
  const monthOverMonth = calculateRate(currentData, previousData);

  // 同比：与去年同月对比
  const yearOverYear = calculateRate(currentData, lastYearData);

  return {
    monthOverMonth: parseFloat(monthOverMonth),
    yearOverYear: parseFloat(yearOverYear),
  };
}

/**
 * 统计累计分摊费用和占比
 * @param {Array} bills - 账单数据数组
 * @returns {Array} 分摊统计数组，名字取账单里当时记录的名字
 */
export function calculateSplitStatistics(bills) {
  if (!bills || bills.length === 0) {
    return [];
  }

  const totals = new Map();
  bills.forEach((bill) => {
    (bill.shares || []).forEach((share) => {
      const current = totals.get(share.key) || {
        name: `${share.emoji || ""} ${share.name}`.trim(),
        amount: 0,
      };
      current.amount += share.amount || 0;
      totals.set(share.key, current);
    });
  });

  const grandTotal = [...totals.values()].reduce((sum, item) => sum + item.amount, 0);
  return [...totals.values()].map((item) => ({
    name: item.name,
    amount: item.amount,
    percentage: grandTotal > 0 ? ((item.amount / grandTotal) * 100).toFixed(1) : 0,
  }));
}

/**
 * 计算费用类型占比
 * @param {Array} bills - 账单数据数组
 * @returns {Array} 费用类型占比数组
 */
const FEE_COLORS = ["#a8edea", "#ffd93d", "#6bcf7f", "#c3a6ff", "#ffb785", "#8ab8f5"];

export function calculateCostDistribution(bills) {
  if (!bills || bills.length === 0) {
    return [];
  }

  const totals = new Map();
  bills.forEach((bill) => getBillFees(bill).forEach((fee) => {
    const amount = (totals.get(fee.key)?.value || 0) + (Number(fee.amount) || 0);
    totals.set(fee.key, { key: fee.key, name: `${fee.emoji || "💰"} ${fee.name}`, value: amount });
  }));
  const grandTotal = [...totals.values()].reduce((sum, fee) => sum + fee.value, 0);
  return [...totals.values()].map((fee, index) => ({
    ...fee,
    percentage: grandTotal > 0 ? ((fee.value / grandTotal) * 100).toFixed(1) : 0,
    color: FEE_COLORS[index % FEE_COLORS.length],
  }));
}

/**
 * 获取默认日期筛选参数（最近12个月）
 * @returns {Object} 日期筛选参数
 */
export function getDefaultDateFilter() {
  const now = dayjs();
  const endYear = now.year();
  const endMonth = now.month() + 1;

  const startDate = now.subtract(11, "month");
  const startYear = startDate.year();
  const startMonth = startDate.month() + 1;

  return {
    startYear,
    startMonth,
    endYear,
    endMonth,
  };
}

/**
 * 格式化日期区间文本
 * @param {Object} filter - 日期筛选参数
 * @returns {String} 格式化后的文本
 */
export function formatDateRangeText(filter) {
  if (
    filter.startYear &&
    filter.startMonth &&
    filter.endYear &&
    filter.endMonth
  ) {
    return `${filter.startYear}.${filter.startMonth} - ${filter.endYear}.${filter.endMonth}`;
  }
  return "选择日期区间";
}

/**
 * 根据月份查找账单数据
 * @param {Array} bills - 账单数据数组
 * @param {Number} year - 年份
 * @param {Number} month - 月份
 * @returns {Object|null} 找到的账单数据
 */
export function findBillByMonth(bills, year, month) {
  return bills.find((b) => b.year === year && b.month === month) || null;
}

/**
 * 生成折线图数据
 * @param {Object} processedData - 处理后的数据
 * @returns {Object} 折线图数据
 */
export function generateLineChartData(processedData) {
  return {
    categories: processedData.timeline,
    series: [{ name: "总费用", data: processedData.totalAmounts, color: "#ff6b9d" },
      ...processedData.feeSeries.map((fee) => ({ name: fee.name, data: fee.data, color: fee.color }))],
  };
}

/** 按账单实际记录的费用项生成柱状图。 */
export function generateBarChartData(processedData) {
  return {
    categories: processedData.timeline,
    series: processedData.feeSeries.map((fee) => ({ name: fee.name, data: fee.data, color: fee.color })),
  };
}

/**
 * 生成饼图数据（费用类型占比）
 * @param {Array} distribution - 费用分布数据
 * @returns {Object} 饼图数据
 */
export function generatePieChartData(distribution) {
  return {
    series: distribution.map((item) => ({
      name: item.name,
      data: item.value || 0,
      color: item.color,
    })),
  };
}

/**
 * 生成组合图数据
 * @param {Object} processedData - 处理后的数据
 * @param {Number} avgAmount - 平均金额
 * @returns {Object} 组合图数据
 */
export function generateComboChartData(processedData, avgAmount) {
  const avgLine = new Array(processedData.timeline.length).fill(avgAmount);

  return {
    categories: processedData.timeline,
    series: [
      {
        name: "总费用",
        data: processedData.totalAmounts,
        type: "bar",
        color: "#ff6b9d",
      },
      {
        name: "平均费用",
        data: avgLine,
        type: "line",
        color: "#a8edea",
      },
    ],
  };
}

/**
 * 格式化金额
 * @param {Number} amount - 金额
 * @param {Number} decimals - 小数位数
 * @returns {String} 格式化后的金额字符串
 */
export function formatAmount(amount, decimals = 2) {
  const numAmount = parseFloat(amount);
  if (isNaN(numAmount)) return "0.00";
  return numAmount.toFixed(decimals);
}
