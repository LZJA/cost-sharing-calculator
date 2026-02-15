<template>
  <view class="container">
    <!-- 头部 -->
    <CustomHeader title="统计分析" :showBack="true" :showGlass="true" />

    <!-- 账单类型切换 -->
    <view class="type-tabs">
      <view
        class="tab"
        :class="{ active: billType === 'lizi' }"
        @tap="switchBillType('lizi')"
      >
        🍐 李子账单
      </view>
      <view
        class="tab"
        :class="{ active: billType === 'gezi' }"
        @tap="switchBillType('gezi')"
      >
        🕊️ 鸽子账单
      </view>
    </view>

    <!-- 筛选面板 -->
    <view class="filter-panel" @tap="showDatePickerModal = true">
      <view class="filter-content">
        <text class="filter-icon">📅</text>
        <text class="filter-text">{{ dateRangeText }}</text>
        <text class="filter-arrow">▼</text>
      </view>
    </view>

    <!-- 加载状态 -->
    <view v-if="loading" class="loading-container">
      <text class="loading-text">加载中...</text>
    </view>

    <!-- 空状态 -->
    <view v-else-if="!loading && !hasData" class="empty-state">
      <text class="empty-icon">📊</text>
      <text class="empty-text">暂无数据</text>
      <text class="empty-hint">请先添加账单记录</text>
    </view>

    <!-- 统计内容 -->
    <scroll-view v-else class="content" scroll-y>
      <!-- 核心指标卡片 -->
      <view class="metrics-container">
        <view class="metric-card primary">
          <text class="metric-label">总支出</text>
          <text class="metric-value"
            >¥{{ formatAmount(metrics.totalExpense) }}</text
          >
        </view>
        <view class="metric-card secondary">
          <text class="metric-label">月均支出</text>
          <text class="metric-value"
            >¥{{ formatAmount(metrics.avgMonthly) }}</text
          >
        </view>
      </view>

      <view class="metrics-container">
        <view class="metric-card">
          <text class="metric-label">📈 最高月份</text>
          <text class="metric-value-small">{{ metrics.maxMonth.month }}</text>
          <text class="metric-sub"
            >¥{{ formatAmount(metrics.maxMonth.amount) }}</text
          >
        </view>
        <view class="metric-card">
          <text class="metric-label">📉 最低月份</text>
          <text class="metric-value-small">{{ metrics.minMonth.month }}</text>
          <text class="metric-sub"
            >¥{{ formatAmount(metrics.minMonth.amount) }}</text
          >
        </view>
      </view>

      <!-- 同比/环比分析 -->
      <view class="comparison-container">
        <view class="comparison-card">
          <text class="comparison-label">环比上月</text>
          <text
            class="comparison-value"
            :class="{
              up: comparison.monthOverMonth > 0,
              down: comparison.monthOverMonth < 0,
            }"
          >
            {{
              comparison.monthOverMonth > 0
                ? "↑"
                : comparison.monthOverMonth < 0
                  ? "↓"
                  : "-"
            }}
            {{ Math.abs(comparison.monthOverMonth) }}%
          </text>
        </view>
        <view class="comparison-card">
          <text class="comparison-label">同比去年</text>
          <text
            class="comparison-value"
            :class="{
              up: comparison.yearOverYear > 0,
              down: comparison.yearOverYear < 0,
            }"
          >
            {{
              comparison.yearOverYear > 0
                ? "↑"
                : comparison.yearOverYear < 0
                  ? "↓"
                  : "-"
            }}
            {{ Math.abs(comparison.yearOverYear) }}%
          </text>
        </view>
      </view>

      <!-- 分摊费用统计 -->
      <view class="split-container">
        <text class="section-title">💰 分摊费用统计</text>
        <view class="split-list">
          <view
            v-for="(item, index) in splitStatistics"
            :key="index"
            class="split-item"
          >
            <view class="split-info">
              <text class="split-name">{{ item.name }}</text>
              <text class="split-amount">¥{{ formatAmount(item.amount) }}</text>
            </view>
            <view class="split-progress">
              <view
                class="split-progress-bar"
                :style="{ width: item.percentage + '%' }"
              ></view>
            </view>
            <text class="split-percentage">{{ item.percentage }}%</text>
          </view>
        </view>
      </view>

      <!-- 费用类型占比 -->
      <view class="distribution-container">
        <text class="section-title">📊 费用类型占比</text>
        <view class="distribution-list">
          <view
            v-for="(item, index) in costDistribution"
            :key="index"
            class="distribution-item"
          >
            <view class="distribution-info">
              <text class="distribution-name">{{ item.name }}</text>
              <text class="distribution-amount"
                >¥{{ formatAmount(item.value) }}</text
              >
            </view>
            <view class="distribution-progress">
              <view
                class="distribution-progress-bar"
                :style="{
                  width: item.percentage + '%',
                  backgroundColor: item.color,
                }"
              ></view>
            </view>
            <text class="distribution-percentage">{{ item.percentage }}%</text>
          </view>
        </view>
      </view>

      <!-- 图表区域 -->
      <view class="chart-tabs">
        <view
          v-for="(tab, index) in chartTabs"
          :key="index"
          class="chart-tab"
          :class="{ active: activeChartTab === index }"
          @tap="switchChartTab(index)"
        >
          {{ tab.name }}
        </view>
      </view>

      <view class="chart-container">
        <!-- 折线图 -->
        <view v-show="activeChartTab === 0" class="chart-wrapper">
          <SimpleLineChart :key="chartKey" :data="lineChartData" />
        </view>

        <!-- 柱状图 -->
        <view v-show="activeChartTab === 1" class="chart-wrapper">
          <SimpleBarChart :key="chartKey" :data="barChartData" />
        </view>

        <!-- 饼图 -->
        <view v-show="activeChartTab === 2" class="chart-wrapper">
          <SimplePieChart :key="chartKey" :data="pieChartData" />
        </view>

        <!-- 组合图 -->
        <view v-show="activeChartTab === 3" class="chart-wrapper">
          <SimpleComboChart :key="chartKey" :data="comboChartData" />
        </view>
      </view>
    </scroll-view>

    <!-- 底部分享按钮 -->
    <view class="bottom-share-btn" @click="showShareMenu">
      <text class="share-btn-text">📊 生成统计报告</text>
    </view>

    <!-- 日期选择弹窗 -->
    <view
      v-if="showDatePickerModal"
      class="modal-overlay"
      @tap="closeDatePicker"
    >
      <view class="modal-content" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">选择日期区间</text>
          <view class="modal-close" @tap="closeDatePicker">
            <text>✕</text>
          </view>
        </view>
        <view class="modal-body">
          <view class="date-picker-group">
            <text class="date-label">开始日期</text>
            <view class="date-row">
              <picker
                mode="selector"
                :range="yearOptions"
                :value="startYearIndex"
                @change="onStartYearChange"
              >
                <view class="picker-btn">
                  {{ filterParams.startYear || "年份" }}
                </view>
              </picker>
              <text class="separator">-</text>
              <picker
                mode="selector"
                :range="monthOptions"
                :value="startMonthIndex"
                @change="onStartMonthChange"
              >
                <view class="picker-btn">
                  {{ filterParams.startMonth || "月份" }}
                </view>
              </picker>
            </view>
          </view>

          <view class="date-picker-group">
            <text class="date-label">结束日期</text>
            <view class="date-row">
              <picker
                mode="selector"
                :range="yearOptions"
                :value="endYearIndex"
                @change="onEndYearChange"
              >
                <view class="picker-btn">
                  {{ filterParams.endYear || "年份" }}
                </view>
              </picker>
              <text class="separator">-</text>
              <picker
                mode="selector"
                :range="monthOptions"
                :value="endMonthIndex"
                @change="onEndMonthChange"
              >
                <view class="picker-btn">
                  {{ filterParams.endMonth || "月份" }}
                </view>
              </picker>
            </view>
          </view>

          <view class="quick-select">
            <text class="quick-label">快速选择：</text>
            <view class="quick-options">
              <view class="quick-btn" @tap="setLast3Months">最近3个月</view>
              <view class="quick-btn" @tap="setLast6Months">最近6个月</view>
              <view class="quick-btn" @tap="setLast12Months">最近12个月</view>
            </view>
          </view>
        </view>
        <view class="modal-footer">
          <button class="btn btn-secondary" @tap="closeDatePicker">取消</button>
          <button class="btn btn-primary" @tap="applyDateFilter">确定</button>
        </view>
      </view>
    </view>

    <!-- 分享报告海报组件 -->
    <ShareReportPoster
      v-if="sharePosterVisible"
      :reportData="reportData"
      @close="sharePosterVisible = false"
    />
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import CustomHeader from "@/components/CustomHeader/CustomHeader.vue";
import ShareReportPoster from "@/components/statistics/ShareReportPoster.vue";
import SimpleLineChart from "@/components/statistics/SimpleLineChart.vue";
import SimpleBarChart from "@/components/statistics/SimpleBarChart.vue";
import SimplePieChart from "@/components/statistics/SimplePieChart.vue";
import SimpleComboChart from "@/components/statistics/SimpleComboChart.vue";

