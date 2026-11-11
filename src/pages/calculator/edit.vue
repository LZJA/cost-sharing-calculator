<template>
  <view class="container">
    <CustomHeader :title="calculatorId ? '编辑计算器' : '新增计算器'" />

    <view class="form-container">
      <view class="form-group">
        <text class="label">名称</text>
        <input v-model="form.name" class="input" placeholder="例如：共同开支、周末出游" />
      </view>
      <view class="form-group">
        <text class="label">描述</text>
        <input v-model="form.description" class="input" placeholder="一句话说明" />
      </view>
      <view class="form-group">
        <text class="label">头像（emoji）</text>
        <input v-model="form.avatar" class="input" placeholder="📒" />
      </view>

      <view class="section-title">人员</view>
      <text class="hint">改名字不会影响已经记过的账单，旧账仍显示当时的名字。</text>
      <view v-for="(member, index) in form.config.members" :key="member.key" class="member-row">
        <input v-model="member.emoji" class="input emoji-input" placeholder="👤" />
        <input v-model="member.name" class="input" placeholder="名字" />
        <text class="remove" @tap="removeMember(index)">删除</text>
      </view>
      <button class="btn btn-secondary small" @tap="addMember">＋ 添加人员</button>

      <view class="section-title">费用项</view>
      <text class="hint">定义要分摊的费用，例如房租、网费、聚餐。金额在记账时填写，改名不影响旧账。</text>
      <view v-for="(fee, index) in form.config.fees" :key="fee.key" class="member-row">
        <input v-model="fee.emoji" class="input emoji-input" placeholder="💰" />
        <input v-model="fee.name" class="input" placeholder="费用名称" />
        <text class="remove" @tap="removeFee(index)">删除</text>
      </view>
      <button class="btn btn-secondary small" @tap="addFee">＋ 添加费用项</button>

      <view class="form-group switch-row">
        <text class="label">启用天数比例</text>
        <switch :checked="form.config.useStayDays" color="#ff9a9e" @change="onStayDaysChange" />
      </view>
      <view v-if="form.config.useStayDays">
        <text class="hint">份额可乘以参与天数占比；「按天数占比均摊」还会除以总人数。开启「只取一部分」时按该比例取费用，剩余费用另加步骤分摊；关闭时按最终份额分完整项费用。</text>
        <view class="form-group">
          <text class="label">总天数字段名称</text>
          <input v-model="form.config.dayLabels.total" class="input" placeholder="例如：周期总天数" />
        </view>
        <view class="form-group">
          <text class="label">参与天数字段名称</text>
          <input v-model="form.config.dayLabels.counted" class="input" placeholder="例如：使用天数、入住天数" />
        </view>
      </view>

      <view class="section-title">分账规则</view>
      <text class="hint">
        每条规则由若干步骤组成。分摊是把一项费用按份额分给指定的人；转移是把某人已经分到的钱按比例转给其他人。
      </text>

      <view v-for="(rule, ruleIndex) in form.config.rules" :key="ruleIndex" class="rule-card">
        <view class="member-row">
          <input v-model="rule.name" class="input" placeholder="规则名称，例如：普通分账" />
          <text class="remove" @tap="removeRule(ruleIndex)">删除</text>
        </view>

        <view v-for="(step, stepIndex) in rule.steps" :key="stepIndex" class="step-card">
          <view class="member-row">
            <picker
              mode="selector"
              :range="stepTypeLabels"
              :value="step.type === 'transfer' ? 1 : 0"
              @change="(e) => changeStepType(step, e.detail.value)"
            >
              <view class="picker">{{ step.type === "transfer" ? "转移" : "分摊" }}</view>
            </picker>
            <text class="remove" @tap="rule.steps.splice(stepIndex, 1)">删除</text>
          </view>

          <template v-if="step.type === 'share'">
            <picker
              mode="selector"
              :range="sourceLabels"
              :value="sourceIndex(step.source)"
              @change="(e) => (step.source = feeSources[Number(e.detail.value)].value)"
            >
              <view class="picker">费用：{{ sourceLabel(step.source) }}</view>
            </picker>
            <view class="member-row">
              <text class="hint inline">只取一部分，剩下的留给后面的步骤</text>
              <switch :checked="!!step.partial" color="#ff9a9e" @change="(e) => (step.partial = e.detail.value)" />
            </view>
            <view v-for="(share, shareIndex) in step.shares" :key="shareIndex" class="share-row">
              <picker
                class="member-picker"
                mode="selector"
                :range="memberNames"
                :value="memberIndex(share.member)"
                @change="(e) => (share.member = form.config.members[e.detail.value].key)"
              >
                <view class="picker">{{ memberName(share.member) }}</view>
              </picker>
              <input v-model.number="share.weight" type="digit" class="input weight-input" placeholder="份额" />
              <picker
                v-if="form.config.useStayDays"
                class="basis-picker"
                mode="selector"
                :range="basisOptions.map((item) => item.label)"
                :value="basisIndex(share.basis)"
                @change="(e) => (share.basis = basisOptions[Number(e.detail.value)].value)"
              >
                <view class="picker">{{ basisOptions[basisIndex(share.basis)].label }}</view>
              </picker>
              <text class="remove" @tap="step.shares.splice(shareIndex, 1)">✕</text>
            </view>
            <text class="link" @tap="addShare(step)">＋ 添加分配对象</text>
          </template>

          <template v-else>
            <picker
              mode="selector"
              :range="memberNames"
              :value="memberIndex(step.from)"
              @change="(e) => (step.from = form.config.members[e.detail.value].key)"
            >
              <view class="picker">转出：{{ memberName(step.from) }}</view>
            </picker>
            <view class="member-row">
              <text class="hint inline">转出比例（0 到 1，0.5 就是一半）</text>
              <input v-model.number="step.ratio" type="digit" class="input weight-input" placeholder="0.5" />
            </view>
            <view v-for="(receiver, receiverIndex) in step.to" :key="receiverIndex" class="share-row">
              <picker
                class="member-picker"
                mode="selector"
                :range="memberNames"
                :value="memberIndex(receiver.member)"
                @change="(e) => (receiver.member = form.config.members[e.detail.value].key)"
              >
                <view class="picker">{{ memberName(receiver.member) }}</view>
              </picker>
              <input v-model.number="receiver.weight" type="digit" class="input weight-input" placeholder="份额" />
              <text class="remove" @tap="step.to.splice(receiverIndex, 1)">✕</text>
            </view>
            <text class="link" @tap="addReceiver(step)">＋ 添加接收人</text>
          </template>
        </view>
        <text class="link" @tap="addStep(rule)">＋ 添加步骤</text>
      </view>
      <button class="btn btn-secondary small" @tap="addRule">＋ 添加规则</button>

      <view class="section-title">试算</view>
      <text class="hint">填一组样例金额，确认规则算得对再保存。这里不算进账单。</text>
      <view v-if="form.config.rules.length > 1" class="form-group">
        <picker mode="selector" :range="ruleNames" :value="previewRuleIndex" @change="onPreviewRuleChange">
          <view class="picker">规则：{{ ruleNames[previewRuleIndex] || "请选择" }}</view>
        </picker>
      </view>
      <view v-for="fee in form.config.fees" :key="fee.key" class="form-group">
        <text class="label">{{ fee.emoji || '💰' }} {{ fee.name || '未命名费用' }}（元）</text>
        <input v-model="preview.fees[fee.key]" type="digit" class="input" placeholder="0.00" />
      </view>
      <view v-if="form.config.useStayDays">
        <view class="form-group">
          <text class="label">{{ dayLabels.total }}</text>
          <input v-model.number="preview.totalDays" type="number" class="input" placeholder="例如：30" />
        </view>
        <view class="form-group">
          <text class="label">{{ dayLabels.counted }}</text>
          <input v-model.number="preview.ownerDays" type="number" class="input" placeholder="例如：15" />
        </view>
      </view>
      <button class="btn btn-secondary small" :loading="previewLoading" :disabled="previewLoading" @tap="runPreview">试算</button>
      <text v-if="previewError" class="error">{{ previewError }}</text>
      <view v-for="share in previewResult" :key="share.key" class="result-item">
        <text class="result-label">{{ share.emoji }} {{ share.name }}</text>
        <text class="result-value">{{ share.amount.toFixed(2) }} 元</text>
      </view>

      <view class="button-group">
        <button v-if="calculatorId" class="btn btn-secondary" @tap="removeCalculator">删除</button>
        <button class="btn btn-primary" @tap="save">保存</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import CustomHeader from "@/components/CustomHeader/CustomHeader.vue";
