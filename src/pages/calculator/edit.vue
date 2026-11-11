<template>
  <view class="container">
    <CustomHeader :title="calculatorId ? '编辑计算器' : '新增计算器'" />

    <view class="form-container">
      <view class="form-group">
        <text class="label">名称</text>
        <input v-model="form.name" class="input" placeholder="例如：李子的分账计算器" />
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

      <view class="form-group switch-row">
        <text class="label">需要填写居住天数</text>
        <switch :checked="form.config.useStayDays" color="#ff9a9e" @change="onStayDaysChange" />
      </view>
      <text v-if="form.config.useStayDays" class="hint">
        分摊时可以选「按居住天数」，份额会再乘以 居住天数 / 总天数。
      </text>

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
              @change="(e) => (step.source = FEE_SOURCES[e.detail.value].value)"
            >
              <view class="picker">费用：{{ sourceLabel(step.source) }}</view>
            </picker>
            <view class="member-row">
              <text class="hint inline">只取一部分，剩下的留给后面的步骤</text>
              <switch :checked="!!step.partial" color="#ff9a9e" @change="(e) => (step.partial = e.detail.value)" />
            </view>
            <view v-for="(share, shareIndex) in step.shares" :key="shareIndex" class="share-row">
              <picker
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
                mode="selector"
                :range="['固定', '按居住天数']"
                :value="share.basis === 'days/totalDays' ? 1 : 0"
                @change="(e) => (share.basis = e.detail.value === 1 ? 'days/totalDays' : '')"
              >
                <view class="picker">{{ share.basis === "days/totalDays" ? "按居住天数" : "固定" }}</view>
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
      <view class="share-row">
        <input v-model.number="preview.waterBill" type="digit" class="input" placeholder="水费" />
        <input v-model.number="preview.electricBill" type="digit" class="input" placeholder="电费" />
        <input v-model.number="preview.gasBill" type="digit" class="input" placeholder="燃气费" />
      </view>
      <view v-if="form.config.useStayDays" class="share-row">
        <input v-model.number="preview.totalDays" type="number" class="input" placeholder="总天数" />
        <input v-model.number="preview.ownerDays" type="number" class="input" placeholder="居住天数" />
      </view>
      <button class="btn btn-secondary small" @tap="runPreview">试算</button>
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
import { computed, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import CustomHeader from "@/components/CustomHeader/CustomHeader.vue";
import api from "@/api/costSharingApi.js";
import { FEE_SOURCES, calculateShares, createMemberKey, emptyCalculatorConfig } from "@/utils/formula.js";

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
const sourceLabels = FEE_SOURCES.map((item) => item.label);

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
  const index = FEE_SOURCES.findIndex((item) => item.value === source);
  return index < 0 ? 0 : index;
};
const sourceLabel = (source) => FEE_SOURCES.find((item) => item.value === source)?.label || "选择费用";

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

const preview = ref({ waterBill: "", electricBill: "", gasBill: "", totalDays: 30, ownerDays: 0 });
const previewRuleIndex = ref(0);
const previewResult = ref([]);
const previewError = ref("");

const onPreviewRuleChange = (event) => {
  previewRuleIndex.value = Number(event.detail.value);
};

const runPreview = () => {
  previewError.value = "";
  previewResult.value = [];
  try {
    const ruleName = form.value.config.rules[previewRuleIndex.value]?.name;
    previewResult.value = calculateShares(form.value.config, ruleName, preview.value);
  } catch (error) {
    previewError.value = error.message;
  }
};

const validate = () => {
  if (!form.value.name.trim()) return "请填写名称";
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
      config: calculator.config,
    };
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
}
.hint.inline {
  flex: 1;
  margin-bottom: 0;
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
.member-row,
.share-row,
.switch-row {
  display: flex;
  gap: 16rpx;
  align-items: center;
  margin-bottom: 16rpx;
}
.member-row .input,
.share-row .input,
.member-row .picker,
.share-row .picker {
  flex: 1;
}
.emoji-input {
  flex: 0 0 110rpx;
  text-align: center;
}
.weight-input {
  flex: 0 0 140rpx;
}
.remove,
.link {
  color: #ff6b9d;
  font-size: 26rpx;
  padding: 8rpx;
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