import {
  fetchStatisticsData,
  processBillData,
  calculateCoreMetrics,
  calculateComparison,
  calculateSplitStatistics,
  calculateCostDistribution,
  getDefaultDateFilter,
  formatDateRangeText,
  findBillByMonth,
  generateLineChartData,
  generateBarChartData,
  generatePieChartData,
  generateComboChartData,
} from "@/utils/statisticsHelper.js";
import { formatAmount } from "@/utils/helpers.js";
import dayjs from "dayjs";

// 账单类型
const billType = ref("lizi");

// 加载状态
const loading = ref(false);
const hasData = ref(false);

// 筛选参数
const filterParams = ref({
  startYear: null,
  startMonth: null,
  endYear: null,
  endMonth: null,
});

// 日期选择器
const showDatePickerModal = ref(false);
const yearOptions = ref([]);
const monthOptions = ref([
  "1月",
  "2月",
  "3月",
  "4月",
  "5月",
  "6月",
  "7月",
  "8月",
  "9月",
  "10月",
  "11月",
  "12月",
]);
const startYearIndex = ref(-1);
const startMonthIndex = ref(-1);
const endYearIndex = ref(-1);
const endMonthIndex = ref(-1);

// 统计数据
const metrics = ref({
  totalExpense: 0,
  avgMonthly: 0,
  maxMonth: { month: "-", amount: 0 },
  minMonth: { month: "-", amount: 0 },
});

