<template>
  <view class="container">
    <!-- 头部 -->
    <CustomHeader :title="pageTitle" :showBack="true" :showGlass="true" />

    <!-- 筛选区域 -->
    <view class="filter-container">
      <view class="filter-row">
        <view class="filter-item" @tap="showDatePicker">
          <text class="filter-label">📅</text>
          <text class="filter-value">{{ dateRangeText }}</text>
        </view>
        <view class="filter-action" @tap="resetFilter">
          <uni-icons
            custom-prefix="iconfont"
            type="icon-shuaxin1"
            color="#5a7c9a"
            size="18"
          ></uni-icons>
        </view>
      </view>
    </view>

    <!-- 账单列表 -->
    <scroll-view
      class="bill-list"
      scroll-y
      @scrolltolower="loadMore"
      :lower-threshold="50"
    >
      <uni-swipe-action>
        <uni-swipe-action-item
          v-for="bill in billList"
          :key="bill.id"
          :right-options="swipeOptions"
          @click="handleSwipeClick($event, bill)"
        >
          <view class="bill-item" @tap="viewBillDetail(bill)">
            <view class="bill-header">
              <text class="bill-date">{{
                formatDate(bill.year, bill.month)
              }}</text>
              <text class="bill-total"
                >¥{{ formatMoney(bill.totalAmount) }}</text
              >
            </view>
            <view class="bill-details">
              <view class="detail-item">
                <text class="detail-icon">💧</text>
                <text class="detail-label">水费</text>
                <text class="detail-value"
                  >¥{{ formatMoney(bill.waterBill) }}</text
                >
              </view>
              <view class="detail-item">
                <text class="detail-icon">⚡</text>
                <text class="detail-label">电费</text>
                <text class="detail-value"
                  >¥{{ formatMoney(bill.electricBill) }}</text
                >
              </view>
              <view class="detail-item">
                <text class="detail-icon">🔥</text>
                <text class="detail-label">燃气费</text>
                <text class="detail-value"
                  >¥{{ formatMoney(bill.gasBill) }}</text
                >
              </view>
            </view>
          </view>
        </uni-swipe-action-item>
      </uni-swipe-action>

      <!-- 加载提示 -->
      <view v-if="loading" class="loading-tip">
        <text>加载中...</text>
      </view>

      <!-- 空状态 -->
      <view v-if="!loading && billList.length === 0" class="empty-state">
        <text class="empty-icon">📋</text>
        <text class="empty-text">暂无账单数据</text>
      </view>

      <!-- 没有更多数据 -->
      <view v-if="!loading && billList.length > 0 && !hasMore" class="no-more">
        <text>没有更多数据了</text>
      </view>
    </scroll-view>

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
          <view class="date-picker-box">
            <view class="date-picker-group">
              <text class="date-label">开始日期</text>
              <view class="date-row">
                <picker
                  mode="selector"
                  :range="yearOptions"
                  :value="startYearIndex"
                  @change="onStartYearChange"
                >
                  <view class="picker-btn">{{
                    filterParams.startYear || "年份"
                  }}</view>
                </picker>
                <text class="separator">-</text>
                <picker
                  mode="selector"
                  :range="monthOptions"
                  :value="startMonthIndex"
                  @change="onStartMonthChange"
                >
                  <view class="picker-btn">{{
                    filterParams.startMonth || "月份"
                  }}</view>
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
                  <view class="picker-btn">{{
                    filterParams.endYear || "年份"
                  }}</view>
                </picker>
                <text class="separator">-</text>
                <picker
                  mode="selector"
                  :range="monthOptions"
                  :value="endMonthIndex"
                  @change="onEndMonthChange"
                >
                  <view class="picker-btn">{{
                    filterParams.endMonth || "月份"
                  }}</view>
                </picker>
              </view>
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
          <button class="btn btn-primary" @tap="applyFilter">确定</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import api from "@/api/costSharingApi.js";
import CustomHeader from "@/components/CustomHeader/CustomHeader.vue";

// 滑动操作选项
const swipeOptions = ref([
  {
    text: "删除",
    style: {
      backgroundColor: "#ff6b9d",
      color: "#fff",
      fontSize: "28rpx",
      padding: "0 30rpx",
    },
  },
]);

// 页面参数
const billType = ref("lizi"); // lizi 或 gezi
const pageTitle = ref("账单列表");

// 账单列表数据
const billList = ref([]);
const currentPage = ref(0);
const pageSize = ref(5);
const totalPages = ref(0);
const loading = ref(false);
const hasMore = ref(true);

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

