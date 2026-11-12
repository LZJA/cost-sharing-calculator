<template>
  <view class="container">
    <CustomHeader :title="calculatorId ? '编辑计算器' : '新增计算器'" />

    <view class="setup-progress">
      <view v-for="(title, index) in wizardTitles" :key="title" class="progress-item" :class="{ active: wizardStep === index, done: wizardStep > index }" @tap="goWizardStep(index)">
        <text class="progress-number">{{ wizardStep > index ? '✓' : index + 1 }}</text><text>{{ title }}</text>
      </view>
    </view>
    <view class="form-container">
      <view v-show="wizardStep === 0">
      <text class="page-step-title">先给计算器起个名字</text>
      <text class="hint">例如合租开支、周末出游。创建后，每次只需填写金额，就能按保存的方式分账。</text>
      <view class="form-group">
        <text class="label">名称</text>
        <input v-model="form.name" class="input" placeholder="例如：共同开支、周末出游" />
      </view>
      <view class="form-group">
        <text class="label">描述（可不填）</text>
        <input v-model="form.description" class="input" placeholder="一句话说明" />
      </view>
      <view class="form-group">
        <text class="label">头像（emoji）</text>
        <input v-model="form.avatar" class="input" placeholder="📒" />
      </view>

      </view>
      <view v-show="wizardStep === 1">
      <text class="page-step-title">这笔开支，涉及谁和哪些费用？</text>
      <view class="section-title">参与人员</view>
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

      </view>
      <view v-show="wizardStep === 2">
      <text class="page-step-title">选一种分法，再试算确认</text>
      <view class="setup-summary"><text>{{ form.config.members.length }} 位参与人 · {{ form.config.fees.length }} 项费用</text><text class="subtle-link" @tap="goWizardStep(1)">修改人员和费用</text></view>
      <view v-if="form.config.useStayDays" class="optional-settings">
        <view class="optional-heading" @tap="daySettingsOpen = !daySettingsOpen"><text>记账时的天数输入</text><text class="optional-toggle">{{ daySettingsOpen ? '收起' : '修改名称' }} <text class="iconfont picker-arrow" :class="daySettingsOpen ? 'icon-icon-top' : 'icon-a-icon-down'"></text></text></view>
        <view v-show="daySettingsOpen">
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

        </view>
      </view>
      <text v-if="form.config.rules.length > 1" class="hint">同一个计算器有多套方案时，记账时选择其中一套。</text>

      <view v-for="(rule, ruleIndex) in form.config.rules" :key="ruleIndex" class="rule-card">
        <view v-if="form.config.rules.length > 1 || detailed(rule)" class="rule-heading">
          <text class="field-label">方案名称</text>
          <text v-if="form.config.rules.length > 1" class="remove" @tap="removeRule(ruleIndex)">删除方案</text>
        </view>
        <input v-if="form.config.rules.length > 1 || detailed(rule)" v-model="rule.name" class="input" placeholder="例如：日常分配、特殊分配" />
        <text class="field-label spaced">这套方案怎么分？</text>
        <view class="preset-grid">
          <view v-for="preset in allocationPresets" :key="preset.value" class="preset-card" :class="{ selected: rulePreset(rule) === preset.value }" @tap="choosePreset(rule, preset.value)">
            <text class="preset-icon">{{ preset.icon }}</text><text class="choice-title">{{ preset.label }}</text><text class="choice-desc">{{ preset.description }}</text>
          </view>
        </view>

        <view v-for="(step, stepIndex) in rule.steps" :key="stepIndex" class="step-card">
          <view class="step-heading">
            <view class="step-number">{{ stepIndex + 1 }}</view>
            <picker v-if="detailed(rule)" class="step-action" mode="selector" :range="stepTypeLabels" :value="step.type === 'transfer' ? 1 : 0"
              @change="(e) => changeStepType(step, e.detail.value)">
              <view class="step-title">{{ step.type === 'transfer' ? '替某人分担费用' : '分配一笔费用' }} <text class="iconfont icon-a-icon-down picker-arrow"></text></view>
            </picker>
            <text v-else class="step-title step-action">{{ rulePreset(rule) === 'days' ? (stepIndex === 0 ? '先按天数计算' : '再分剩余费用') : '分配总费用' }}</text>
            <text v-if="detailed(rule)" class="remove" @tap="rule.steps.splice(stepIndex, 1)">删除</text>
          </view>

          <template v-if="step.type === 'share'">
            <view v-if="detailed(rule)">
            <view class="fee-selection-heading"><text class="field-label">要分哪些费用？<text class="multi-select-label">可多选</text></text><text v-if="stepSources(rule, stepIndex).length" class="subtle-link" @tap="selectAllSources(rule, stepIndex)">全选待分配费用</text></view>
            <view class="fee-source-options">
              <view v-for="source in stepSources(rule, stepIndex)" :key="source.value" class="fee-source-option" :class="{ active: isFeeSelected(step, source.value) }" @tap="toggleFeeSource(rule, stepIndex, source.value)">
                <text class="fee-source-check">{{ isFeeSelected(step, source.value) ? '✓' : '＋' }}</text><text class="fee-source-label">{{ source.emoji || '💰' }} {{ source.label }}</text>
              </view>
              <view v-for="source in invalidSelectedSources(rule, stepIndex)" :key="source" class="fee-source-option unavailable" @tap="toggleFeeSource(rule, stepIndex, source)"><text>× {{ sourceLabel(source) }}（不可用，点此移除）</text></view>
            </view>
            <text v-if="validSourceSelection(rule, stepIndex)" class="mode-explanation">{{ selectedFeesText(rule, stepIndex) }}，合并金额后使用下方同一种分法。</text>
            <text v-else class="error">请至少选择一项可用费用。已在前面分配的费用不能再次选择。</text>

            <text class="field-label spaced">这一步分多少？</text>
            <view class="choice-grid">
              <view class="choice-card" :class="{ selected: !step.partial }" @tap="changeScope(step, false)">
                <text class="choice-title">全部分完</text><text class="choice-desc">这笔费用不留到下一步</text>
              </view>
              <view class="choice-card" :class="{ selected: !!step.partial }" @tap="changeScope(step, true)">
                <text class="choice-title">先分出一部分</text><text class="choice-desc">剩余费用稍后再安排</text>
              </view>
            </view>

            <text class="field-label spaced">怎么分？</text>
            <view class="choice-grid">
              <template v-if="!step.partial">
                <view class="choice-card" :class="{ selected: allocationMode(step) === 'equal' }" @tap="changeMode(rule, stepIndex, 'equal')">
                  <text class="choice-title">平均分</text><text class="choice-desc">每个人一样多，无需填数字</text>
                </view>
                <view class="choice-card" :class="{ selected: allocationMode(step) === 'ratio' }" @tap="changeMode(rule, stepIndex, 'ratio')">
                  <text class="choice-title">按份数分</text><text class="choice-desc">例如 1 份 : 2 份</text>
                </view>
              </template>
              <template v-else>
                <view class="choice-card" :class="{ selected: allocationMode(step) === 'percent' }" @tap="changeMode(rule, stepIndex, 'percent')">
                  <text class="choice-title">按百分比分</text><text class="choice-desc">直接填 20%、50% 等比例</text>
                </view>
                <view class="choice-card" :class="{ selected: allocationMode(step) === 'days' }" @tap="changeMode(rule, stepIndex, 'days')">
                  <text class="choice-title">按天数折算</text><text class="choice-desc">按参与天数计算承担金额</text>
                </view>
              </template>
            </view>
            </view>
            <view v-if="rulePreset(rule) === 'days' && stepIndex === 1">
              <text class="field-label">剩余费用怎么分？</text>
              <view class="choice-grid">
                <view class="choice-card" :class="{ selected: allocationMode(step) === 'equal' }" @tap="changeMode(rule, stepIndex, 'equal')"><text class="choice-title">平均分</text><text class="choice-desc">每个人承担一样多</text></view>
                <view class="choice-card" :class="{ selected: allocationMode(step) === 'ratio' }" @tap="changeMode(rule, stepIndex, 'ratio')"><text class="choice-title">按份数分</text><text class="choice-desc">例如 1 份 : 2 份</text></view>
              </view>
            </view>
            <text class="mode-explanation">{{ modeExplanation(step) }}</text>
          </template>

          <template v-else>
            <text class="field-label">替谁分担？</text>
            <picker mode="selector" :range="memberNames" :value="memberIndex(step.from)"
              @change="(e) => changeTransferFrom(step, form.config.members[Number(e.detail.value)].key)">
              <view class="picker"><text class="picker-label">{{ memberName(step.from) }}</text><text class="iconfont icon-a-icon-down picker-arrow"></text></view>
            </picker>
            <text class="field-label spaced">替对方承担已分配金额的多少？</text>
            <view class="number-box transfer-percent">
              <input :value="percentageValue(step, 'ratio')" type="digit" class="number-input"
                placeholder="例如：50" @input="(e) => setPercentage(step, 'ratio', e.detail.value)" />
              <text class="number-unit">%</text>
            </view>
            <text class="mode-explanation">填 50%，表示替对方承担一半，对方自己的金额会减少一半。</text>
            <text class="field-label spaced">这部分由其他人怎么分担？</text>
            <view class="choice-grid">
              <view class="choice-card" :class="{ selected: allocationMode(step) === 'equal' }" @tap="changeMode(rule, stepIndex, 'equal')">
                <text class="choice-title">平均分担</text><text class="choice-desc">每个人承担一样多</text>
              </view>
              <view class="choice-card" :class="{ selected: allocationMode(step) === 'ratio' }" @tap="changeMode(rule, stepIndex, 'ratio')">
                <text class="choice-title">按份数分担</text><text class="choice-desc">例如 1 份 : 2 份</text>
              </view>
            </view>
          </template>

          <view v-if="rulePreset(rule) === 'days' && stepIndex === 0" class="day-person-select">
            <text class="field-label">谁按天数计算？</text>
            <picker mode="selector" :range="memberNames" :value="memberIndex(step.shares[0]?.member)" @change="(e) => changeDayPerson(rule, form.config.members[Number(e.detail.value)].key)">
              <view class="picker"><text class="picker-label">{{ memberName(step.shares[0]?.member) }}</text><text class="iconfont icon-a-icon-down picker-arrow"></text></view>
            </picker>
          </view>
          <view v-else>
          <view class="participants-heading">
            <text class="field-label">{{ step.type === 'transfer' ? '谁来承担这部分？' : '谁参与这一步？' }}</text>
            <text class="selected-count">已选 {{ allocationRows(step).length }} 人</text>
          </view>
          <view class="person-chips">
            <view v-for="member in selectableMembers(step)" :key="member.key" class="person-chip"
              :class="{ active: isParticipant(step, member.key) }" @tap="toggleParticipant(step, member.key)">
              <text class="person-check">{{ isParticipant(step, member.key) ? '✓' : '＋' }}</text>
              <text>{{ member.emoji || '👤' }} {{ memberName(member.key) }}</text>
            </view>
          </view>

          </view>
          <view v-for="(share, shareIndex) in allocationRows(step)" :key="share.member" class="allocation-person">
            <view v-if="!(rulePreset(rule) === 'days' && stepIndex === 0)" class="allocation-person-header">
              <text class="allocation-name">{{ memberName(share.member) }}</text>
              <text v-if="allocationMode(step) === 'equal'" class="share-percent">平均承担 {{ fractionText(step, share) }}%</text>
            </view>
            <view v-if="allocationMode(step) === 'ratio' || allocationMode(step) === 'custom'" class="allocation-field">
              <view class="allocation-copy"><text class="field-label">承担几份</text><text class="field-tip">份数越多，承担越多</text></view>
              <view class="number-box">
                <input v-model.number="share.weight" type="digit" class="number-input" placeholder="例如：1" />
                <text class="number-unit">份</text>
              </view>
            </view>
            <view v-if="allocationMode(step) === 'percent'" class="allocation-field">
              <view class="allocation-copy"><text class="field-label">占这笔费用的比例</text><text class="field-tip">填百分数，不用换成小数</text></view>
              <view class="number-box">
                <input :value="percentageValue(share, 'weight')" type="digit" class="number-input" placeholder="例如：50"
                  @input="(e) => setPercentage(share, 'weight', e.detail.value)" />
                <text class="number-unit">%</text>
              </view>
            </view>
            <template v-if="allocationMode(step) === 'days' || allocationMode(step) === 'custom'">
              <text class="field-label">承担金额怎么算？</text>
              <picker mode="selector" :range="basisLabels(step)" :value="basisChoice(step, share)"
                @change="(e) => changeBasis(step, share, Number(e.detail.value))">
                <view class="picker"><text class="picker-label">{{ basisLabels(step)[basisChoice(step, share)] }}</text><text class="iconfont icon-a-icon-down picker-arrow"></text></view>
              </picker>
              <text class="field-tip day-formula">{{ dayFormula(share) }}</text>
              <template v-if="allocationMode(step) === 'days'">
                <view v-if="showMultiplier(share)" class="allocation-field">
                  <text class="field-label">上述结果再乘以</text>
                  <view class="number-box"><input v-model.number="share.weight" type="digit" class="number-input" placeholder="1" /><text class="number-unit">倍</text></view>
                </view>
                <text v-else class="subtle-link" @tap="expandMultiplier(share)">需要调整倍数？</text>
              </template>
            </template>
            <text class="allocation-example">{{ personExample(step, share) }}</text>
          </view>

          <view class="allocation-summary" :class="{ invalid: stepError(step) }">
            <text class="summary-title">{{ allocationSummary(step) }}</text>
            <text v-if="stepError(step)" class="error">{{ stepError(step) }}</text>
            <text v-else class="summary-note">{{ step.type === 'transfer' ? transferExplanation(step) : '上方以本笔费用 100 元示意，小数尾差以实际试算结果为准。' }}</text>
          </view>
          <view v-if="step.type === 'share' && step.partial && needsRemainder(step)" class="remainder-tip">
            <text v-if="remainderStepIndex(rule, stepIndex) >= 0">剩余费用已安排在第 {{ remainderStepIndex(rule, stepIndex) + 1 }} 步，可以继续调整那一步的参与人和份数。</text>
            <template v-else><text>这一步还有剩余费用，需要安排由谁承担。</text><button class="remainder-button" @tap="addRemainderStep(rule, stepIndex)">＋ 分配剩余费用</button></template>
          </view>
        </view>
        <button v-if="detailed(rule)" class="add-step-button" @tap="addStep(rule)">＋ 添加下一步</button>
      </view>
      <view class="optional-heading" @tap="multiplePlansOpen = !multiplePlansOpen"><text>需要多套分配方案？</text><text>{{ multiplePlansOpen ? '收起' : '展开' }} <text class="iconfont picker-arrow" :class="multiplePlansOpen ? 'icon-icon-top' : 'icon-a-icon-down'"></text></text></view>
      <view v-show="multiplePlansOpen"><text class="hint">例如工作日与周末使用不同分法，记账时可以选择。</text><button class="btn btn-secondary small" @tap="addRule">＋ 新增另一套分配方案</button></view>

      <view class="section-title">用一组金额检查结果</view>
      <text class="hint">这里仅验证分配方式，不会生成账单。试算无误后再保存计算器。</text>
      <view v-if="form.config.rules.length > 1" class="form-group">
        <picker mode="selector" :range="ruleNames" :value="previewRuleIndex" @change="onPreviewRuleChange">
          <view class="picker"><text class="picker-label">规则：{{ ruleNames[previewRuleIndex] || "请选择" }}</text><text class="iconfont icon-a-icon-down picker-arrow"></text></view>
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
      <button class="btn trial-button" :loading="previewLoading" :disabled="previewLoading" @tap="runPreview">试算分摊金额</button>
      <text v-if="previewError" class="error">{{ previewError }}</text>
      <view v-if="previewResult.length" class="preview-summary"><text>试算结果 · 不会生成账单</text><text>合计承担 {{ previewResult.reduce((sum, share) => sum + share.amount, 0).toFixed(2) }} 元</text></view>
      <view v-for="share in previewResult" :key="share.key" class="result-item">
        <text class="result-label">{{ share.emoji }} {{ share.name }}</text>
        <text class="result-value">{{ share.amount.toFixed(2) }} 元</text>
      </view>

      <text v-if="formError" class="error form-error">{{ formError }}</text>
      </view>
      <view class="wizard-footer">
        <button v-if="wizardStep > 0" class="back-step-button" @tap="goWizardStep(wizardStep - 1)">上一步</button>
        <button v-if="wizardStep === 2" class="btn btn-primary save-calculator-button" :loading="saving" :disabled="saving" @tap="save">{{ saving ? '正在保存…' : '保存计算器' }}</button>
        <button v-if="wizardStep < 2" class="btn btn-primary" @tap="goWizardStep(wizardStep + 1)">{{ wizardStep === 0 ? '下一步：费用和人员' : '下一步：选择分法' }}</button>
      </view>
      <text v-if="calculatorId && wizardStep === 0" class="delete-calculator" @tap="removeCalculator">删除这个计算器</text>
    </view>
  </view>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import CustomHeader from "@/components/CustomHeader/CustomHeader.vue";
