<template>
  <view class="container">
    <!-- 状态栏占位 -->
    <view class="status-bar" :style="{ height: statusBarHeight + 'px' }"></view>

    <view v-if="!isShareMode" class="header">
      <text class="title">💧⚡🔥 水电燃气分账计算器</text>
    </view>

    <view v-if="!isShareMode" class="form-container">
      <view class="form-group">
        <text class="label">📅 选择月份</text>
        <picker
          mode="selector"
          :range="monthOptions"
          :value="monthIndex"
          @change="onMonthChange"
        >
          <view class="picker">
            {{ formData.month ? `${formData.month}月` : "请选择月份" }}
          </view>
        </picker>
      </view>

      <view class="form-group">
        <text class="label">📆 月份总天数</text>
        <input
          type="number"
          v-model.number="formData.totalDays"
          placeholder="例如：30"
          class="input"
          @input="handleTotalDaysInput"
        />
        <text v-if="errors.totalDays" class="error">⚠️ 天数必须在1-31之间</text>
      </view>

      <view class="form-group">
        <text class="label">💧 水费（元）</text>
        <uni-easyinput
          type="digit"
          v-model="formData.waterBill"
          placeholder="0.00"
          :clearable="true"
          :inputBorder="true"
          :styles="easyinputStyles"
          :placeholderStyle="easyinputPlaceholderStyle"
          class="easyinput"
          @input="(val) => handleMoneyInputValue('waterBill', val)"
          @blur="() => fixMoneyOnBlur('waterBill')"
        ></uni-easyinput>
      </view>

      <view class="form-group">
        <text class="label">⚡ 电费（元）</text>
        <uni-easyinput
          type="digit"
          v-model="formData.electricBill"
          placeholder="0.00"
          :clearable="true"
          :inputBorder="true"
          :styles="easyinputStyles"
          :placeholderStyle="easyinputPlaceholderStyle"
          class="easyinput"
          @input="(val) => handleMoneyInputValue('electricBill', val)"
          @blur="() => fixMoneyOnBlur('electricBill')"
        ></uni-easyinput>
      </view>

      <view class="form-group">
        <text class="label">🔥 燃气费（元）</text>
        <uni-easyinput
          type="digit"
          v-model="formData.gasBill"
          placeholder="0.00"
          :clearable="true"
          :inputBorder="true"
          :styles="easyinputStyles"
          :placeholderStyle="easyinputPlaceholderStyle"
          class="easyinput"
          @input="(val) => handleMoneyInputValue('gasBill', val)"
          @blur="() => fixMoneyOnBlur('gasBill')"
        ></uni-easyinput>
      </view>

      <view class="form-group">
        <text class="label">🏠 房主居住天数</text>
        <input
          type="number"
          v-model.number="formData.ownerDays"
          placeholder="0"
          class="input"
          @input="handleOwnerDaysInput"
          min="1"
          step="1"
        />
        <text v-if="errors.ownerDays" class="error"
          >⚠️ 房主居住天数必须为正整数且不能超过总天数</text
        >
        <text
          v-if="
            formData.ownerDays &&
            (!Number.isInteger(formData.ownerDays) || formData.ownerDays <= 0)
          "
          class="error"
        >
          ⚠️ 房主居住天数必须为正整数
        </text>
      </view>

      <view class="button-group">
        <button class="btn btn-primary" @click="calculateBill">计算费用</button>
        <button class="btn btn-secondary" @click="resetForm">重置</button>
      </view>
    </view>

    <view v-if="showResult" class="result">
      <view class="result-header" @click="toggleShareMode">
        <text class="result-title">💰 费用分摊结果</text>
      </view>

      <view class="summary compact">
        <text class="summary-title">{{ summaryTitle }}</text>
        <view class="summary-chips">
          <text class="chip">💧 {{ formatAmount(formData.waterBill) }}</text>
          <text class="chip">⚡ {{ formatAmount(formData.electricBill) }}</text>
          <text class="chip">🔥 {{ formatAmount(formData.gasBill) }}</text>
        </view>
      </view>

      <view class="result-item">
        <text class="result-label">💵 总费用</text>
        <text class="result-value highlight"
          >{{ formatAmount(result.totalAmount) }} 元</text
        >
      </view>

      <view class="result-item">
        <text class="result-label">
          👤 房主应承担
          <text v-if="formData.ownerDays" class="subnote"
            >（居住{{ formData.ownerDays }}天）</text
          >
        </text>
        <text class="result-value"
          >{{ formatAmount(result.ownerAmount) }} 元</text
        >
      </view>

      <view class="result-item">
        <text class="result-label">📊 剩余费用</text>
        <text class="result-value"
          >{{ formatAmount(result.remainingAmount) }} 元</text
        >
      </view>

      <view class="summary">
        <text class="summary-title">💫 剩余费用分摊：</text>
        <view class="result-item">
          <text class="result-label">🌸 谢林珠应承担</text>
          <text class="result-value"
            >{{ formatAmount(result.sisterAmount) }} 元</text
          >
        </view>
        <view class="result-item">
          <text class="result-label">🌻 张锦豪应承担</text>
          <text class="result-value"
            >{{ formatAmount(result.datouAmount) }} 元</text
          >
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import {
  getLastMonth,
  getDaysInMonth,
  formatAmount,
  getMonthCn,
} from "@/utils/helpers.js";
import { calculateCostSharing } from "@/utils/calculator.js";
import { validateTotalDays, validateOwnerDays } from "@/utils/validator.js";