const comparison = ref({
  monthOverMonth: 0,
  yearOverYear: 0,
});

const splitStatistics = ref([]);
const costDistribution = ref([]);

// 图表相关
const chartTabs = [
  { name: "趋势", value: "line" },
  { name: "对比", value: "bar" },
  { name: "占比", value: "pie" },
  { name: "组合", value: "combo" },
];
const activeChartTab = ref(0);
const chartKey = ref(0);

// 图表数据
const lineChartData = ref({});
const barChartData = ref({});
const pieChartData = ref({});
const comboChartData = ref({});

// 分享
const sharePosterVisible = ref(false);
const reportData = ref({});

// 日期区间文本
const dateRangeText = computed(() => formatDateRangeText(filterParams.value));

// 初始化年份选项
const initYearOptions = () => {
  const currentYear = dayjs().year();
  const years = [];
  for (let i = 0; i < 10; i++) {
    years.push(`${currentYear - i}年`);
  }
  yearOptions.value = years;
};

// 更新选择器索引
const updatePickerIndices = () => {
  const currentYear = dayjs().year();
  if (filterParams.value.startYear) {
    startYearIndex.value = currentYear - filterParams.value.startYear;
  }
  if (filterParams.value.startMonth) {
    startMonthIndex.value = filterParams.value.startMonth - 1;
  }
  if (filterParams.value.endYear) {
    endYearIndex.value = currentYear - filterParams.value.endYear;
  }
  if (filterParams.value.endMonth) {
    endMonthIndex.value = filterParams.value.endMonth - 1;
  }
};

