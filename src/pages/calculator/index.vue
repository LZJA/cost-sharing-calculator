<template>
  <view class="container">
    <CustomHeader :title="calculator.name || '分账计算'" />
    <view v-if="calculator.id" class="calculator-toolbar">
      <text class="access-label">{{ calculator.currentRole === 'OWNER' ? '我创建的' : '同伴共享' }}</text>
      <view class="toolbar-actions">
        <button class="toolbar-button" @tap="viewCompanions">同伴</button>
        <button class="toolbar-button bills-button" @tap="viewBillList">查看账单 →</button>
      </view>
    </view>

    <view class="form-container">
      <view class="bill-guide"><text class="bill-guide-title">填金额 → 看结果 → 确认保存</text><text>查看结果不会记账，保存后才会出现在账单列表中。</text></view>
      <view class="form-group">
        <text class="label">📆 年份</text>
        <picker mode="selector" :range="yearOptions" :value="yearIndex" @change="onYearChange">
          <view class="picker"><text class="picker-label">{{ form.year ? `${form.year}年` : "请选择年份" }}</text><text class="iconfont icon-a-icon-down picker-arrow"></text></view>
        </picker>
      </view>
      <view class="form-group">
        <text class="label">📅 月份</text>
        <picker mode="selector" :range="monthOptions" :value="monthIndex" @change="onMonthChange">
          <view class="picker"><text class="picker-label">{{ form.month ? `${form.month}月` : "请选择月份" }}</text><text class="iconfont icon-a-icon-down picker-arrow"></text></view>
        </picker>
      </view>

      <view v-if="config.useStayDays" class="form-group">
        <text class="label">📆 {{ dayLabels.total }}</text>
        <input v-model.number="form.totalDays" type="number" class="input" placeholder="例如：30" />
      </view>
      <view v-if="config.useStayDays" class="form-group">
        <text class="label">📅 {{ dayLabels.counted }}</text>
        <input v-model.number="form.ownerDays" type="number" class="input" placeholder="0" />
      </view>

      <view v-for="fee in feeDefinitions" :key="fee.key" class="form-group">
        <text class="label">{{ fee.emoji || '💰' }} {{ fee.name }}（元）</text>
        <input v-model="form.fees[fee.key]" type="digit" class="input" placeholder="0.00" />
      </view>

      <view v-if="config.rules.length > 1" class="form-group">
        <text class="label">📐 这次使用哪套分配方案？</text>
        <picker mode="selector" :range="ruleNames" :value="ruleIndex" @change="onRuleChange">
          <view class="picker"><text class="picker-label">{{ ruleNames[ruleIndex] || "请选择规则" }}</text><text class="iconfont icon-a-icon-down picker-arrow"></text></view>
        </picker>
      </view>

      <view class="button-group">
        <button class="btn btn-primary" :loading="calculating" :disabled="calculating || saving || !calculator.id || (showResult && hasSaved)" @tap="calculateBill">{{ showResult && hasSaved ? '修改金额后可重新计算' : '查看分摊结果' }}</button>
      </view>

      <text v-if="errorMessage" class="error">{{ errorMessage }}</text>

      <view v-if="showResult" class="result">
        <view class="result-header">
          <text class="result-title">💰 费用分摊结果</text>
          <text class="save-status" :class="{ saved: hasSaved }">{{ hasSaved ? '✓ 账单已保存' : '预览结果 · 尚未保存' }}</text>
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
          <button class="btn btn-primary" :loading="saving" :disabled="saving || hasSaved" @tap="saveBill">{{ hasSaved ? '已保存' : '确认保存账单' }}</button>
        </view>
        <button class="poster-button" @tap="generatePoster">生成分享海报</button>
      </view>
    </view>

    <SharePoster ref="sharePosterRef" :data="posterData" />
  </view>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { onLoad, onShow } from "@dcloudio/uni-app";
import dayjs from "dayjs";
import CustomHeader from "@/components/CustomHeader/CustomHeader.vue";
import SharePoster from "@/components/SharePoster/SharePoster.vue";
import api from "@/api/costSharingApi.js";
import { getFeeDefinitions, getFeeAmounts, getBillFees, getDayLabels } from "@/utils/formula.js";

const calculator = ref({});
const config = computed(() => calculator.value.config || { fees: [], members: [], rules: [], useStayDays: false });
const feeDefinitions = computed(() => getFeeDefinitions(config.value));
const dayLabels = computed(() => getDayLabels(config.value));

const currentYear = dayjs().year();
const yearOptions = Array.from({ length: Math.min(10, currentYear - 2020 + 1) }, (_, i) => `${currentYear - i}年`);
const monthOptions = Array.from({ length: 12 }, (_, i) => `${i + 1}月`);
const yearIndex = ref(0);
const monthIndex = ref(dayjs().month());
const ruleIndex = ref(0);

const form = ref({
  year: currentYear,
  month: dayjs().month() + 1,
  totalDays: "",
  ownerDays: 0,
  fees: {},
});

const resultBill = ref(null);
const shares = computed(() => resultBill.value?.shares || []);
const calculating = ref(false);
const saving = ref(false);
const hasSaved = ref(false);
const previewRequest = ref(null);
const showResult = ref(false);
const errorMessage = ref("");
const sharePosterRef = ref(null);

const ruleNames = computed(() => config.value.rules.map((rule) => rule.name));
const totalAmount = computed(() => Number(resultBill.value?.totalAmount || 0));
let inputVersion = 0;
watch([form, ruleIndex], () => {
  inputVersion += 1;
  showResult.value = false;
  hasSaved.value = false;
  previewRequest.value = null;
}, { deep: true, flush: "sync" });

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