import api from "@/api/costSharingApi.js";
import { allocationMode, allocationRows, setAllocationMode, sampleFraction, percentText, parsePercentage, nextFeeSource, selectedFeeSources, sourceGroupKey, setFeeSources, expandedFeeSources, stepSourceOptions, DAY_BASIS, DAY_PER_MEMBER } from "@/utils/ruleEditor.js";
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

const wizardStep = ref(0);
const formError = ref("");
const saving = ref(false);
const wizardTitles = ["基本信息", "费用和人员", "分配方式"];
const daySettingsOpen = ref(false);
const multiplePlansOpen = ref(false);
const presetOverrides = ref(new Map());
const allocationPresets = [
  { value: "equal", icon: "⚖️", label: "大家平均分", description: "选好参与人，每个人一样多" },
  { value: "ratio", icon: "📊", label: "按份数分", description: "例如 1 份和 2 份，承担 1:2" },
  { value: "days", icon: "📅", label: "先按天数，再分余款", description: "一人按天数计算，其他人分余款" },
  { value: "custom", icon: "⚙️", label: "自定义安排", description: "每项费用分别设置，或替人分担" },
];
const singleSource = (step) => selectedFeeSources(step).length === 1 ? selectedFeeSources(step)[0] : null;
const rulePreset = (rule) => {
  if (presetOverrides.value.has(rule)) return presetOverrides.value.get(rule);
  const steps = rule.steps || [];
  if (steps.length === 1 && steps[0].type === "share" && singleSource(steps[0]) === "total" && !steps[0].partial) {
    const mode = allocationMode(steps[0]);
    if (["equal", "ratio"].includes(mode)) return mode;
  }
  if (steps.length === 2 && steps[0].type === "share" && singleSource(steps[0]) === "total"
      && steps[0].partial && allocationMode(steps[0]) === "days" && steps[0].shares.length === 1
      && steps[1].type === "share" && singleSource(steps[1]) === "remainder:total" && !steps[1].partial) return "days";
  return "custom";
};
const detailed = (rule) => rulePreset(rule) === "custom";
const choosePreset = (rule, preset) => {
  if (rulePreset(rule) === preset) return;
  if (preset === "custom") {
    if (rule.steps.length === 1 && rule.steps[0].type === "share" && singleSource(rule.steps[0]) === "total" && !rule.steps[0].partial) {
      const original = rule.steps[0];
      rule.steps = form.value.config.fees.map((fee) => ({ ...original, source: fee.key, sources: [fee.key], shares: original.shares.map((row) => ({ ...row })) }));
    }
    presetOverrides.value.set(rule, preset);
    return;
  }
  const apply = () => {
    const step = newShareStep();
    rule.steps = [step];
    setAllocationMode(step, preset, form.value.config.members);
    presetOverrides.value.set(rule, preset);
    if (preset === "days") {
      form.value.config.useStayDays = true;
      addRemainderStep(rule, 0);
    }
  };
  const hasSpecialSetup = rule.steps.length > 1 || rule.steps.some((step) => step.type === "transfer" || step.partial || allocationRows(step).some((row) => row.basis || Number(row.weight) !== 1));
  if (hasSpecialSetup) {
    uni.showModal({ title: "切换分法", content: "这会重新设置当前方案的分配步骤，其他方案和历史账单不受影响。是否继续？", success: (result) => { if (result.confirm) apply(); } });
  } else apply();
};
const changeDayPerson = (rule, key) => {
  const first = rule.steps[0];
  first.shares = [{ ...first.shares[0], member: key }];
  const remaining = rule.steps[1];
  const previous = new Map(remaining.shares.map((row) => [row.member, row]));
  remaining.shares = form.value.config.members.filter((member) => member.key !== key)
    .map((member) => previous.get(member.key) || { member: member.key, weight: 1, basis: "" });
};
const goWizardStep = (index) => {
  if (index > 0) {
    const error = validateBasic();
    if (error) { wizardStep.value = 0; uni.showToast({ title: error, icon: "none" }); return; }
  }
  if (index > 1) {
    const error = validatePeopleFees();
    if (error) { wizardStep.value = 1; uni.showToast({ title: error, icon: "none" }); return; }
  }
  wizardStep.value = index;
  uni.pageScrollTo?.({ scrollTop: 0, duration: 0 });
};