// 加载数据
const loadData = async () => {
  loading.value = true;
  hasData.value = false;

  try {
    const bills = await fetchStatisticsData(billType.value, filterParams.value);

    if (bills.length === 0) {
      loading.value = false;
      return;
    }

    hasData.value = true;

    // 处理数据
    const processed = processBillData(bills, billType.value);

    // 计算核心指标
    metrics.value = calculateCoreMetrics(processed.sortedBills);

    // 计算同比环比
    const currentMonth =
      processed.sortedBills[processed.sortedBills.length - 1];
    const previousMonth =
      processed.sortedBills[processed.sortedBills.length - 2] || null;
    const lastYearMonth = findBillByMonth(
      processed.sortedBills,
      currentMonth.year - 1,
      currentMonth.month,
    );

    comparison.value = calculateComparison(
      currentMonth,
      previousMonth,
      lastYearMonth,
    );

    // 计算分摊统计
    splitStatistics.value = calculateSplitStatistics(
      processed.sortedBills,
      billType.value,
    );

    // 计算费用分布
    costDistribution.value = calculateCostDistribution(processed.sortedBills);

    // 生成图表数据
    lineChartData.value = generateLineChartData(processed);
    barChartData.value = generateBarChartData(processed);
    pieChartData.value = generatePieChartData(costDistribution.value);
    comboChartData.value = generateComboChartData(
      processed,
      metrics.value.avgMonthly,
    );

    // 准备报告数据
    reportData.value = {
      billType: billType.value,
      dateRange: dateRangeText.value,
      metrics: metrics.value,
      comparison: comparison.value,
      splitStatistics: splitStatistics.value,
      costDistribution: costDistribution.value,
      chartData: lineChartData.value,
    };
  } catch (error) {
    console.error("加载统计数据失败", error);
    uni.showToast({
      title: "加载失败",
      icon: "none",
    });
  } finally {
    loading.value = false;
  }
};

// 切换账单类型
const switchBillType = (type) => {
  if (billType.value === type) return;
  billType.value = type;
  loadData();
};

// 切换图表标签
const switchChartTab = (index) => {
  activeChartTab.value = index;
  chartKey.value++;
};

// 日期选择器变更
const onStartYearChange = (e) => {
  startYearIndex.value = e.detail.value;
  filterParams.value.startYear = parseInt(yearOptions.value[e.detail.value]);
};

const onStartMonthChange = (e) => {
  startMonthIndex.value = e.detail.value;
  filterParams.value.startMonth = Number(e.detail.value) + 1;
};

const onEndYearChange = (e) => {
  endYearIndex.value = e.detail.value;
  filterParams.value.endYear = parseInt(yearOptions.value[e.detail.value]);
};

const onEndMonthChange = (e) => {
  endMonthIndex.value = e.detail.value;
  filterParams.value.endMonth = Number(e.detail.value) + 1;
};

// 快速选择
const setLast3Months = () => {
  const now = dayjs();
  const endYear = now.year();
  const endMonth = now.month() + 1;

  const startDate = now.subtract(2, "month");
  const startYear = startDate.year();
  const startMonth = startDate.month() + 1;

  filterParams.value = { startYear, startMonth, endYear, endMonth };
  updatePickerIndices();
};

const setLast6Months = () => {
  const now = dayjs();
  const endYear = now.year();
  const endMonth = now.month() + 1;

  const startDate = now.subtract(5, "month");
  const startYear = startDate.year();
  const startMonth = startDate.month() + 1;

  filterParams.value = { startYear, startMonth, endYear, endMonth };
  updatePickerIndices();
};

const setLast12Months = () => {
  const now = dayjs();
  const endYear = now.year();
  const endMonth = now.month() + 1;

  const startDate = now.subtract(11, "month");
  const startYear = startDate.year();
  const startMonth = startDate.month() + 1;

  filterParams.value = { startYear, startMonth, endYear, endMonth };
  updatePickerIndices();
};

// 关闭日期选择器
const closeDatePicker = () => {
  showDatePickerModal.value = false;
};

// 应用日期筛选
const applyDateFilter = () => {
  closeDatePicker();
  loadData();
};

// 显示分享菜单
const showShareMenu = () => {
  sharePosterVisible.value = true;
};

// 页面加载
onMounted(() => {
  initYearOptions();
  // 设置默认为最近12个月
  setLast12Months();
  // 加载数据
  loadData();
});