import api from "@/api/costSharingApi.js";
import { createMemberKey, createFeeKey, emptyCalculatorConfig, getFeeDefinitions, getFeeSources, getFeeAmounts, getDayLabels } from "@/utils/formula.js";

const calculatorId = ref(null);
const form = ref({
  name: "",
  description: "",
  avatar: "📒",
  background: "",
  enableBackground: true,
  config: emptyCalculatorConfig(),
});

const stepTypeLabels = ["分摊", "转移"];
const basisOptions = [
  { value: "", label: "固定" },
  { value: "days/totalDays", label: "按天数占比" },
  { value: "days/totalDays/memberCount", label: "按天数占比均摊" },
];
const basisIndex = (basis) => Math.max(0, basisOptions.findIndex((item) => item.value === basis));
const feeSources = computed(() => getFeeSources(form.value.config));
const sourceLabels = computed(() => feeSources.value.map((item) => item.label));
const dayLabels = computed(() => getDayLabels(form.value.config));

const memberNames = computed(() =>
  form.value.config.members.map((member) => member.name || "未命名")
);
const ruleNames = computed(() => form.value.config.rules.map((rule) => rule.name || "未命名"));

const memberName = (key) => {
  const member = form.value.config.members.find((item) => item.key === key);
  return member ? member.name || "未命名" : "选择人员";
};
const memberIndex = (key) => {
  const index = form.value.config.members.findIndex((item) => item.key === key);
  return index < 0 ? 0 : index;
};
const sourceIndex = (source) => {
  const index = feeSources.value.findIndex((item) => item.value === source);
  return index < 0 ? 0 : index;
};
const sourceLabel = (source) => feeSources.value.find((item) => item.value === source)?.label || "费用已删除，请重新选择";