// 月份选项
const monthOptions = Array.from({ length: 12 }, (_, i) => `${i + 1}月`);
const monthIndex = ref(0);

// uni-easyinput 統一樣式
const easyinputStyles = {
  color: "#5a7c9a",
  backgroundColor: "#fafbfc",
  borderColor: "#e8f0f5",
  borderRadius: "32rpx",
};

// placeholder 樣式
const easyinputPlaceholderStyle = "font-size:32rpx;color:#5a7c9a;";

// 表单数据
const formData = ref({
  month: "",
  totalDays: "",
  waterBill: "",
  electricBill: "",
  gasBill: "",
  ownerDays: 0,
});

// 错误信息
const errors = ref({
  totalDays: false,
  ownerDays: false,
});

// 结果显示
const showResult = ref(false);
const isShareMode = ref(false);

// 状态栏高度
const statusBarHeight = ref(0);

// 计算结果
const result = ref({
  totalAmount: 0,
  ownerAmount: 0,
  remainingAmount: 0,
  sisterAmount: 0,
  datouAmount: 0,
});

// 摘要标题
const summaryTitle = computed(() => {
  const monthCn = getMonthCn(parseInt(formData.value.month, 10));
  const totalDays = formData.value.totalDays;

  if (monthCn && !isNaN(totalDays)) {
    return `🧾 ${monthCn}（${totalDays}天）账单：`;
  }
  return "🧾 本次账单：";
});

// 初始化月份
const initMonth = () => {
  const { month, year } = getLastMonth();
  formData.value.month = month;
  monthIndex.value = month - 1;
  const days = getDaysInMonth(month, year);
  formData.value.totalDays = days;
};

// 月份选择变化
const onMonthChange = (e) => {
  const index = Number(e.detail.value);
  const month = index + 1;
  formData.value.month = month;
  monthIndex.value = index;

  const now = new Date();
  const year = now.getFullYear();
  const days = getDaysInMonth(month, year);
  formData.value.totalDays = days;

  // 重新验证房主居住天数
  handleOwnerDaysInput();
};

// 验证总天数
const handleTotalDaysInput = () => {
  const days = Number(formData.value.totalDays);
  errors.value.totalDays = !validateTotalDays(days);
};

// 验证房主居住天数
const handleOwnerDaysInput = () => {
  const ownerDays = Number(formData.value.ownerDays);
  const totalDays = Number(formData.value.totalDays);
  errors.value.ownerDays = !validateOwnerDays(ownerDays, totalDays);
};

// 处理 uni-easyinput 的输入值（直接接收字符串值）
const handleMoneyInputValue = (field, val) => {
  let value = String(val ?? "");

  value = value.replace(/[^\d.]/g, "");

  if (value.startsWith(".")) {
    value = "0." + value.slice(1).replace(/\./g, "");
  }

  const firstDotIndex = value.indexOf(".");
  if (firstDotIndex !== -1) {
    let integerPart = value.substring(0, firstDotIndex);
    let decimalRaw = value.substring(firstDotIndex + 1).replace(/\./g, "");
    integerPart = integerPart.replace(/^0+(?=\d)/, "");
    if (integerPart === "") integerPart = "0";
    const decimalPart = decimalRaw.substring(0, 2);
    value = integerPart + "." + decimalPart;
  } else {
    value = value.replace(/^0+(?=\d)/, "");
  }

  formData.value[field] = value;
};