// 日期区间文本显示
const dateRangeText = computed(() => {
  if (
    filterParams.value.startYear &&
    filterParams.value.startMonth &&
    filterParams.value.endYear &&
    filterParams.value.endMonth
  ) {
    return `${filterParams.value.startYear}.${filterParams.value.startMonth} - ${filterParams.value.endYear}.${filterParams.value.endMonth}`;
  }
  return "选择日期区间";
});

// 初始化年份选项（最近10年）
const initYearOptions = () => {
  const currentYear = new Date().getFullYear();
  const years = [];
  for (let i = 0; i < 10; i++) {
    years.push(`${currentYear - i}年`);
  }
  yearOptions.value = years;
};

// 格式化日期显示
const formatDate = (year, month) => {
  return `${year}年${month}月`;
};

// 格式化金额
const formatMoney = (amount) => {
  if (!amount) return "0.00";
  return Number(amount).toFixed(2);
};

// 显示日期选择器
const showDatePicker = () => {
  showDatePickerModal.value = true;
};

// 关闭日期选择器
const closeDatePicker = () => {
  showDatePickerModal.value = false;
};

// 开始年份变更
const onStartYearChange = (e) => {
  startYearIndex.value = e.detail.value;
  filterParams.value.startYear = parseInt(yearOptions.value[e.detail.value]);
};

// 开始月份变更
const onStartMonthChange = (e) => {
  startMonthIndex.value = e.detail.value;
  filterParams.value.startMonth = Number(e.detail.value) + 1;
};

// 结束年份变更
const onEndYearChange = (e) => {
  endYearIndex.value = e.detail.value;
  filterParams.value.endYear = parseInt(yearOptions.value[e.detail.value]);
};

// 结束月份变更
const onEndMonthChange = (e) => {
  endMonthIndex.value = e.detail.value;
  filterParams.value.endMonth = Number(e.detail.value) + 1;
};

// 设置最近3个月
const setLast3Months = () => {
  const now = new Date();
  const endYear = now.getFullYear();
  const endMonth = now.getMonth() + 1;

  const startDate = new Date(now.getFullYear(), now.getMonth() - 2, 1);
  const startYear = startDate.getFullYear();
  const startMonth = startDate.getMonth() + 1;

  filterParams.value = { startYear, startMonth, endYear, endMonth };
  updatePickerIndices();
};

// 设置最近6个月
const setLast6Months = () => {
  const now = new Date();
  const endYear = now.getFullYear();
  const endMonth = now.getMonth() + 1;

  const startDate = new Date(now.getFullYear(), now.getMonth() - 5, 1);
  const startYear = startDate.getFullYear();
  const startMonth = startDate.getMonth() + 1;

  filterParams.value = { startYear, startMonth, endYear, endMonth };
  updatePickerIndices();
};

// 设置最近12个月
const setLast12Months = () => {
  const now = new Date();
  const endYear = now.getFullYear();
  const endMonth = now.getMonth() + 1;

  const startDate = new Date(now.getFullYear(), now.getMonth() - 11, 1);
  const startYear = startDate.getFullYear();
  const startMonth = startDate.getMonth() + 1;

  filterParams.value = { startYear, startMonth, endYear, endMonth };
  updatePickerIndices();
};