const addFee = () => form.value.config.fees.push({ key: createFeeKey(), name: "", emoji: "💰" });
const removeFee = (index) => {
  if (form.value.config.fees.length <= 1) {
    uni.showToast({ title: "至少保留一项费用", icon: "none" });
    return;
  }
  const fee = form.value.config.fees[index];
  const used = form.value.config.rules.some((rule) => rule.steps.some((step) =>
    step.type === "share" && [fee.key, `remainder:${fee.key}`].includes(step.source)));
  if (used) {
    uni.showToast({ title: "请先调整引用此费用的分摊步骤", icon: "none" });
    return;
  }
  form.value.config.fees.splice(index, 1);
};

const addMember = () => {
  form.value.config.members.push({ key: createMemberKey(), name: "", emoji: "👤" });
};
const removeMember = (index) => {
  if (form.value.config.members.length <= 2) {
    uni.showToast({ title: "至少保留两个人", icon: "none" });
    return;
  }
  const removed = form.value.config.members[index].key;
  form.value.config.members.splice(index, 1);
  form.value.config.rules.forEach((rule) => {
    rule.steps.forEach((step) => {
      if (step.shares) step.shares = step.shares.filter((share) => share.member !== removed);
      if (step.to) step.to = step.to.filter((share) => share.member !== removed);
      if (step.from === removed) step.from = "";
    });
  });
};

const onStayDaysChange = (event) => {
  const usesStayDays = form.value.config.rules.some((rule) =>
    rule.steps.some((step) => step.type === "share" && (step.shares || []).some((share) =>
      ["days/totalDays", "days/totalDays/memberCount"].includes(share.basis)))
  );
  if (!event.detail.value && usesStayDays) {
    // 让 switch 回到当前配置，保留原规则，避免静默改变分摊比例。
    form.value.config.useStayDays = false;
    setTimeout(() => { form.value.config.useStayDays = true; }, 0);
    uni.showToast({ title: "请先把规则中的天数比例改为固定", icon: "none" });
    return;
  }
  form.value.config.useStayDays = event.detail.value;
};

const blankShare = () => ({
  member: form.value.config.members[0]?.key || "",
  weight: 1,
  basis: "",
});