// 失焦后将金额格式化为两位小数
const fixMoneyOnBlur = (field) => {
  const raw = formData.value[field];
  if (raw === "" || raw === "." || raw === undefined || raw === null) {
    formData.value[field] = "";
    return;
  }
  const num = parseFloat(raw);
  if (isNaN(num)) {
    formData.value[field] = "";
    return;
  }
  formData.value[field] = num.toFixed(2);
};

// 计算费用
const calculateBill = () => {
  // 验证表单
  const totalDays = Number(formData.value.totalDays);
  const ownerDays = Number(formData.value.ownerDays);

  const isTotalDaysValid = validateTotalDays(totalDays);
  const isOwnerDaysValid = validateOwnerDays(ownerDays, totalDays);

  errors.value.totalDays = !isTotalDaysValid;
  errors.value.ownerDays = !isOwnerDaysValid;

  if (!isTotalDaysValid || !isOwnerDaysValid) {
    showResult.value = false;
    return;
  }

  // 确保金额字段为数字类型
  const calculationData = {
    ...formData.value,
    waterBill: parseFloat(formData.value.waterBill) || 0,
    electricBill: parseFloat(formData.value.electricBill) || 0,
    gasBill: parseFloat(formData.value.gasBill) || 0,
    totalDays: totalDays,
    ownerDays: ownerDays,
  };

  // 计算费用
  const calculated = calculateCostSharing(calculationData);
  result.value = calculated;
  showResult.value = true;
};

// 重置表单
const resetForm = () => {
  formData.value = {
    month: "",
    totalDays: "",
    waterBill: "",
    electricBill: "",
    gasBill: "",
    ownerDays: 0,
  };
  errors.value = {
    totalDays: false,
    ownerDays: false,
  };
  showResult.value = false;
  isShareMode.value = false;
  monthIndex.value = 0;
  initMonth();
};

// 切换分享模式
const toggleShareMode = () => {
  isShareMode.value = !isShareMode.value;
  // 小程序中可以通过设置页面标题等方式实现分享模式
};

// 页面分享配置
const onShareAppMessage = () => {
  return {
    title: "分账计算器",
    desc: "轻松计算水电燃气费用分摊，让合租生活更简单！",
    path: "/pages/index/index",
    imageUrl: "", // 可以设置自定义分享图片
    success: function (res) {
      console.log("分享成功", res);
    },
    fail: function (res) {
      console.log("分享失败", res);
    },
  };
};

// 分享到朋友圈
const onShareTimeline = () => {
  return {
    title: "水电燃气分账计算器 - 让合租生活更简单",
    query: "",
    imageUrl: "", // 可以设置自定义分享图片
    success: function (res) {
      console.log("分享朋友圈成功", res);
    },
    fail: function (res) {
      console.log("分享朋友圈失败", res);
    },
  };
};

// 页面加载时初始化
onMounted(() => {
  setTimeout(() => {
    initMonth();
  }, 100);
  // 获取系统信息，设置状态栏高度
  uni.getSystemInfo({
    success: (res) => {
      statusBarHeight.value = res.statusBarHeight || 20;
    },
    fail: () => {
      statusBarHeight.value = 20; // 默认值
    },
  });
});

// 导出分享方法供小程序调用
defineExpose({
  onShareAppMessage,
  onShareTimeline,
});
</script>

<style lang="scss" scoped>
.status-bar {
  width: 100%;
  background: transparent;
}

.container {
  min-height: 100vh;
  padding: 48rpx 24rpx 32rpx;
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 50%, #ffecd2 100%);
}

.header {
  text-align: center;
  margin-bottom: 60rpx;
}

.title {
  font-size: 36rpx;
  font-weight: 700;
  color: #5a7c9a;
  text-shadow: 2rpx 2rpx 4rpx rgba(0, 0, 0, 0.05);
  letter-spacing: 2rpx;
}

.form-container {
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(10px);
  border-radius: 30rpx;
  padding: 40rpx;
  box-shadow: 0 20rpx 60rpx rgba(0, 0, 0, 0.1);
  margin-bottom: 40rpx;
}

