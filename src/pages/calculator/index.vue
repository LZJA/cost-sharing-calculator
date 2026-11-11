<template>
  <view class="container">
    <CustomHeader :title="calculator.name || '分账计算'">
      <view slot="header-right" class="header-action" @click="viewBillList">
        <text class="action-text">查看账单</text>
      </view>
    </CustomHeader>

    <view class="form-container">
      <view class="form-group">
        <text class="label">📆 年份</text>
        <picker mode="selector" :range="yearOptions" :value="yearIndex" @change="onYearChange">
          <view class="picker">{{ form.year ? `${form.year}年` : "请选择年份" }}</view>
        </picker>
      </view>
      <view class="form-group">
        <text class="label">📅 月份</text>
        <picker mode="selector" :range="monthOptions" :value="monthIndex" @change="onMonthChange">
          <view class="picker">{{ form.month ? `${form.month}月` : "请选择月份" }}</view>
        </picker>
      </view>

      <view v-if="config.useStayDays" class="form-group">
        <text class="label">📆 月份总天数</text>
        <input v-model.number="form.totalDays" type="number" class="input" placeholder="例如：30" />
      </view>
      <view v-if="config.useStayDays" class="form-group">
        <text class="label">🏠 居住天数</text>
        <input v-model.number="form.ownerDays" type="number" class="input" placeholder="0" />
      </view>

      <view class="form-group">
        <text class="label">💧 水费（元）</text>
        <input v-model="form.waterBill" type="digit" class="input" placeholder="0.00" />
      </view>
      <view class="form-group">
        <text class="label">⚡ 电费（元）</text>
        <input v-model="form.electricBill" type="digit" class="input" placeholder="0.00" />
      </view>
      <view class="form-group">
        <text class="label">🔥 燃气费（元）</text>
        <input v-model="form.gasBill" type="digit" class="input" placeholder="0.00" />
      </view>

      <view v-if="config.rules.length > 1" class="form-group">
        <text class="label">📐 分账规则</text>
        <picker mode="selector" :range="ruleNames" :value="ruleIndex" @change="onRuleChange">
          <view class="picker">{{ ruleNames[ruleIndex] || "请选择规则" }}</view>
        </picker>
      </view>

      <view class="button-group">
        <button class="btn btn-primary" @tap="calculateBill">计算费用</button>
      </view>

      <text v-if="errorMessage" class="error">{{ errorMessage }}</text>

      <view v-if="showResult" class="result">
        <view class="result-header">
          <text class="result-title">💰 费用分摊结果</text>
        </view>
        <view class="result-item">
          <text class="result-label">💵 总费用</text>
          <text class="result-value">{{ totalAmount.toFixed(2) }} 元</text>
        </view>
        <view v-for="share in shares" :key="share.key" class="result-item">
          <text class="result-label">{{ share.emoji }} {{ share.name }}应承担</text>
          <text class="result-value">{{ share.amount.toFixed(2) }} 元</text>
        </view>
        <view class="button-group">
          <button class="btn btn-primary" @tap="generatePoster">🖼️ 生成分享海报</button>
        </view>
      </view>
    </view>

    <SharePoster ref="sharePosterRef" :data="posterData" />
  </view>
</template>