const stepTypeLabels = ["分配一笔费用", "替某人分担费用"];
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
  form.value.config.members.map((member, index) => member.name || `成员${index + 1}`)
);
const ruleNames = computed(() => form.value.config.rules.map((rule) => rule.name || "未命名"));

const memberName = (key) => {
  const member = form.value.config.members.find((item) => item.key === key);
  return member ? member.name || `成员${form.value.config.members.indexOf(member) + 1}` : "请选择人员";
};
const memberIndex = (key) => {
  const index = form.value.config.members.findIndex((item) => item.key === key);
  return index < 0 ? 0 : index;
};
const sourceIndex = (source) => {
  const index = feeSources.value.findIndex((item) => item.value === source);
  return index < 0 ? 0 : index;
};
const sourceLabel = (source) => feeSources.value.find((item) => item.value === source)?.label || (source?.startsWith("remainder:") ? "费用组的剩余金额" : "费用已删除，请重新选择");
const stepSources = (rule, index) => stepSourceOptions(rule, index, form.value.config.fees);
const isFeeSelected = (step, source) => expandedFeeSources(step, form.value.config.fees).includes(source);
const validSourceSelection = (rule, index) => {
  const selected = expandedFeeSources(rule.steps[index], form.value.config.fees);
  const options = new Set(stepSources(rule, index).map((option) => option.value));
  return selected.length > 0 && new Set(selected).size === selected.length && selected.every((source) => options.has(source));
};
const invalidSelectedSources = (rule, index) => {
  const options = new Set(stepSources(rule, index).map((option) => option.value));
  return [...new Set(expandedFeeSources(rule.steps[index], form.value.config.fees))].filter((source) => !options.has(source));
};
const selectedFeesText = (rule, index) => {
  const options = new Map(stepSources(rule, index).map((source) => [source.value, source.label]));
  const selected = expandedFeeSources(rule.steps[index], form.value.config.fees);
  return `已选 ${selected.length} 项：${selected.map((source) => options.get(source) || sourceLabel(source)).join(' + ')}`;
};
const sourceAnchors = ref(new Map());
const updateFeeSelection = (rule, index, sources) => {
  const step = rule.steps[index];
  const oldOrigin = sourceGroupKey(selectedFeeSources(step)) || sourceAnchors.value.get(step);
  setFeeSources(step, sources);
  const newOrigin = sourceGroupKey(selectedFeeSources(step));
  if (!newOrigin) {
    if (oldOrigin) sourceAnchors.value.set(step, oldOrigin);
    return;
  }
  sourceAnchors.value.set(step, newOrigin);
  if (!oldOrigin || oldOrigin === newOrigin) return;
  // 费用组选项变动时同步更新后续余款引用，也覆盖余款再次与其他费用合并的情况。
  const replacements = new Map([[`remainder:${oldOrigin}`, `remainder:${newOrigin}`]]);
  rule.steps.slice(index + 1).forEach((later) => {
    if (later.type !== "share") return;
    const original = selectedFeeSources(later);
    const changed = original.map((source) => replacements.get(source) || source);
    if (original.every((source, i) => source === changed[i])) return;
    const before = sourceGroupKey(original);
    setFeeSources(later, changed);
    const after = sourceGroupKey(changed);
    if (later.partial && before && after && before !== after) replacements.set(`remainder:${before}`, `remainder:${after}`);
  });
};
const toggleFeeSource = (rule, index, source) => {
  const selected = [...new Set(expandedFeeSources(rule.steps[index], form.value.config.fees))];
  updateFeeSelection(rule, index, selected.includes(source) ? selected.filter((item) => item !== source) : [...selected, source]);
};
const selectAllSources = (rule, index) => updateFeeSelection(rule, index, stepSources(rule, index).map((source) => source.value));