const addRule = () => {
  form.value.config.rules.push({
    name: "",
    steps: [
      {
        type: "share",
        source: "total",
        partial: false,
        shares: form.value.config.members.map((member) => ({ member: member.key, weight: 1, basis: "" })),
      },
    ],
  });
};
const removeRule = (index) => {
  if (form.value.config.rules.length <= 1) {
    uni.showToast({ title: "至少保留一条规则", icon: "none" });
    return;
  }
  form.value.config.rules.splice(index, 1);
};

const addStep = (rule) => {
  rule.steps.push({
    type: "share",
    source: "total",
    partial: false,
    shares: [blankShare()],
  });
};
const changeStepType = (step, value) => {
  if (value === 1 || value === "1") {
    step.type = "transfer";
    step.from = step.from || form.value.config.members[0]?.key || "";
    step.ratio = step.ratio ?? 0.5;
    step.to = step.to || [blankShare()];
  } else {
    step.type = "share";
    step.source = step.source || "total";
    step.partial = !!step.partial;
    step.shares = step.shares || [blankShare()];
  }
};
const addShare = (step) => step.shares.push(blankShare());
const addReceiver = (step) => step.to.push(blankShare());

const preview = ref({ fees: {}, totalDays: 30, ownerDays: 0 });
const previewRuleIndex = ref(0);
const previewResult = ref([]);
const previewError = ref("");
let previewVersion = 0;
watch([() => form.value.config, preview, previewRuleIndex], () => {
  previewVersion += 1;
  previewResult.value = [];
  previewError.value = "";
}, { deep: true, flush: "sync" });

const onPreviewRuleChange = (event) => {
  previewRuleIndex.value = Number(event.detail.value);
};

const previewLoading = ref(false);
const runPreview = async () => {
  if (previewLoading.value) return;
  previewLoading.value = true;
  previewError.value = "";
  previewResult.value = [];
  const version = previewVersion;
  try {
    const ruleName = form.value.config.rules[previewRuleIndex.value]?.name;
    const result = await api.bill.preview({
      config: form.value.config,
      ruleName,
      fees: getFeeAmounts(form.value.config, preview.value.fees),
      totalDays: form.value.config.useStayDays ? preview.value.totalDays : null,
      days: form.value.config.useStayDays ? preview.value.ownerDays : null,
    });
    if (version === previewVersion) previewResult.value = result;
  } catch (error) {
    if (version === previewVersion) previewError.value = error.message;
  } finally {
    previewLoading.value = false;
  }
};

const validate = () => {
  if (!form.value.name.trim()) return "请填写名称";
  if (form.value.config.fees.some((fee) => !fee.name.trim())) return "费用名称不能为空";
  const feeNames = form.value.config.fees.map((fee) => fee.name.trim());
  if (new Set(feeNames).size !== feeNames.length) return "费用名称不能重复";
  if (form.value.config.useStayDays && (!form.value.config.dayLabels.total.trim() || !form.value.config.dayLabels.counted.trim())) return "天数字段名称不能为空";
  if (form.value.config.members.some((member) => !member.name.trim())) return "人员名字不能为空";
  if (form.value.config.rules.some((rule) => !rule.name.trim())) return "规则名字不能为空";
  const names = form.value.config.rules.map((rule) => rule.name.trim());
  if (new Set(names).size !== names.length) return "规则名字不能重复";
  for (const rule of form.value.config.rules) {
    if (!rule.steps.length) return `规则「${rule.name}」没有任何步骤`;
  }
  return "";
};

const save = async () => {
  const message = validate();
  if (message) {
    uni.showToast({ title: message, icon: "none" });
    return;
  }
  const payload = {
    name: form.value.name.trim(),
    description: form.value.description,
    avatar: form.value.avatar,
    background: form.value.background,
    enableBackground: form.value.enableBackground,
    config: form.value.config,
  };
  try {
    if (calculatorId.value) {
      await api.calculator.update(calculatorId.value, payload);
    } else {
      await api.calculator.create(payload);
    }
    uni.showToast({ title: "已保存", icon: "success" });
    setTimeout(() => uni.navigateBack(), 600);
  } catch (error) {
    console.error("保存计算器失败", error);
  }
};

const removeCalculator = () => {
  uni.showModal({
    title: "删除计算器",
    content: "已有账单的计算器不能删除。确定删除？",
    success: async (res) => {
      if (!res.confirm) return;
      try {
        await api.calculator.delete(calculatorId.value);
        uni.showToast({ title: "已删除", icon: "success" });
        setTimeout(() => uni.navigateBack(), 600);
      } catch (error) {
        console.error("删除计算器失败", error);
      }
    },
  });
};