.form-group {
  margin-bottom: 48rpx;
  position: relative;
}

.label {
  display: block;
  margin-bottom: 20rpx;
  color: #6b7c93;
  font-weight: 600;
  font-size: 30rpx;
}

.input {
  width: 100%;
  padding: 0 32rpx;
  border: 2rpx solid #e8f0f5;
  border-radius: 32rpx;
  font-size: 32rpx;
  height: 104rpx;
  background: #fafbfc;
  color: #5a7c9a;
}

.input:focus {
  border-color: #ffb3d9;
  background: #fff;
}

:deep(.uni-easyinput .uni-easyinput__content) {
  border: 2rpx solid #e8f0f5 !important;
  border-radius: 32rpx !important;
  background: #fafbfc !important;
  height: 104rpx !important;
  padding: 0 32rpx !important;
  box-shadow: none !important;
}

:deep(.uni-easyinput .uni-easyinput__content input) {
  font-size: 32rpx !important;
  color: #5a7c9a !important;
  padding-left: 0 !important;
}

/* 聚焦時邊框與背景 */
:deep(.uni-easyinput .uni-easyinput__content.is-input-focus),
:deep(.uni-easyinput .uni-easyinput__content.is-focused),
:deep(.uni-easyinput .uni-easyinput__content:focus-within) {
  border-color: #ffb3d9 !important;
  background: #fff !important;
}

.picker {
  width: 100%;
  padding: 28rpx 32rpx;
  border: 2rpx solid #e8f0f5;
  border-radius: 32rpx;
  font-size: 32rpx;
  background: #fafbfc;
  color: #5a7c9a;
}

.error {
  display: block;
  color: #ff6b6b;
  font-size: 24rpx;
  margin-top: 12rpx;
  padding-left: 48rpx;
}

.button-group {
  display: flex;
  gap: 24rpx;
  margin-top: 70rpx;
}

.btn {
  flex: 1;
  padding: 12rpx 0;
  border-radius: 40rpx;
  font-size: 32rpx;
  font-weight: 600;
  border: none;
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

.result {
  background: linear-gradient(135deg, #f5f7fa 0%, #fef5e7 100%);
  border-radius: 48rpx;
  padding: 24rpx 32rpx;
  border: 2rpx dashed #ffb3d9;
  box-shadow: inset 0 4rpx 20rpx rgba(0, 0, 0, 0.05);
}

.result-header {
  text-align: center;
  margin-bottom: 44rpx;
  padding-bottom: 24rpx;
  border-bottom: 3rpx solid #ffb3d9;
}

.result-title {
  font-size: 48rpx;
  font-weight: 700;
  color: #5a7c9a;
}

.summary {
  margin-top: 44rpx;
  padding: 40rpx;
  background: linear-gradient(135deg, #e8f5f8 0%, #fff5f5 100%);
  border-radius: 36rpx;
  border-left: 5rpx solid #ffb3d9;
}

.summary.compact {
  background: rgba(255, 255, 255, 0.9);
  border-left: none;
  border: 1rpx dashed #e7eef5;
  padding: 24rpx 28rpx;
}

.summary-title {
  display: block;
  color: #3e627f;
  font-weight: 600;
  font-size: 30rpx;
  margin-bottom: 20rpx;
}

.summary-chips {
  display: flex;
  gap: 12rpx;
}

.chip {
  flex: 1;
  background: rgba(255, 255, 255, 0.8);
  border: 1rpx solid #eef2f7;
  border-radius: 999rpx;
  padding: 8rpx 12rpx;
  color: #3e627f;
  font-weight: 700;
  font-size: 28rpx;
  text-align: center;
}

.result-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx dashed #e0e0e0;
}

.result-item:last-child {
  border-bottom: none;
}

.result-label {
  color: #6b7c93;
  font-weight: 600;
  font-size: 30rpx;
}

.subnote {
  color: #7c95aa;
  font-weight: 600;
  font-size: 26rpx;
  margin-left: 16rpx;
}

.result-value {
  color: #5a7c9a;
  font-weight: 700;
  font-size: 36rpx;
}

.result-value.highlight {
  color: #ff6b9d;
  font-size: 44rpx;
}
</style>
