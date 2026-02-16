/**
 * 统计分析工具函数
 * 用于处理账单数据，计算统计指标，生成图表数据
 */

import api from "@/api/costSharingApi.js";
import dayjs from "dayjs";

/**
 * 获取统计数据
 * @param {String} type - 'lizi' | 'gezi'
 * @param {Object} filter - { startYear, startMonth, endYear, endMonth }
 * @returns {Promise<Array>} 账单数据数组
 */
export async function fetchStatisticsData(type, filter) {
  try {
    const apiMethod = type === "lizi" ? api.liziBill : api.geziBill;

    // 获取所有数据（设置较大的size）
    const result = await apiMethod.getPage({
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
 * @param {String} type - 'lizi' | 'gezi'
 * @returns {Object} 处理后的统计数据
 */
export function processBillData(bills, type) {
  if (!bills || bills.length === 0) {
    return {
      timeline: [],
      waterBills: [],
      electricBills: [],
      gasBills: [],
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
  const waterBills = sortedBills.map((b) => b.waterBill || 0);
  const electricBills = sortedBills.map((b) => b.electricBill || 0);
  const gasBills = sortedBills.map((b) => b.gasBill || 0);
  const totalAmounts = sortedBills.map((b) => b.totalAmount || 0);

  // 4. 根据类型提取分摊数据
  let splitData = {};
  if (type === "lizi") {
    splitData = {
      owner: sortedBills.map((b) => b.ownerAmount || 0),
      sister: sortedBills.map((b) => b.sisterAmount || 0),
      datou: sortedBills.map((b) => b.datouAmount || 0),
    };
  } else {
    splitData = {
      lizi: sortedBills.map((b) => b.liziAmount || 0),
      gezi: sortedBills.map((b) => b.geziAmount || 0),
      chunfeng: sortedBills.map((b) => b.chunfengAmount || 0),
      chengzi: sortedBills.map((b) => b.chengziAmount || 0),
    };
  }

  return {
    timeline,
    waterBills,
    electricBills,
    gasBills,
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
 * @param {String} type - 'lizi' | 'gezi'
 * @returns {Array} 分摊统计数组
 */
export function calculateSplitStatistics(bills, type) {
  if (!bills || bills.length === 0) {
    return [];
  }

  if (type === "lizi") {
    const ownerTotal = bills.reduce((sum, b) => sum + (b.ownerAmount || 0), 0);
    const sisterTotal = bills.reduce((sum, b) => sum + (b.sisterAmount || 0), 0);
    const datouTotal = bills.reduce((sum, b) => sum + (b.datouAmount || 0), 0);
    const grandTotal = ownerTotal + sisterTotal + datouTotal;

    return [
      {
        name: "👤 房主",
        amount: ownerTotal,
        percentage: grandTotal > 0 ? (ownerTotal / grandTotal * 100).toFixed(1) : 0,
      },
      {
        name: "🌸 谢林珠",
        amount: sisterTotal,
        percentage: grandTotal > 0 ? (sisterTotal / grandTotal * 100).toFixed(1) : 0,
      },
      {
        name: "🌻 张锦豪",
        amount: datouTotal,
        percentage: grandTotal > 0 ? (datouTotal / grandTotal * 100).toFixed(1) : 0,
      },
    ];
  } else {
    const liziTotal = bills.reduce((sum, b) => sum + (b.liziAmount || 0), 0);
    const geziTotal = bills.reduce((sum, b) => sum + (b.geziAmount || 0), 0);
    const chunfengTotal = bills.reduce((sum, b) => sum + (b.chunfengAmount || 0), 0);
    const chengziTotal = bills.reduce((sum, b) => sum + (b.chengziAmount || 0), 0);
    const grandTotal = liziTotal + geziTotal + chunfengTotal + chengziTotal;

    return [
      {
        name: "🍐 李子",
        amount: liziTotal,
        percentage: grandTotal > 0 ? (liziTotal / grandTotal * 100).toFixed(1) : 0,
      },
      {
        name: "🕊️ 鸽子",
        amount: geziTotal,
        percentage: grandTotal > 0 ? (geziTotal / grandTotal * 100).toFixed(1) : 0,
      },
      {
        name: "🌸 春风",
        amount: chunfengTotal,
        percentage: grandTotal > 0 ? (chunfengTotal / grandTotal * 100).toFixed(1) : 0,
      },
      {
        name: "🍊 橙子",
        amount: chengziTotal,
        percentage: grandTotal > 0 ? (chengziTotal / grandTotal * 100).toFixed(1) : 0,
      },
    ];
  }
}

/**
 * 计算费用类型占比
 * @param {Array} bills - 账单数据数组
 * @returns {Array} 费用类型占比数组
 */
export function calculateCostDistribution(bills) {
  if (!bills || bills.length === 0) {
    return [];
  }

  const waterTotal = bills.reduce((sum, b) => sum + (b.waterBill || 0), 0);
  const electricTotal = bills.reduce((sum, b) => sum + (b.electricBill || 0), 0);
  const gasTotal = bills.reduce((sum, b) => sum + (b.gasBill || 0), 0);
  const grandTotal = waterTotal + electricTotal + gasTotal;

  return [
    {
      name: "💧 水费",
      value: waterTotal,
      percentage: grandTotal > 0 ? (waterTotal / grandTotal * 100).toFixed(1) : 0,
      color: "#a8edea",
    },
    {
      name: "⚡ 电费",
      value: electricTotal,
      percentage: grandTotal > 0 ? (electricTotal / grandTotal * 100).toFixed(1) : 0,
      color: "#ffd93d",
    },
    {
      name: "🔥 燃气费",
      value: gasTotal,
      percentage: grandTotal > 0 ? (gasTotal / grandTotal * 100).toFixed(1) : 0,
      color: "#ff6b9d",
    },
  ];
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
    series: [
      {
        name: "总费用",
        data: processedData.totalAmounts,
        color: "#ff6b9d",
      },
      {
        name: "水费",
        data: processedData.waterBills,
        color: "#a8edea",
      },
      {
        name: "电费",
        data: processedData.electricBills,
        color: "#ffd93d",
      },
      {
        name: "燃气费",
        data: processedData.gasBills,
        color: "#6bcf7f",
      },
    ],
  };
}

/**
 * 生成柱状图数据
 * @param {Object} processedData - 处理后的数据
 * @returns {Object} 柱状图数据
 */
export function generateBarChartData(processedData) {
  return {
    categories: processedData.timeline,
    series: [
      {
        name: "水费",
        data: processedData.waterBills,
        color: "#a8edea",
      },
      {
        name: "电费",
        data: processedData.electricBills,
        color: "#ffd93d",
      },
      {
        name: "燃气费",
        data: processedData.gasBills,
        color: "#6bcf7f",
      },
    ],
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