defineExpose({
  formatAmount,
});
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 50%, #ffecd2 100%);
}

.header-action {
  padding: 16rpx 32rpx;
  background: linear-gradient(135deg, #ff9ab8 0%, #ffb3d9 100%);
  border-radius: 44rpx;
  box-shadow: 0 4rpx 16rpx rgba(255, 154, 184, 0.3);
}

.action-text {
  font-size: 28rpx;
  color: #fff;
  font-weight: 600;
}

.type-tabs {
  display: flex;
  gap: 24rpx;
  padding: 24rpx;
}

.tab {
  flex: 1;
  padding: 24rpx;
  text-align: center;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 24rpx;
  font-size: 30rpx;
  color: #5a7c9a;
  font-weight: 500;
  transition: all 0.3s ease;
  border: 2rpx solid transparent;
}

.tab.active {
  background: rgba(255, 255, 255, 0.8);
  color: #ff6b9d;
  font-weight: 600;
  border-color: #ff6b9d;
  box-shadow: 0 8rpx 24rpx rgba(255, 107, 157, 0.2);
}

.filter-panel {
  padding: 0 24rpx 24rpx;
}

.filter-content {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 24rpx 32rpx;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(90, 124, 154, 0.1);
}

.filter-icon {
  font-size: 32rpx;
}

.filter-text {
  flex: 1;
  font-size: 28rpx;
  color: #5a7c9a;
  font-weight: 500;
}

.filter-arrow {
  font-size: 24rpx;
  color: #6b8aa6;
}

.loading-container,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 0;
  gap: 24rpx;
}

.loading-text,
.empty-text {
  font-size: 28rpx;
  color: #6b8aa6;
}

.empty-icon {
  font-size: 120rpx;
  opacity: 0.3;
}

.empty-hint {
  font-size: 24rpx;
  color: #7c95aa;
}

.content {
  height: calc(
    100vh - 320rpx - var(--status-bar-height) -
      194rpx - env(safe-area-inset-bottom)
  );
  padding: 0 24rpx 24rpx;
  box-sizing: border-box;
}

.metrics-container {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.metric-card {
  flex: 1;
  padding: 32rpx 24rpx;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(90, 124, 154, 0.1);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}

.metric-card.primary {
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);
}

.metric-card.secondary {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
}

.metric-label {
  font-size: 26rpx;
  color: #6b7c93;
  font-weight: 500;
}

.metric-card.primary .metric-label,
.metric-card.secondary .metric-label {
  color: rgba(255, 255, 255, 0.9);
}

.metric-value {
  font-size: 40rpx;
  font-weight: 700;
  color: #ff6b9d;
}

.metric-card.primary .metric-value,
.metric-card.secondary .metric-value {
  color: #fff;
}

.metric-value-small {
  font-size: 32rpx;
  font-weight: 600;
  color: #5a7c9a;
}

.metric-sub {
  font-size: 24rpx;
  color: #7c95aa;
}

.comparison-container {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.comparison-card {
  flex: 1;
  padding: 24rpx;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(90, 124, 154, 0.1);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}

.comparison-label {
  font-size: 26rpx;
  color: #6b7c93;
  font-weight: 500;
}

.comparison-value {
  font-size: 36rpx;
  font-weight: 700;
  color: #5a7c9a;
}

.comparison-value.up {
  color: #ff6b9d;
}

.comparison-value.down {
  color: #6bcf7f;
}

.split-container,
.distribution-container {
  background: rgba(255, 255, 255, 0.8);
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(90, 124, 154, 0.1);
}

.section-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #5a7c9a;
  margin-bottom: 24rpx;
}