const viewCompanions = () => uni.navigateTo({ url: `/pages/companions/index?id=${calculator.value.id}` });

const viewBillList = () => {
  uni.navigateTo({
    url: `/pages/billList/index?calculatorId=${calculator.value.id}&name=${encodeURIComponent(calculator.value.name || "")}`,
  });
};

const calculateBill = async () => {
  if (calculating.value || saving.value || !calculator.value.id) return;
  if (showResult.value && hasSaved.value) return;
  calculating.value = true;
  errorMessage.value = "";
  showResult.value = false;
  const ruleName = config.value.rules[ruleIndex.value]?.name;
  const version = inputVersion;
  try {
    const request = {
      calculatorId: calculator.value.id,
      year: form.value.year,
      month: form.value.month,
      fees: getFeeAmounts(config.value, form.value.fees),
      totalDays: config.value.useStayDays ? form.value.totalDays : null,
      days: config.value.useStayDays ? form.value.ownerDays : null,
      ruleName,
    };
    const results = await api.bill.preview({ ...request, config: config.value });
    if (version === inputVersion) {
      previewRequest.value = request;
      resultBill.value = {
        ...request,
        fees: feeDefinitions.value.map((fee) => ({ ...fee, amount: request.fees[fee.key] })),
        shares: results,
        totalAmount: Number(results.reduce((sum, share) => sum + share.amount, 0).toFixed(2)),
      };
      hasSaved.value = false;
      showResult.value = true;
    }
  } catch (error) {
    if (version === inputVersion) errorMessage.value = error.message || "计算失败，请重试";
  } finally {
    calculating.value = false;
  }
};

const saveBill = async () => {
  if (saving.value || hasSaved.value || !showResult.value || !previewRequest.value) return;
  saving.value = true;
  errorMessage.value = "";
  const version = inputVersion;
  try {
    const bill = await api.bill.save(previewRequest.value);
    if (version === inputVersion) {
      resultBill.value = bill;
      hasSaved.value = true;
    } else {
      uni.showToast({ title: "上一笔账单已保存，可在列表查看", icon: "none" });
    }
  } catch (error) {
    errorMessage.value = error.message || "保存失败，请重试";
  } finally {
    saving.value = false;
  }
};

const posterData = computed(() => ({
  month: resultBill.value?.month,
  fees: getBillFees(resultBill.value),
  shares: shares.value.map((share) => ({
    ...share,
    amount: share.amount.toFixed(2),
  })),
  totalAmount: totalAmount.value.toFixed(2),
}));

const generatePoster = () => {
  sharePosterRef.value.show();
};

const requestedId = ref("");
const refreshing = ref(false);
const refreshCalculator = async () => {
  if (!requestedId.value || refreshing.value) return;
  refreshing.value = true;
  try {
    calculator.value = await api.calculator.getById(requestedId.value);
  } catch (error) {
    calculator.value = {};
    resultBill.value = null;
    showResult.value = false;
    errorMessage.value = error.message;
    console.error("加载计算器失败", error);
  } finally { refreshing.value = false; }
};
onLoad((options) => { requestedId.value = options?.id || ""; refreshCalculator(); });
onShow(() => { refreshCalculator(); });
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  padding: 24rpx 24rpx 80rpx;
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 50%, #ffecd2 100%);
}
.header-action {
  padding: 8rpx 12rpx;
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  background: #fafbfc;
  border: 1rpx solid #e8f0f5;
  border-radius: 24rpx;
  padding: 20rpx 24rpx;
  color: #5a7c9a;
  font-size: 30rpx;
}
.picker-label { flex: 1; min-width: 0; line-height: 46rpx; }
.picker-arrow { display: flex; align-items: center; justify-content: center; flex: 0 0 32rpx; width: 32rpx; height: 32rpx; color: #8ba1af; font-size: 24rpx; line-height: 1; }
.input {
  height: 88rpx;
  line-height: 88rpx;
  padding: 0 24rpx;
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
.bill-guide { color: #7894a4; font-size: 24rpx; line-height: 1.6; margin-bottom: 32rpx; padding: 22rpx; background: rgba(255,255,255,.6); border-radius: 20rpx; }
.bill-guide-title { display: block; color: #4b7488; font-size: 28rpx; font-weight: 600; margin-bottom: 8rpx; }
.save-status { display: block; text-align: center; color: #ab8c64; font-size: 24rpx; margin: -12rpx 0 20rpx; }
.save-status.saved { color: #568e80; }
.poster-button { margin-top: 20rpx; background: transparent; color: #6a8c9c; border: 1rpx solid #c8dbe0; border-radius: 40rpx; font-size: 28rpx; line-height: 76rpx; }
.poster-button::after { border: none; }
.calculator-toolbar { display: flex; justify-content: space-between; align-items: center; gap: 16rpx; margin: -26rpx 16rpx 28rpx; padding: 20rpx 24rpx; border-radius: 24rpx; background: rgba(255,255,255,.65); }
.access-label { color: #708c99; font-size: 24rpx; }
.toolbar-actions { display: flex; gap: 16rpx; }
.toolbar-button { margin: 0; padding: 0 22rpx; line-height: 64rpx; border-radius: 16rpx; background: #f5f9fa; color: #4a7485; font-size: 25rpx; }
.toolbar-button::after { border: none; }
.bills-button { background: #e4f2f0; color: #386d6c; font-weight: 600; }
</style>