const addFee = () => form.value.config.fees.push({ key: createFeeKey(), name: "", emoji: "💰" });
const removeFee = (index) => {
  if (form.value.config.fees.length <= 1) {
    uni.showToast({ title: "至少保留一项费用", icon: "none" });
    return;
  }
  const fee = form.value.config.fees[index];
  const used = form.value.config.rules.some((rule) => rule.steps.some((step) =>
    step.type === "share" && selectedFeeSources(step).some((source) => [fee.key, `remainder:${fee.key}`].includes(source))));
  if (used) {
    uni.showToast({ title: "请先调整引用此费用的分摊步骤", icon: "none" });
    return;
  }
  form.value.config.fees.splice(index, 1);
};

const addMember = () => {
  const targets = form.value.config.rules.flatMap((rule) => {
    const preset = rulePreset(rule);
    const rows = rule.steps.flatMap(allocationRows);
    const allSelected = form.value.config.members.every((member) => rows.some((row) => row.member === member.key));
    if (!allSelected) return [];
    if (["equal", "ratio"].includes(preset)) return [rule.steps[0].shares];
    if (preset === "days") return [rule.steps[1].shares];
    return [];
  });
  const member = { key: createMemberKey(), name: "", emoji: "👤" };
  form.value.config.members.push(member);
  targets.forEach((rows) => rows.push({ member: member.key, weight: 1, basis: "" }));
};
const removeMember = (index) => {
  if (form.value.config.members.length <= 2) {
    uni.showToast({ title: "至少保留两个人", icon: "none" });
    return;
  }
  const dayRules = form.value.config.rules.filter((rule) => rulePreset(rule) === "days");
  const removed = form.value.config.members[index].key;
  form.value.config.members.splice(index, 1);
  form.value.config.rules.forEach((rule) => {
    rule.steps.forEach((step) => {
      if (step.shares) step.shares = step.shares.filter((share) => share.member !== removed);
      if (step.to) step.to = step.to.filter((share) => share.member !== removed);
      if (step.from === removed) changeTransferFrom(step, form.value.config.members[0]?.key || "");
    });
  });
  dayRules.forEach((rule) => {
    if (!rule.steps[0].shares.length) changeDayPerson(rule, form.value.config.members[0].key);
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

const percentageDrafts = ref(new Map());
const expandedMultipliers = ref(new Set());
const percentageValue = (row, field) => percentageDrafts.value.get(row)?.[field] ?? percentText(row[field]);
const setPercentage = (row, field, text) => {
  percentageDrafts.value.set(row, { ...percentageDrafts.value.get(row), [field]: text });
  row[field] = parsePercentage(text);
};
const showMultiplier = (row) => Number(row.weight) !== 1 || expandedMultipliers.value.has(row);
const expandMultiplier = (row) => expandedMultipliers.value.add(row);
const selectableMembers = (step) => form.value.config.members.filter((member) => step.type !== "transfer" || member.key !== step.from);
const isParticipant = (step, key) => allocationRows(step).some((row) => row.member === key);
const toggleParticipant = (step, key) => {
  const rows = allocationRows(step);
  const mode = allocationMode(step);
  const index = rows.findIndex((row) => row.member === key);
  if (index >= 0) {
    if (rows.length === 1) {
      uni.showToast({ title: "这一步至少选择一人", icon: "none" });
      return;
    }
    rows.splice(index, 1);
  } else {
    rows.push({ member: key, weight: mode === "percent" ? 0 : 1,
      basis: mode === "days" ? DAY_PER_MEMBER : "" });
  }
  if (mode === "equal") rows.forEach((row) => { row.weight = 1; row.basis = ""; });
};
const changeTransferFrom = (step, key) => {
  step.from = key;
  step.to = (step.to || []).filter((row) => row.member !== key);
  if (!step.to.length) step.to = selectableMembers(step).map((member) => ({ member: member.key, weight: 1 }));
};
const changeScope = (step, partial) => {
  if (!!step.partial === partial) return;
  setAllocationMode(step, partial ? "percent" : "equal", form.value.config.members);
};
const changeMode = (rule, index, mode) => {
  const step = rule.steps[index];
  if (allocationMode(step) === mode) return;
  setAllocationMode(step, mode, form.value.config.members);
  if (mode === "days") {
    form.value.config.useStayDays = true;
    addRemainderStep(rule, index);
  }
};
const fractionText = (step, row) => percentText(sampleFraction(step, row, form.value.config.members.length));
const totalWeight = (step) => allocationRows(step).reduce((sum, row) => sum + (Number(row.weight) || 0), 0);
const needsRemainder = (step) => allocationRows(step).some((row) => !!row.basis) || totalWeight(step) < 1 - 1e-9;
const remainderStepIndex = (rule, index) => {
  const origin = sourceGroupKey(selectedFeeSources(rule.steps[index]));
  return origin ? rule.steps.findIndex((step, i) => i > index && step.type === "share" && selectedFeeSources(step).includes(`remainder:${origin}`)) : -1;
};
const addRemainderStep = (rule, index) => {
  if (remainderStepIndex(rule, index) >= 0) return;
  const step = rule.steps[index];
  const origin = sourceGroupKey(selectedFeeSources(step));
  if (!origin) { uni.showToast({ title: "请先选择要分配的费用", icon: "none" }); return; }
  const selected = new Set(allocationRows(step).map((row) => row.member));
  const others = form.value.config.members.filter((member) => !selected.has(member.key));
  rule.steps.splice(index + 1, 0, {
    type: "share", source: `remainder:${origin}`,
    partial: false, allocationMode: "equal",
    shares: (others.length ? others : form.value.config.members).map((member) => ({ member: member.key, weight: 1, basis: "" })),
  });
};
const basisValues = (step) => allocationMode(step) === "custom" ? ["", DAY_BASIS, DAY_PER_MEMBER] : [DAY_BASIS, DAY_PER_MEMBER];
const basisLabels = (step) => allocationMode(step) === "custom"
  ? ["固定份数", "按参与天数折算", "先按总人数均分，再按参与天数折算"]
  : ["按参与天数折算", "先按总人数均分，再按参与天数折算"];
const basisChoice = (step, row) => Math.max(0, basisValues(step).indexOf(row.basis || ""));
const changeBasis = (step, row, index) => {
  row.basis = basisValues(step)[index];
  if (row.basis) form.value.config.useStayDays = true;
};
const dayFormula = (row) => {
  if (!row.basis) return "按这里填写的固定份数分配。";
  const average = row.basis === DAY_PER_MEMBER ? ` ÷ 总人数（${form.value.config.members.length}人）` : "";
  const multiplier = Number(row.weight) === 1 ? "" : ` × ${row.weight ?? '待填'}倍`;
  return `本笔费用 × ${dayLabels.value.counted} ÷ ${dayLabels.value.total}${average}${multiplier}`;
};
const modeExplanation = (step) => {
  const mode = allocationMode(step);
  if (mode === "equal") return "选好参与人即可，每个人承担一样多。";
  if (mode === "ratio") return "这里填的是份数，不是金额。例如 1 份和 2 份，分别承担三分之一和三分之二。";
  if (mode === "percent") return "直接填写百分数，例如填 50 就是承担 50%，不需要输入 0.5。合计不足 100% 时，需继续分配剩余费用。";
  if (mode === "days") return `按「${dayLabels.value.counted} / ${dayLabels.value.total}」计算，剩余费用在下一步分配。`;
  return "这一步保留了原来的组合规则。下方可逐人调整份数和天数方式，也可选择上面的分法重新设置。";
};
const personExample = (step, row) => {
  const ratio = sampleFraction(step, row, form.value.config.members.length);
  const percent = percentText(ratio);
  if (step.type === "transfer") return `对方原先承担 100 元时，这一步替其承担约 ${(100 * Number(step.ratio || 0) * ratio).toFixed(2)} 元（分担部分的 ${percent}%）。`;
  const days = row.basis ? "，假设参与半个周期" : "";
  const target = step.type === "transfer" ? "待分担金额" : "本笔费用";
  const shares = allocationMode(step) === "ratio" ? `${row.weight ?? '待填'} 份 / 共 ${totalWeight(step)} 份，` : "";
  return `${shares}占 ${percent}% · ${target} 100 元时${days}，约承担 ${(ratio * 100).toFixed(2)} 元`;
};
const stepError = (step) => {
  const rows = allocationRows(step);
  const partial = step.type === "share" && step.partial;
  if (!rows.length) return "请至少选择一位参与人";
  if (step.type === "transfer" && (step.ratio == null || !Number.isFinite(Number(step.ratio)) || step.ratio < 0 || step.ratio > 1)) return "请填写 0% 到 100% 之间的分担比例";
  if (rows.some((row) => row.weight == null || row.weight === "" || !Number.isFinite(Number(row.weight)) || row.weight < 0)) return "请为每位参与人填写有效的份数或比例";
  if (!partial && totalWeight(step) <= 0) return "承担份数合计必须大于 0";
  if (partial) {
    const max = rows.reduce((sum, row) => sum + Number(row.weight) / (row.basis === DAY_PER_MEMBER ? form.value.config.members.length : 1), 0);
    if (max > 1 + 1e-9) return "承担比例合计不能超过 100%";
  }
  return "";
};
const allocationSummary = (step) => {
  if (step.type === "transfer") return `将 ${memberName(step.from)} 已分配金额的 ${percentText(step.ratio)}% 转由下方人员承担`;
  if (allocationMode(step) === "equal") return `${allocationRows(step).length} 人平均分，每人承担 ${percentText(1 / Math.max(1, allocationRows(step).length))}%`;
  if (!step.partial) return `共 ${totalWeight(step)} 份，按份数分完整笔费用`;
  if (allocationRows(step).some((row) => row.basis)) return "按天数先计算这一步，剩余费用继续安排";
  const percent = Number((totalWeight(step) * 100).toFixed(6));
  return percent > 100 ? `已分配 ${percent}%，超过了总费用` : `已分配 ${percent}%，剩余 ${Number((100 - percent).toFixed(6))}%`;
};
const transferExplanation = (step) => `${memberName(step.from)} 的金额会减少，这部分由 ${allocationRows(step).map((row) => memberName(row.member)).join('、') || '待选人员'}${allocationMode(step) === 'equal' ? '平均' : '按份数'}承担。`;

const newShareStep = (source = "total") => ({
  type: "share", source, partial: false, allocationMode: "equal",
  shares: form.value.config.members.map((member) => ({ member: member.key, weight: 1, basis: "" })),
});
const addRule = () => form.value.config.rules.push({ name: "", steps: [newShareStep()] });
const removeRule = (index) => {
  if (form.value.config.rules.length <= 1) {
    uni.showToast({ title: "至少保留一套方案", icon: "none" });
    return;
  }
  form.value.config.rules.splice(index, 1);
};
const addStep = (rule) => {
  const options = stepSources(rule, rule.steps.length).filter((option) => !option.optional);
  if (options.length) {
    const step = newShareStep(options[0].value);
    if (options.length > 1) setFeeSources(step, options.map((option) => option.value));
    rule.steps.push(step);
  }
  else {
    const from = form.value.config.members[0]?.key || "";
    rule.steps.push({ type: "transfer", from, ratio: 0.5, allocationMode: "equal",
      to: form.value.config.members.filter((member) => member.key !== from).map((member) => ({ member: member.key, weight: 1 })) });
    uni.showToast({ title: "费用已分完，可添加分担调整", icon: "none" });
  }
};
const changeStepType = (step, value) => {
  if (Number(value) === 1) {
    step.type = "transfer";
    step.partial = false;
    step.from = step.from || form.value.config.members[0]?.key || "";
    step.ratio = step.ratio ?? 0.5;
    step.to = step.to || selectableMembers(step).map((member) => ({ member: member.key, weight: 1 }));
    step.allocationMode = allocationRows(step).every((row) => row.weight === 1) ? "equal" : "ratio";
  } else {
    step.type = "share";
    const sources = selectedFeeSources(step);
    setFeeSources(step, sources.length ? sources : ["total"]);
    step.shares = step.shares || form.value.config.members.map((member) => ({ member: member.key, weight: 1, basis: "" }));
    step.allocationMode = undefined;
  }
};

const preview = ref({ fees: {}, totalDays: 30, ownerDays: 15 });
const previewRuleIndex = ref(0);
const previewResult = ref([]);
const previewError = ref("");
watch(() => form.value.config.fees.map((fee) => fee.key).join("|"), () => {
  const keys = form.value.config.fees.map((fee) => fee.key);
  if (keys.length && !keys.some((key) => preview.value.fees[key] !== undefined)) preview.value.fees[keys[0]] = 100;
}, { immediate: true });
let previewVersion = 0;
watch([() => form.value.config, preview, previewRuleIndex], () => {
  previewVersion += 1;
  previewResult.value = [];
  previewError.value = "";
  formError.value = "";
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

const validateBasic = () => !form.value.name.trim() ? "请先填写计算器名称" : "";
const validatePeopleFees = () => {
  if (form.value.config.members.length < 2) return "至少添加两位参与人";
  if (form.value.config.members.some((member) => !member.name.trim())) return "请填写每位参与人的名字";
  if (!form.value.config.fees.length) return "至少添加一项费用";
  if (form.value.config.fees.some((fee) => !fee.name.trim())) return "请填写每项费用的名称";
  const feeNames = form.value.config.fees.map((fee) => fee.name.trim());
  if (new Set(feeNames).size !== feeNames.length) return "费用名称不能重复";
  return "";
};
const validate = () => {
  const basic = validateBasic() || validatePeopleFees();
  if (basic) return basic;
  if (form.value.config.useStayDays && (!form.value.config.dayLabels.total.trim() || !form.value.config.dayLabels.counted.trim())) return "天数字段名称不能为空";
  if (form.value.config.rules.some((rule) => !rule.name.trim())) return "规则名字不能为空";
  const names = form.value.config.rules.map((rule) => rule.name.trim());
  if (new Set(names).size !== names.length) return "规则名字不能重复";
  for (const rule of form.value.config.rules) {
    if (!rule.steps.length) return `方案「${rule.name}」还没有分配步骤`;
    for (const [index, step] of rule.steps.entries()) {
      const error = stepError(step);
      if (error) return error;
      if (step.type === "share" && !validSourceSelection(rule, index)) return `第 ${index + 1} 步的费用不可再次分配，或尚未选择费用，请调整费用来源`;
    }
    const next = nextFeeSource(rule, form.value.config.fees);
    if (next) return `「${stepSources(rule, rule.steps.length).find((option) => option.value === next)?.label || sourceLabel(next)}」还没有分配，请安排剩余费用或添加下一步`;
  }
  return "";
};

const save = async () => {
  if (saving.value) return;
  const message = validate();
  if (message) {
    formError.value = message;
    uni.showToast({ title: "还有配置未完成，请查看下方提示", icon: "none" });
    return;
  }
  saving.value = true;
  formError.value = "";
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
    saving.value = false;
    formError.value = error.message || "保存失败，请重试";
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
    if (calculator.currentRole !== "OWNER") {
      uni.showToast({ title: "仅创建者可修改配置", icon: "none" });
      uni.redirectTo({ url: `/pages/calculator/index?id=${options.id}` });
      return;
    }
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
  padding: 24rpx 12rpx 80rpx;
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 50%, #ffecd2 100%);
}
.form-container {
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(10px);
  border-radius: 30rpx;
  padding: 24rpx;
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  min-height: 88rpx;
  padding: 20rpx 24rpx;
  line-height: 46rpx;
}
.picker-label {
  flex: 1;
  min-width: 0;
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

.rule-card {
  padding: 24rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.85);
}
.rule-heading,
.step-heading,
.participants-heading,
.allocation-person-header,
.allocation-field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}
.rule-heading { margin-bottom: 12rpx; }
.field-label {
  display: block;
  color: #49667e;
  font-size: 26rpx;
  font-weight: 600;
  line-height: 1.5;
  margin-bottom: 12rpx;
}
.field-label.spaced { margin-top: 28rpx; }
.rule-heading .field-label,
.participants-heading .field-label,
.allocation-field .field-label { margin-bottom: 0; }
.step-card {
  background: #fff;
  padding: 24rpx;
  margin-top: 24rpx;
  border: 1rpx solid #e4ecf2;
}
.step-heading { margin-bottom: 24rpx; }
.step-number {
  flex: 0 0 44rpx;
  height: 44rpx;
  line-height: 44rpx;
  border-radius: 14rpx;
  text-align: center;
  color: #fff;
  font-size: 26rpx;
  font-weight: 700;
  background: #638aa2;
}
.step-action { flex: 1; min-width: 0; }
.step-title {
  color: #3e5d73;
  font-size: 28rpx;
  font-weight: 700;
  line-height: 1.5;
}
.picker-arrow { display: inline-flex; align-items: center; justify-content: center; flex: 0 0 auto; width: 40rpx; height: 40rpx; color: #8ba1af; font-size: 32rpx; line-height: 1; }
.optional-toggle { display: inline-flex; align-items: center; }
.step-title .picker-arrow { margin-left: 8rpx; vertical-align: middle; }
.choice-grid { display: flex; gap: 12rpx; }
.choice-card {
  flex: 1 1 0;
  min-width: 0;
  padding: 18rpx 14rpx;
  background: #f7f9fb;
  border: 2rpx solid #edf1f5;
  border-radius: 18rpx;
}
.choice-card.selected { background: #eaf5f5; border-color: #76aaa9; }
.choice-title { display: block; color: #47677c; font-size: 26rpx; font-weight: 700; line-height: 1.5; }
.choice-desc { display: block; color: #7c909e; font-size: 22rpx; line-height: 1.5; margin-top: 6rpx; }
.choice-card.selected .choice-title { color: #326e70; }
.mode-explanation { display: block; margin-top: 16rpx; color: #748996; font-size: 23rpx; line-height: 1.7; }
.participants-heading { margin: 28rpx 0 14rpx; }
.selected-count { flex-shrink: 0; color: #8b9da8; font-size: 22rpx; }
.person-chips { display: flex; flex-wrap: wrap; gap: 12rpx; margin-bottom: 20rpx; }
.person-chip { padding: 12rpx 16rpx; border-radius: 16rpx; border: 2rpx solid #e7edf1; color: #7f929e; background: #fafbfc; font-size: 25rpx; line-height: 1.5; overflow-wrap: break-word; }
.person-chip.active { border-color: #aacbcb; background: #f0f7f7; color: #3d7275; }
.person-check { margin-right: 8rpx; font-weight: 700; }
.allocation-person { border-top: 1rpx solid #edf1f4; padding: 22rpx 0; }
.allocation-person-header { flex-wrap: wrap; margin-bottom: 12rpx; }
.allocation-name { color: #47657a; font-size: 28rpx; font-weight: 700; overflow-wrap: break-word; }
.share-percent { color: #4c8a89; font-size: 24rpx; }
.allocation-copy { flex: 1; min-width: 0; }
.field-tip { display: block; margin-top: 6rpx; color: #8a9ba5; font-size: 22rpx; line-height: 1.6; }
.number-box { display: flex; align-items: center; flex: 0 0 160rpx; min-width: 0; padding: 0 14rpx; height: 80rpx; border-radius: 16rpx; background: #f8fafb; border: 1rpx solid #dbe5eb; }
.number-input { flex: 1; width: 0; min-width: 0; height: 78rpx; line-height: 78rpx; font-size: 30rpx; color: #365e76; text-align: center; padding: 0; }
.number-unit { flex-shrink: 0; font-size: 24rpx; color: #8499a6; margin-left: 6rpx; }
.transfer-percent { width: 220rpx; }
.day-formula { margin: 12rpx 0; color: #648797; }
.subtle-link { display: block; color: #648ca2; font-size: 23rpx; line-height: 1.5; padding: 6rpx 0; }
.allocation-example { display: block; margin-top: 12rpx; color: #7b8f9c; font-size: 22rpx; line-height: 1.7; }
.allocation-summary { background: #f0f7f7; padding: 20rpx; border-radius: 16rpx; margin-top: 8rpx; }
.allocation-summary.invalid { background: #fff1f1; }
.summary-title { display: block; color: #487477; font-size: 25rpx; font-weight: 600; line-height: 1.6; }
.summary-note { display: block; color: #80949d; font-size: 22rpx; line-height: 1.6; margin-top: 8rpx; }
.remainder-tip { color: #9a7d53; font-size: 23rpx; line-height: 1.7; padding: 20rpx 0 0; }
.remainder-button,
.add-step-button { width: 100%; margin: 18rpx 0 0; padding: 16rpx; border-radius: 16rpx; border: 1rpx dashed #adc5cd; background: #f8fbfc; color: #5c8597; font-size: 25rpx; line-height: 1.6; }
.remainder-button::after,
.add-step-button::after { border: none; }

.setup-progress { display: flex; gap: 12rpx; margin: 0 0 28rpx; }
.progress-item { flex: 1; min-width: 0; display: flex; align-items: center; justify-content: center; gap: 8rpx; color: #8da0ad; font-size: 24rpx; line-height: 1.5; }
.progress-number { flex: 0 0 36rpx; height: 36rpx; line-height: 36rpx; border-radius: 50%; background: rgba(255,255,255,.65); text-align: center; color: #90a2ac; }
.progress-item.active { color: #426e83; font-weight: 700; }
.progress-item.active .progress-number { background: #668fa4; color: #fff; }
.progress-item.done { color: #558c87; }
.progress-item.done .progress-number { background: #e4f2ef; color: #558c87; }
.page-step-title { display: block; color: #3f6177; font-size: 36rpx; font-weight: 700; line-height: 1.5; margin-bottom: 18rpx; }
.wizard-footer { display: flex; align-items: stretch; gap: 20rpx; margin-top: 36rpx; padding-top: 28rpx; border-top: 1rpx solid #dce6e9; padding-bottom: env(safe-area-inset-bottom); }
.wizard-footer .btn { font-size: 28rpx; }
.back-step-button { flex: 0 0 154rpx; padding: 0 16rpx; background: #f3f7f9; color: #6a899a; font-size: 28rpx; line-height: 80rpx; border-radius: 40rpx; }
.back-step-button::after { border: none; }
.delete-calculator { display: block; text-align: center; color: #d18e94; font-size: 25rpx; margin-top: 32rpx; padding: 16rpx; }
.setup-summary { display: flex; flex-wrap: wrap; align-items: center; gap: 16rpx; justify-content: space-between; color: #7f94a0; font-size: 25rpx; margin-bottom: 20rpx; }
.preset-grid { display: flex; flex-wrap: wrap; gap: 12rpx; }
.preset-card { flex: 0 0 calc(50% - 6rpx); min-width: 0; box-sizing: border-box; padding: 20rpx 16rpx; background: #f7fafb; border: 2rpx solid #e5edf1; border-radius: 20rpx; }
.preset-card.selected { background: #eaf5f4; border-color: #75aaa6; }
.preset-card.selected .choice-title { color: #356e70; }
.preset-icon { display: block; font-size: 32rpx; line-height: 1.2; margin-bottom: 12rpx; }
.optional-settings { border: 1rpx solid rgba(255,255,255,.9); border-radius: 20rpx; padding: 0 20rpx; margin-bottom: 20rpx; background: rgba(255,255,255,.45); }
.optional-toggle { flex: 0 0 auto; white-space: nowrap; color: #4c7b8d; padding: 6rpx 0 6rpx 16rpx; }
.optional-heading { display: flex; justify-content: space-between; align-items: center; gap: 16rpx; color: #7691a2; font-size: 24rpx; line-height: 1.5; padding: 22rpx 0; }
.day-person-select { margin: 26rpx 0 16rpx; }
.preview-summary { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 12rpx; background: #edf6f4; padding: 20rpx; border-radius: 18rpx; color: #4d827e; font-size: 24rpx; line-height: 1.6; margin-top: 20rpx; }
.form-error { padding: 20rpx; background: #fff0f1; border-radius: 16rpx; }
.fee-selection-heading { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 8rpx; margin-bottom: 12rpx; }
.fee-selection-heading .field-label { margin-bottom: 0; }
.multi-select-label { color: #829ba7; font-size: 22rpx; font-weight: 400; margin-left: 12rpx; }
.fee-source-options { display: flex; flex-wrap: wrap; gap: 12rpx; }
.fee-source-option { display: flex; flex: 0 0 calc(50% - 6rpx); box-sizing: border-box; align-items: center; gap: 12rpx; max-width: 100%; min-width: 0; padding: 18rpx 14rpx; border: 2rpx solid #e4ecf0; border-radius: 16rpx; background: #fafcfd; color: #7b929f; font-size: 25rpx; line-height: 1.6; overflow-wrap: break-word; }
.fee-source-label { flex: 1; min-width: 0; word-break: normal; overflow-wrap: anywhere; }
.fee-source-option.active { border-color: #8fbab6; background: #edf7f5; color: #3e7773; }
.fee-source-check { flex: 0 0 32rpx; height: 32rpx; line-height: 32rpx; text-align: center; border: 1rpx solid #cbdcde; border-radius: 8rpx; background: #fff; }
.fee-source-option.active .fee-source-check { color: #fff; background: #6caaa3; border-color: #6caaa3; }
.fee-source-option.unavailable { color: #bd7780; background: #fff4f5; border-color: #efd4d8; }
.trial-button { width: 100%; background: #f4f8f9; color: #4d7d8c; border: 2rpx solid #cadfe2; font-size: 28rpx; margin-top: 16rpx; }
.save-calculator-button { background: #4e8587; color: #fff; box-shadow: 0 6rpx 16rpx rgba(65,116,119,.15); }
</style>