<script setup>
import { computed, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import dayjs from "dayjs";
import CustomHeader from "@/components/CustomHeader/CustomHeader.vue";
import SharePoster from "@/components/SharePoster/SharePoster.vue";
import api from "@/api/costSharingApi.js";
import { calculateShares } from "@/utils/formula.js";

const calculator = ref({});
const config = computed(() => calculator.value.config || { members: [], rules: [], useStayDays: false });

const currentYear = dayjs().year();
const yearOptions = Array.from({ length: 10 }, (_, i) => `${currentYear - i}年`);
const monthOptions = Array.from({ length: 12 }, (_, i) => `${i + 1}月`);
const yearIndex = ref(0);
const monthIndex = ref(0);
const ruleIndex = ref(0);

const form = ref({
  year: currentYear,
  month: dayjs().month() + 1,
  totalDays: "",
  ownerDays: 0,
  waterBill: "",
  electricBill: "",
  gasBill: "",
});

const shares = ref([]);
const showResult = ref(false);
const errorMessage = ref("");
const sharePosterRef = ref(null);

const ruleNames = computed(() => config.value.rules.map((rule) => rule.name));
const totalAmount = computed(
  () =>
    (parseFloat(form.value.waterBill) || 0) +
    (parseFloat(form.value.electricBill) || 0) +
    (parseFloat(form.value.gasBill) || 0)
);

const onYearChange = (event) => {
  yearIndex.value = Number(event.detail.value);
  form.value.year = currentYear - yearIndex.value;
};
const onMonthChange = (event) => {
  monthIndex.value = Number(event.detail.value);
  form.value.month = monthIndex.value + 1;
};
const onRuleChange = (event) => {
  ruleIndex.value = Number(event.detail.value);
};

const viewBillList = () => {
  uni.navigateTo({
    url: `/pages/billList/index?calculatorId=${calculator.value.id}&name=${encodeURIComponent(calculator.value.name || "")}`,
  });
};

const calculateBill = async () => {
  errorMessage.value = "";
  showResult.value = false;
  const ruleName = config.value.rules[ruleIndex.value]?.name;
  try {
    shares.value = calculateShares(config.value, ruleName, form.value);
    showResult.value = true;
  } catch (error) {
    errorMessage.value = error.message;
    return;
  }

  // 展示用上面的即时结果，落库金额由后端按同一套规则重算
  try {
    await api.bill.save({
      calculatorId: calculator.value.id,
      year: form.value.year,
      month: form.value.month,
      waterBill: parseFloat(form.value.waterBill) || 0,
      electricBill: parseFloat(form.value.electricBill) || 0,
      gasBill: parseFloat(form.value.gasBill) || 0,
      totalDays: config.value.useStayDays ? form.value.totalDays : null,
      ownerDays: config.value.useStayDays ? form.value.ownerDays : null,
      ruleName,
    });
  } catch (error) {
    console.error("保存账单失败", error);
  }
};

const posterData = computed(() => ({
  month: form.value.month,
  waterBill: form.value.waterBill || 0,
  electricBill: form.value.electricBill || 0,
  gasBill: form.value.gasBill || 0,
  shares: shares.value.map((share) => ({
    ...share,
    amount: share.amount.toFixed(2),
  })),
  totalAmount: totalAmount.value.toFixed(2),
}));

const generatePoster = () => {
  sharePosterRef.value.show();
};

onLoad(async (options) => {
  if (!options?.id) return;
  try {
    calculator.value = await api.calculator.getById(options.id);
  } catch (error) {
    console.error("加载计算器失败", error);
  }
});
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  padding: 24rpx 24rpx 80rpx;
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 50%, #ffecd2 100%);
}
.header-action {
  padding: 8rpx 20rpx;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 24rpx;
}
.action-text {
  color: #5a7c9a;
  font-size: 26rpx;
}
.form-container {
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(10px);
  border-radius: 30rpx;
  padding: 40rpx;
}
.form-group {
  margin-bottom: 36rpx;
}
.label {
  display: block;
  margin-bottom: 16rpx;
  color: #6b7c93;
  font-weight: 600;
}
.input,
.picker {
  background: #fafbfc;
  border: 1rpx solid #e8f0f5;
  border-radius: 24rpx;
  padding: 20rpx 24rpx;
  color: #5a7c9a;
  font-size: 30rpx;
}
.button-group {
  display: flex;
  gap: 24rpx;
  margin-top: 32rpx;
}
.btn {
  flex: 1;
  border-radius: 40rpx;
  font-size: 32rpx;
  font-weight: 600;
  border: none;
  &::after {
    border: none;
  }
}
.btn-primary {
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);
  color: #fff;
}
.error {
  display: block;
  color: #ff6b6b;
  margin-top: 20rpx;
}
.result {
  margin-top: 48rpx;
  background: linear-gradient(135deg, #f5f7fa 0%, #fef5e7 100%);
  border-radius: 36rpx;
  padding: 32rpx;
  border: 2rpx dashed #ffb3d9;
}
.result-title {
  display: block;
  text-align: center;
  font-size: 40rpx;
  font-weight: 700;
  color: #5a7c9a;
  margin-bottom: 24rpx;
}
.result-item {
  display: flex;
  justify-content: space-between;
  padding: 16rpx 0;
  color: #5a7c9a;
}
</style>