.split-list,
.distribution-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.split-item,
.distribution-item {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.split-info,
.distribution-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.split-name,
.distribution-name {
  font-size: 28rpx;
  color: #5a7c9a;
  font-weight: 500;
}

.split-amount,
.distribution-amount {
  font-size: 28rpx;
  color: #5a7c9a;
  font-weight: 600;
}

.split-progress,
.distribution-progress {
  height: 12rpx;
  background: #f0f0f0;
  border-radius: 12rpx;
  overflow: hidden;
}

.split-progress-bar {
  height: 100%;
  background: linear-gradient(135deg, #ff9ab8 0%, #ffb3d9 100%);
  border-radius: 12rpx;
  transition: width 0.5s ease;
}

.distribution-progress-bar {
  height: 100%;
  border-radius: 12rpx;
  transition: width 0.5s ease;
}

.split-percentage,
.distribution-percentage {
  font-size: 24rpx;
  color: #7c95aa;
  text-align: right;
}

.chart-tabs {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.chart-tab {
  flex: 1;
  padding: 20rpx;
  text-align: center;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 20rpx;
  font-size: 26rpx;
  color: #6b8aa6;
  font-weight: 500;
  transition: all 0.3s ease;
}

.chart-tab.active {
  background: rgba(255, 255, 255, 0.9);
  color: #ff6b9d;
  font-weight: 600;
  box-shadow: 0 4rpx 12rpx rgba(255, 107, 157, 0.15);
}

.chart-container {
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx;
  box-shadow: 0 8rpx 24rpx rgba(90, 124, 154, 0.1);
  margin-bottom: 24rpx;
}

.chart-wrapper {
  height: 400rpx;
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 日期选择弹窗 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx 20rpx;
}

.modal-content {
  background: #fff;
  border-radius: 24rpx;
  width: 100%;
  max-width: 600rpx;
  max-height: 80vh;
  overflow: hidden;
  box-shadow: 0 20rpx 60rpx rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 40rpx;
  border-bottom: 1rpx solid #e8f0f5;
}

.modal-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #5a7c9a;
}

.modal-close {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #f5f5f5;
  color: #666;
  font-size: 32rpx;
}

.modal-body {
  padding: 40rpx;
  max-height: calc(80vh - 200rpx);
  overflow-y: auto;
}

.date-picker-group {
  margin-bottom: 32rpx;
}

.date-label {
  display: block;
  margin-bottom: 16rpx;
  color: #5a7c9a;
  font-weight: 600;
  font-size: 28rpx;
}

.date-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.picker-btn {
  flex: 1;
  padding: 24rpx;
  background: #fafbfc;
  border: 2rpx solid #e8f0f5;
  border-radius: 12rpx;
  text-align: center;
  color: #5a7c9a;
  font-size: 28rpx;
}

.separator {
  color: #6b8aa6;
  font-size: 28rpx;
}

.quick-select {
  margin-top: 32rpx;
  padding-top: 32rpx;
  border-top: 1rpx solid #e8f0f5;
}

.quick-label {
  display: block;
  margin-bottom: 16rpx;
  color: #5a7c9a;
  font-size: 28rpx;
}

.quick-options {
  display: flex;
  gap: 16rpx;
}

.quick-btn {
  flex: 1;
  padding: 20rpx;
  background: linear-gradient(135deg, #ff9ab8 0%, #ffb3d9 100%);
  border-radius: 12rpx;
  text-align: center;
  color: #fff;
  font-size: 24rpx;
  font-weight: 500;
}

.quick-btn:active {
  opacity: 0.8;
}

.modal-footer {
  padding: 40rpx;
  border-top: 1rpx solid #e8f0f5;
  display: flex;
  gap: 24rpx;
}

.btn {
  flex: 1;
  height: 88rpx;
  border-radius: 44rpx;
  font-size: 32rpx;
  font-weight: 600;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;

  &::after {
    border: none;
  }
}

.btn-primary {
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 50%, #fecfef 100%);
  color: #fff;
  box-shadow: 0 8rpx 30rpx rgba(255, 154, 158, 0.3);
}

.btn-secondary {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  color: #fff;
  box-shadow: 0 8rpx 30rpx rgba(168, 237, 234, 0.3);
}

/* 底部分享按钮 */
.bottom-share-btn {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 24rpx;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.1);
  z-index: 10;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
}

.share-btn-text {
  display: block;
  width: 100%;
  padding: 28rpx;
  text-align: center;
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  border-radius: 44rpx;
  box-shadow: 0 8rpx 24rpx rgba(255, 154, 158, 0.3);
}

.share-btn-text:active {
  opacity: 0.8;
}
</style>