onLoad(async (options) => {
  if (!options?.id) return;
  calculatorId.value = options.id;
  try {
    const calculator = await api.calculator.getById(options.id);
    form.value = {
      name: calculator.name,
      description: calculator.description || "",
      avatar: calculator.avatar || "📒",
      background: calculator.background || "",
      enableBackground: calculator.enableBackground !== false,
      config: { ...calculator.config, fees: getFeeDefinitions(calculator.config), dayLabels: getDayLabels(calculator.config) },
    };
  } catch (error) {
    console.error("加载计算器失败", error);
  }
});
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 24rpx 24rpx 80rpx;
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 50%, #ffecd2 100%);
}
.form-container {
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(10px);
  border-radius: 30rpx;
  padding: 40rpx;
}
.form-group {
  margin-bottom: 32rpx;
}
.label,
.section-title {
  display: block;
  margin-bottom: 16rpx;
  color: #5a7c9a;
  font-weight: 600;
  line-height: 1.5;
}
.section-title {
  margin-top: 48rpx;
  font-size: 34rpx;
}
.hint {
  display: block;
  color: #7c95aa;
  font-size: 24rpx;
  margin-bottom: 16rpx;
  line-height: 1.6;
}
.hint.inline {
  flex: 1;
  margin-bottom: 0;
}
.input,
.picker {
  box-sizing: border-box;
  background: #fafbfc;
  border: 1rpx solid #e8f0f5;
  border-radius: 24rpx;
  color: #5a7c9a;
  font-size: 30rpx;
}
.input {
  width: 100%;
  min-width: 0;
  height: 88rpx;
  line-height: 88rpx;
  padding: 0 24rpx;
}
.picker {
  min-height: 88rpx;
  padding: 20rpx 24rpx;
  line-height: 46rpx;
  overflow-wrap: break-word;
}
.member-row,
.share-row,
.switch-row {
  display: flex;
  gap: 16rpx;
  align-items: center;
  margin-bottom: 16rpx;
}
.share-row {
  flex-wrap: wrap;
}
.member-row .input,
.share-row .input {
  flex: 1 1 0;
  width: 0;
}
.member-row .emoji-input {
  flex: 0 0 88rpx;
  width: 88rpx;
  padding: 0 8rpx;
  text-align: center;
}
.member-row > picker,
.member-picker {
  flex: 1 1 0;
  min-width: 0;
}
.share-row .weight-input,
.member-row .weight-input {
  flex: 0 0 112rpx;
  width: 112rpx;
  padding: 0 16rpx;
}
.basis-picker {
  flex: 0 0 100%;
  min-width: 0;
  order: 1;
}
.switch-row {
  justify-content: space-between;
  margin-top: 24rpx;
}
.switch-row .label {
  flex: 1;
  min-width: 0;
  margin-bottom: 0;
}
.member-row switch,
.switch-row switch {
  flex-shrink: 0;
}
.remove,
.link {
  color: #ff6b9d;
  font-size: 26rpx;
  padding: 8rpx;
}
.remove {
  flex-shrink: 0;
  line-height: 44rpx;
  white-space: nowrap;
}
.rule-card,
.step-card {
  background: rgba(255, 255, 255, 0.55);
  border-radius: 24rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}
.step-card {
  background: rgba(255, 255, 255, 0.8);
}
.button-group {
  display: flex;
  gap: 24rpx;
  margin-top: 48rpx;
}
.btn {
  flex: 1;
  border-radius: 40rpx;
  font-size: 32rpx;
  font-weight: 600;
  border: none;
  min-height: 80rpx;
  line-height: 80rpx;
  padding: 0 24rpx;
  &::after {
    border: none;
  }
}
.btn.small {
  margin: 8rpx 0 24rpx;
}
.btn-primary {
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);
  color: #fff;
}
.btn-secondary {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  color: #fff;
}
.error {
  display: block;
  color: #ff6b6b;
  font-size: 26rpx;
  margin: 12rpx 0;
}
.result-item {
  display: flex;
  justify-content: space-between;
  padding: 16rpx 8rpx;
  color: #5a7c9a;
}
</style>