// 更新选择器索引
const updatePickerIndices = () => {
  const currentYear = new Date().getFullYear();
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

// 应用筛选
const applyFilter = () => {
  closeDatePicker();
  // 重置列表
  currentPage.value = 0;
  billList.value = [];
  hasMore.value = true;
  // 重新加载数据
  loadBills();
};

// 重置筛选
const resetFilter = () => {
  filterParams.value = {
    startYear: null,
    startMonth: null,
    endYear: null,
    endMonth: null,
  };
  startYearIndex.value = -1;
  startMonthIndex.value = -1;
  endYearIndex.value = -1;
  endMonthIndex.value = -1;
  // 重置列表
  currentPage.value = 0;
  billList.value = [];
  hasMore.value = true;
  // 重新加载数据
  loadBills();
};

// 加载账单数据
const loadBills = async () => {
  if (loading.value || !hasMore.value) return;

  loading.value = true;
  try {
    const apiMethod = billType.value === "lizi" ? api.liziBill : api.geziBill;

    // 使用 getPage 接口，支持区间筛选参数
    const result = await apiMethod.getPage({
      ...filterParams.value,
      page: currentPage.value,
      size: pageSize.value,
    });

    // 处理分页数据
    const newBills = result.content || [];
    billList.value = [...billList.value, ...newBills];

    totalPages.value = result.totalPages || 0;
    hasMore.value = currentPage.value < totalPages.value - 1;
  } catch (error) {
    console.error("加载账单失败", error);
    uni.showToast({
      title: "加载失败",
      icon: "none",
    });
  } finally {
    loading.value = false;
  }
};

// 加载更多
const loadMore = () => {
  if (hasMore.value && !loading.value) {
    currentPage.value++;
    loadBills();
  }
};

// 处理滑动操作点击
const handleSwipeClick = (e, bill) => {
  if (e.index === 0) {
    // 点击了删除按钮
    uni.showModal({
      title: "确认删除",
      content: `确定要删除 ${formatDate(bill.year, bill.month)} 的账单吗？`,
      confirmColor: "#ff6b9d",
      success: async (res) => {
        if (res.confirm) {
          try {
            const apiMethod =
              billType.value === "lizi" ? api.liziBill : api.geziBill;
            await apiMethod.delete(bill.id);

            uni.showToast({
              title: "删除成功",
              icon: "success",
            });

            // 从列表中移除该项
            billList.value = billList.value.filter((b) => b.id !== bill.id);
          } catch (error) {
            console.error("删除账单失败", error);
            uni.showToast({
              title: "删除失败",
              icon: "none",
            });
          }
        }
      },
    });
  }
};

// 查看账单详情
const viewBillDetail = (bill) => {
  // 可以导航到详情页或显示详情弹窗
  console.log("查看账单详情", bill);
};

// 页面加载
onMounted(() => {
  // 获取系统信息
  /*uni.getSystemInfo({
    success: (res) => {
      statusBarHeight.value = res.statusBarHeight || 20;
    },
  });*/

  // 获取页面参数
  const pages = getCurrentPages();
  const currentPage = pages[pages.length - 1];
  const options = currentPage.options || {};

  billType.value = options.type || "lizi";
  pageTitle.value = billType.value === "lizi" ? "李子账单列表" : "鸽子账单列表";

  // 初始化年份选项
  initYearOptions();

  // 设置默认为最近12个月
  setLast12Months();

  // 加载账单数据
  loadBills();
});
</script>

<style lang="scss" scoped>
.container {
  height: 100vh;
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 50%, #ffecd2 100%);
}

.filter-container {
  padding: 24rpx;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.filter-item {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 24rpx 32rpx;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 16rpx;
  box-shadow: 0 4rpx 16rpx rgba(90, 124, 154, 0.1);
}

.filter-label {
  font-size: 32rpx;
}

.filter-value {
  font-size: 28rpx;
  color: #5a7c9a;
  font-weight: 500;
}

.filter-action {
  padding: 23rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 16rpx;
  box-shadow: 0 4rpx 16rpx rgba(90, 124, 154, 0.1);
}

.bill-list {
  height: calc(100vh - 320rpx - var(--status-bar-height));
  padding: 0 24rpx 24rpx;
  box-sizing: border-box;
}

.bill-item {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 24rpx;
  padding: 32rpx;
  box-shadow: 0 8rpx 24rpx rgba(90, 124, 154, 0.1);
  transition: all 0.3s ease;
  margin-bottom: 24rpx;
}
/* swipe-action 样式优化 */
:deep(.uni-swipe_button) {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  margin-bottom: 24rpx;
  border-radius: 24rpx;
}

.bill-item:active {
  transform: scale(0.98);
}

.bill-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
  padding-bottom: 24rpx;
  border-bottom: 1rpx solid rgba(90, 124, 154, 0.1);
}

.bill-date {
  font-size: 32rpx;
  font-weight: 600;
  color: #5a7c9a;
}

.bill-total {
  font-size: 40rpx;
  font-weight: 700;
  color: #ff6b9d;
}

.bill-details {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.detail-icon {
  font-size: 28rpx;
  width: 48rpx;
}

.detail-label {
  flex: 1;
  font-size: 28rpx;
  color: #6b8aa6;
}

.detail-value {
  font-size: 28rpx;
  font-weight: 600;
  color: #5a7c9a;
}

.loading-tip,
.no-more {
  text-align: center;
  padding: 32rpx;
  color: #6b8aa6;
  font-size: 28rpx;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 0;
  gap: 24rpx;
}

.empty-icon {
  font-size: 120rpx;
  opacity: 0.3;
}

.empty-text {
  font-size: 28rpx;
  color: #6b8aa6;
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
  max-height: calc(80vh - 300rpx);
  overflow-y: auto;
}
.date-picker-box {
  display: flex;
  align-items: center;
  gap: 60rpx;
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
</style>
