import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { ref, computed, watch, nextTick } from "vue";
import dayjs from "dayjs";

const configHelpers = await import(`data:text/javascript;base64,${Buffer.from(
  readFileSync(new URL("../src/utils/formula.js", import.meta.url), "utf8")
).toString("base64")}`);

const ruleHelpers = await import(`data:text/javascript;base64,${Buffer.from(
  readFileSync(new URL("../src/utils/ruleEditor.js", import.meta.url), "utf8")
).toString("base64")}`);
const navigationHelpers = await import(`data:text/javascript;base64,${Buffer.from(
  readFileSync(new URL("../src/utils/navigation.js", import.meta.url), "utf8")
).toString("base64")}`);

// 在无小程序运行时的环境执行真实页面脚本，替换网络和平台边界。
function setup(file, bindings, exposed) {
  const source = readFileSync(new URL(`../${file}`, import.meta.url), "utf8")
    .match(/<script setup>([\s\S]*?)<\/script>/)[1]
    .replace(/^import\s[\s\S]*?from\s+["'][^"']+["'];?\s*/gm, "");
  const dependencies = {
    ref, computed, watch, nextTick, dayjs, ...configHelpers, ...ruleHelpers,
    onMounted() {}, onShow() {}, onLoad() {}, getCurrentInstance: () => null,
    defineExpose() {}, defineEmits: () => () => {},
    console: { error() {} }, ...bindings,
  };
  return new Function(...Object.keys(dependencies), `${source}\nreturn {${exposed}};`)(...Object.values(dependencies));
}

function calculator(api) {
  const page = setup("src/pages/calculator/index.vue", { api, uni: { showToast() {} } },
    "calculator, form, calculateBill, saveBill, saving, hasSaved, shares, showResult, posterData, errorMessage, calculating");
  page.calculator.value = { id: 1, config: { fees: [{ key: "rent", name: "房租", emoji: "🏠" }], members: [], rules: [{ name: "默认分账" }] } };
  page.form.value.fees.rent = "2.01";
  return page;
}

function editor(exposed, uni = {}) {
  return setup("src/pages/calculator/edit.vue", { uni: { showToast() {}, pageScrollTo() {},
    showModal: ({ success }) => success({ confirm: true }), ...uni } }, exposed);
}

function groupedEditor(exposed) {
  const page = editor(`form, ${exposed}`);
  page.form.value.name = "多项费用";
  page.form.value.config.members.forEach((member, i) => { member.name = `成员${i + 1}`; });
  page.form.value.config.fees = [
    { key: "water", name: "水费" }, { key: "electric", name: "电费" },
    { key: "gas", name: "燃气费" }, { key: "parking", name: "车位费" },
  ];
  return page;
}

test("水费分完后可以多选电费和车位费，未选的燃气费继续单独安排", () => {
  const page = groupedEditor("addStep, toggleFeeSource, stepSources, selectedFeesText, validate");
  const rule = page.form.value.config.rules[0];
  rule.steps[0].source = "water";
  page.addStep(rule);
  assert.deepEqual(rule.steps[1].sources, ["electric", "gas", "parking"]);
  assert.ok(page.stepSources(rule, 1).every((option) => option.value !== "water"));
  page.toggleFeeSource(rule, 1, "gas");
  assert.deepEqual(rule.steps[1].sources, ["electric", "parking"]);
  assert.equal(rule.steps[1].source, null);
  assert.match(page.selectedFeesText(rule, 1), /电费 \+ 车位费/);
  assert.match(page.validate(), /燃气费.*还没有分配/);
  page.addStep(rule);
  assert.equal(rule.steps[2].source, "gas");
  assert.equal(page.validate(), "");
});

test("多选部分分摊同步关联余款，清空后重新选也不会留下错误引用", () => {
  const page = groupedEditor("toggleFeeSource, selectAllSources, validSourceSelection, validate");
  const rule = page.form.value.config.rules[0];
  const first = rule.steps[0];
  ruleHelpers.setFeeSources(first, ["electric", "gas"]);
  first.partial = true;
  first.shares = [{ member: page.form.value.config.members[0].key, weight: 0.5 }];
  const oldKey = ruleHelpers.sourceGroupKey(first.sources);
  rule.steps.push({ type: "share", source: `remainder:${oldKey}`, partial: false,
    shares: [{ member: page.form.value.config.members[1].key, weight: 1 }] });
  page.toggleFeeSource(rule, 0, "parking");
  const newKey = ruleHelpers.sourceGroupKey(first.sources);
  assert.equal(rule.steps[1].source, `remainder:${newKey}`);
  assert.equal(rule.steps[1].shares[0].weight, 1);
  [...first.sources].forEach((source) => page.toggleFeeSource(rule, 0, source));
  assert.equal(page.validSourceSelection(rule, 0), false);
  page.selectAllSources(rule, 0);
  assert.equal(rule.steps[1].source, `remainder:${ruleHelpers.sourceGroupKey(first.sources)}`);
  assert.equal(page.validate(), "");
});

test("费用组的余款再次合并后，修改上游选择会同步更新整条余款链", () => {
  const page = groupedEditor("toggleFeeSource, validate");
  const members = page.form.value.config.members;
  const a = { type: "share", partial: true, sources: ["electric", "gas"], shares: [{ member: members[0].key, weight: 0.5 }] };
  const b = { type: "share", partial: true, sources: [`remainder:${ruleHelpers.sourceGroupKey(a.sources)}`, "parking"], shares: [{ member: members[1].key, weight: 0.5 }] };
  const c = { type: "share", partial: false, source: `remainder:${ruleHelpers.sourceGroupKey(b.sources)}`, shares: [{ member: members[1].key, weight: 1 }] };
  const rule = page.form.value.config.rules[0];
  rule.steps = [a, b, c];
  page.toggleFeeSource(rule, 0, "water");
  assert.equal(rule.steps[1].sources[0], `remainder:${ruleHelpers.sourceGroupKey(rule.steps[0].sources)}`);
  assert.equal(rule.steps[2].source, `remainder:${ruleHelpers.sourceGroupKey(rule.steps[1].sources)}`);
  assert.equal(page.validate(), "");
  assert.equal(ruleHelpers.sourceGroupKey(["electric", "gas"]), "group:8:electric3:gas");
  assert.equal(ruleHelpers.sourceGroupKey(["gas", "electric"]), "group:8:electric3:gas");
});

test("创建向导先检查基本信息和人员，返回修改不丢失费用", () => {
  const page = editor("form, wizardStep, goWizardStep");
  page.goWizardStep(2);
  assert.equal(page.wizardStep.value, 0);
  page.form.value.name = "共同开支";
  page.goWizardStep(1);
  assert.equal(page.wizardStep.value, 1);
  page.goWizardStep(2);
  assert.equal(page.wizardStep.value, 1);
  page.form.value.config.members.forEach((member, i) => { member.name = `成员${i}`; });
  page.form.value.config.fees[0].name = "网费";
  page.goWizardStep(2);
  assert.equal(page.wizardStep.value, 2);
  page.goWizardStep(1);
  assert.equal(page.form.value.config.fees[0].name, "网费");
});

test("公共导航读取实际胶囊位置，不同屏宽下右侧按钮均位于胶囊左边", () => {
  for (const [width, left, status, top] of [[320, 222, 20, 24], [375, 278, 20, 26], [430, 330, 44, 54]]) {
    const capsule = { left, top, height: 32 };
    const header = setup("src/components/CustomHeader/CustomHeader.vue", {
      ...navigationHelpers,
      defineProps: () => ({ title: "测试标题", showGlass: false }),
      onMounted: (callback) => callback(),
      uni: { getWindowInfo: () => ({ windowWidth: width, statusBarHeight: status }), getMenuButtonBoundingClientRect: () => capsule },
    }, "rightInset, statusBarHeight, navHeight");
    assert.ok(width - header.rightInset.value <= left - 8);
    assert.equal(header.statusBarHeight.value, status);
    assert.ok(header.navHeight.value >= 44);
  }
  assert.equal(navigationHelpers.navigationLayout({ windowWidth: 375, statusBarHeight: 20 }, null, true).rightInset, 104);
  assert.equal(navigationHelpers.navigationLayout({ windowWidth: 375, statusBarHeight: 20 }, null, false).rightInset, 0);
});

test("份数与百分比都有明确单位，百分比输入保持精度和输入中的小数点", () => {
  const page = editor("form, choosePreset, allocationMode, personExample, setPercentage, percentageValue, changeScope, stepError");
  const rule = page.form.value.config.rules[0];
  page.choosePreset(rule, "ratio");
  const step = rule.steps[0];
  step.shares[1].weight = 2;
  assert.equal(page.allocationMode(step), "ratio");
  assert.match(page.personExample(step, step.shares[0]), /1 份 \/ 共 3 份/);
  assert.match(page.personExample(step, step.shares[0]), /33.33 元/);
  page.changeScope(step, true);
  page.setPercentage(step.shares[0], "weight", "33.34");
  assert.equal(step.shares[0].weight, 0.3334);
  page.setPercentage(step.shares[0], "weight", "12.");
  assert.equal(page.percentageValue(step.shares[0], "weight"), "12.");
  page.setPercentage(step.shares[0], "weight", "100");
  page.setPercentage(step.shares[1], "weight", "50");
  assert.match(page.stepError(step), /不能超过 100%/);
  page.setPercentage(step.shares[0], "weight", "");
  assert.match(page.stepError(step), /填写有效/);
});

test("天数常用方案自动分余款，更换计费人和增减成员保持其余人的份数", () => {
  const page = editor("form, choosePreset, rulePreset, changeDayPerson, addMember, removeMember");
  const rule = page.form.value.config.rules[0];
  page.addMember();
  assert.equal(rule.steps[0].shares.length, 3);
  page.choosePreset(rule, "days");
  assert.equal(page.rulePreset(rule), "days");
  assert.equal(rule.steps.length, 2);
  assert.equal(rule.steps[0].shares[0].basis, ruleHelpers.DAY_PER_MEMBER);
  assert.equal(rule.steps[1].source, "remainder:total");
  rule.steps[1].shares[1].weight = 3;
  const secondKey = page.form.value.config.members[1].key;
  const thirdKey = page.form.value.config.members[2].key;
  page.changeDayPerson(rule, secondKey);
  assert.equal(rule.steps[0].shares[0].member, secondKey);
  assert.equal(rule.steps[1].shares.find((row) => row.member === thirdKey).weight, 3);
  assert.ok(rule.steps[1].shares.every((row) => row.member !== secondKey));
  page.addMember();
  assert.equal(rule.steps[1].shares.length, 3);
  page.removeMember(1);
  assert.equal(rule.steps[0].shares.length, 1);
  assert.ok(page.form.value.config.members.some((member) => member.key === rule.steps[0].shares[0].member));
});

test("复杂方案切换常用分法可以取消，查看旧规则不会改写金额配置", () => {
  const page = editor("form, choosePreset, rulePreset", { showModal: ({ success }) => success({ confirm: false }) });
  const rule = page.form.value.config.rules[0];
  rule.steps[0].shares[1].weight = 2;
  const before = JSON.stringify(rule);
  assert.equal(page.rulePreset(rule), "ratio");
  page.choosePreset(rule, "equal");
  assert.equal(JSON.stringify(rule), before);
  page.choosePreset(rule, "custom");
  assert.equal(page.rulePreset(rule), "custom");
  assert.equal(rule.steps[0].source, page.form.value.config.fees[0].key);
  assert.equal(rule.steps[0].shares[1].weight, 2);
});

test("三等分转成百分比恰好合计 100%，分担比例直接填写 50 而非 0.5", () => {
  const members = ["a", "b", "c"].map((key) => ({ key }));
  const step = { type: "share", partial: false, shares: members.map((member) => ({ member: member.key, weight: 1 })) };
  ruleHelpers.setAllocationMode(step, "percent", members);
  assert.deepEqual(step.shares.map((row) => row.weight), [0.3334, 0.3333, 0.3333]);
  const page = editor("form, changeStepType, setPercentage, personExample, stepError");
  const transfer = page.form.value.config.rules[0].steps[0];
  page.changeStepType(transfer, 1);
  page.setPercentage(transfer, "ratio", "50");
  assert.equal(transfer.ratio, 0.5);
  assert.match(page.personExample(transfer, transfer.to[0]), /50.00 元/);
  assert.equal(page.stepError(transfer), "");
});

test("费用来源按前面的步骤过滤，保存前指出漏分和重复分配", () => {
  const page = editor("form, choosePreset, stepSources, validate, setPercentage");
  page.form.value.name = "测试计算器";
  page.form.value.config.members.forEach((member, i) => { member.name = `成员${i}`; });
  const rule = page.form.value.config.rules[0];
  page.choosePreset(rule, "custom");
  const first = rule.steps[0];
  rule.steps.push(JSON.parse(JSON.stringify(first)));
  assert.deepEqual(page.stepSources(rule, 1), []);
  assert.match(page.validate(), /不可再次分配/);
  rule.steps.pop();
  rule.steps[0].partial = true;
  rule.steps[0].allocationMode = "percent";
  page.setPercentage(first.shares[0], "weight", "25");
  page.setPercentage(first.shares[1], "weight", "25");
  assert.match(page.validate(), /还没有分配/);
  page.setPercentage(first.shares[0], "weight", "50");
  page.setPercentage(first.shares[1], "weight", "50");
  assert.equal(page.validate(), "");
});

test("旧李子和鸽子规则按原数据识别，打开编辑器不改写比例或转移系数", () => {
  const page = editor("form, rulePreset, allocationMode, personExample");
  const members = ["owner", "sister", "datou"].map((key) => ({ key, name: key }));
  page.form.value.config.members = members;
  const dayRule = { name: "默认分账", steps: [
    { type: "share", source: "total", partial: true, shares: [{ member: "owner", weight: 1, basis: ruleHelpers.DAY_PER_MEMBER }] },
    { type: "share", source: "remainder:total", partial: false, shares: [{ member: "sister", weight: 2 }, { member: "datou", weight: 1 }] },
  ] };
  const before = JSON.stringify(dayRule);
  assert.equal(page.rulePreset(dayRule), "days");
  assert.equal(page.allocationMode(dayRule.steps[1]), "ratio");
  assert.match(page.personExample(dayRule.steps[0], dayRule.steps[0].shares[0]), /16.67 元/);
  assert.equal(JSON.stringify(dayRule), before);
  const special = { name: "特殊分账", steps: [
    { type: "share", source: "water", shares: [{ member: "owner", weight: 1 }, { member: "sister", weight: 1 }] },
    { type: "transfer", from: "owner", ratio: 0.5, to: [{ member: "sister", weight: 1 }, { member: "datou", weight: 1 }] },
  ] };
  const specialBefore = JSON.stringify(special);
  assert.equal(page.rulePreset(special), "custom");
  assert.equal(page.allocationMode(special.steps[1]), "equal");
  assert.match(page.personExample(special.steps[1], special.steps[1].to[0]), /25.00 元/);
  assert.equal(JSON.stringify(special), specialBefore);
});

test("新计算器默认使用通用费用和天数字段，旧费用仅作兼容", () => {
  const config = configHelpers.emptyCalculatorConfig();
  assert.deepEqual(config.fees.map((fee) => fee.name), ["费用"]);
  assert.deepEqual(config.dayLabels, { total: "周期总天数", counted: "参与天数" });
  assert.equal(configHelpers.getFeeSources(config).length, 4);
  assert.deepEqual(configHelpers.getBillFees({ waterBill: 1, electricBill: 2, gasBill: 3 }).map((fee) => fee.amount), [1, 2, 3]);
});

test("费用改名同步更新规则选项，已引用的费用不能误删", () => {
  const toasts = [];
  const page = setup("src/pages/calculator/edit.vue", { uni: { showToast: (toast) => toasts.push(toast) } },
    "form, addFee, removeFee, feeSources");
  const first = page.form.value.config.fees[0];
  first.name = "房租";
  page.addFee();
  const second = page.form.value.config.fees[1];
  second.name = "网费";
  page.form.value.config.rules[0].steps[0].source = first.key;
  assert.equal(page.feeSources.value.find((source) => source.value === first.key).label, "房租");
  page.removeFee(0);
  assert.equal(page.form.value.config.fees.length, 2);
  assert.equal(toasts.length, 1);
  page.removeFee(1);
  assert.equal(page.form.value.config.fees.length, 1);
});

test("费用统计和图表按自定义费用生成，兼容改名、删项及旧账", () => {
  const source = readFileSync(new URL("../src/subpackages/statistics/utils/statisticsHelper.js", import.meta.url), "utf8")
    .replace(/^import\s[\s\S]*?from\s+["'][^"']+["'];?\s*/gm, "").replace(/^export /gm, "");
  const stats = new Function("dayjs", "api", "getBillFees", `${source}\nreturn {processBillData, calculateCostDistribution, generateLineChartData, generateBarChartData};`)
    (dayjs, {}, configHelpers.getBillFees);
  const bills = [
    { year: 2026, month: 1, totalAmount: 30, fees: [{ key: "rent", name: "房租", amount: 10 }, { key: "net", name: "网费", amount: 20 }] },
    { year: 2026, month: 2, totalAmount: 15, fees: [{ key: "rent", name: "租金", amount: 15 }] },
  ];
  const processed = stats.processBillData(bills);
  assert.deepEqual(processed.feeSeries.map((fee) => fee.data), [[10, 15], [20, 0]]);
  assert.deepEqual(stats.generateLineChartData(processed).series.map((series) => series.name), ["总费用", "租金", "网费"]);
  assert.equal(stats.generateBarChartData(processed).series.length, 2);
  assert.deepEqual(stats.calculateCostDistribution(bills).map((fee) => fee.value), [25, 20]);
  assert.equal(stats.calculateCostDistribution([{ waterBill: 1, electricBill: 2, gasBill: 3 }]).length, 3);
  assert.deepEqual(stats.generateLineChartData(stats.processBillData([])).series[0].data, []);
});

test("先看分摊结果不会记账，确认保存后使用服务端账单且不能重复保存", async () => {
  const saved = { month: 11, fees: [{ key: "rent", name: "房租", emoji: "🏠", amount: 2.01 }], totalAmount: 2.01,
    shares: [{ key: "a", amount: 1.01 }, { key: "b", amount: 1 }] };
  let writes = 0;
  const page = calculator({ bill: {
    preview: async (payload) => {
      assert.deepEqual(payload.fees, { rent: 2.01 });
      assert.equal("waterBill" in payload, false);
      return saved.shares;
    },
    save: async (payload) => { writes++; assert.deepEqual(payload.fees, { rent: 2.01 }); return saved; },
  } });
  await page.saveBill();
  assert.equal(writes, 0);
  await page.calculateBill();
  assert.equal(writes, 0);
  assert.equal(page.hasSaved.value, false);
  assert.deepEqual(page.shares.value, saved.shares);
  assert.deepEqual(page.posterData.value.shares.map((share) => share.amount), ["1.01", "1.00"]);
  await page.saveBill();
  assert.equal(page.hasSaved.value, true);
  assert.equal(writes, 1);
  await page.saveBill();
  assert.equal(writes, 1);
  page.form.value.fees.rent = "9";
  assert.equal(page.showResult.value, false);
  assert.equal(page.hasSaved.value, false);
  assert.equal(page.posterData.value.fees[0].amount, 2.01);
});

test("计算中重复点击只请求一次，修改输入后不展示旧结果", async () => {
  let complete;
  let calls = 0;
  const page = calculator({ bill: { preview: () => {
    calls++;
    return new Promise((resolve) => { complete = resolve; });
  }, save: async () => assert.fail("仅计算不能保存") } });
  const pending = page.calculateBill();
  await page.calculateBill();
  assert.equal(calls, 1);
  page.form.value.fees.rent = "3";
  complete([{ key: "a", amount: 2.01 }]);
  await pending;
  assert.equal(page.showResult.value, false);
  assert.equal(page.calculating.value, false);
});

test("计算失败不展示结果，保存失败则保留预览供重试", async () => {
  const page = calculator({ bill: { preview: async () => { throw new Error("网络失败"); } } });
  await page.calculateBill();
  assert.equal(page.showResult.value, false);
  assert.equal(page.errorMessage.value, "网络失败");
  assert.equal(page.calculating.value, false);
  const preview = calculator({ bill: { preview: async () => [{ key: "a", amount: 2.01 }], save: async () => { throw new Error("保存失败"); } } });
  await preview.calculateBill();
  await preview.saveBill();
  assert.equal(preview.showResult.value, true);
  assert.equal(preview.hasSaved.value, false);
  assert.equal(preview.saving.value, false);
  assert.equal(preview.errorMessage.value, "保存失败");
});

test("确认保存的并发点击只写一次，返回时不覆盖后来修改的金额", async () => {
  let complete;
  let writes = 0;
  const page = calculator({ bill: {
    preview: async () => [{ key: "a", amount: 2.01 }],
    save: () => { writes++; return new Promise((resolve) => { complete = resolve; }); },
  } });
  await page.calculateBill();
  const pending = page.saveBill();
  await page.saveBill();
  assert.equal(writes, 1);
  page.form.value.fees.rent = "3";
  complete({ totalAmount: 2.01, shares: [{ key: "a", amount: 2.01 }] });
  await pending;
  assert.equal(page.form.value.fees.rent, "3");
  assert.equal(page.showResult.value, false);
  assert.equal(page.saving.value, false);
});

test("试算调用后端且不保存账单，按天数比例分摊时不能关闭天数输入", async () => {
  let payload;
  const toasts = [];
  const page = setup("src/pages/calculator/edit.vue", {
    api: { bill: { preview: async (request) => { payload = request; return [{ key: "a", amount: 50 }]; } } },
    uni: { showToast: (toast) => toasts.push(toast) },
  }, "form, preview, runPreview, previewResult, onStayDaysChange");
  page.form.value.config.useStayDays = true;
  page.form.value.config.rules[0].steps[0].shares[0].basis = "days/totalDays/memberCount";
  const feeKey = page.form.value.config.fees[0].key;
  page.preview.value.fees[feeKey] = 300;
  await page.runPreview();
  assert.equal(payload.fees[feeKey], 300);
  assert.equal(payload.days, 15);
  assert.equal(payload.config, page.form.value.config);
  assert.equal(page.previewResult.value[0].amount, 50);
  page.onStayDaysChange({ detail: { value: false } });
  await new Promise((resolve) => setTimeout(resolve, 1));
  assert.equal(page.form.value.config.useStayDays, true);
  assert.equal(page.form.value.config.rules[0].steps[0].shares[0].basis, "days/totalDays/memberCount");
  assert.equal(toasts.length, 1);
});

test("修改试算输入后，不展示旧请求返回的结果", async () => {
  let complete;
  const page = setup("src/pages/calculator/edit.vue", {
    api: { bill: { preview: () => new Promise((resolve) => { complete = resolve; }) } },
  }, "preview, runPreview, previewResult");
  const pending = page.runPreview();
  page.preview.value.fees.changed = 123;
  complete([{ key: "a", amount: 50 }]);
  await pending;
  assert.deepEqual(page.previewResult.value, []);
});

test("裁剪回传字符串 ID 能保存背景，先保存新背景再删除旧文件", async () => {
  const calls = [];
  const page = setup("src/pages/home/home.vue", {
    uni: { showLoading() {}, hideLoading: () => calls.push("hide"), showToast() {} },
    uploadImageWithCompress: async () => ({ fileID: "cloud://new" }),
    deleteFromCloud: async () => calls.push("delete"),
    api: { calculator: { update: async (id, data) => {
      assert.equal(id, 1);
      assert.equal(data.background, "cloud://new");
      calls.push("save");
    } } },
  }, "calculators, setCardBackground");
  page.calculators.value = [{ id: 1, background: "cloud://old" }];
  await page.setCardBackground("temp.jpg", "1");
  assert.deepEqual(calls, ["save", "delete", "hide"]);
  assert.equal(page.calculators.value[0].background, "cloud://new");
});

test("背景保存失败保留旧图并结束加载", async () => {
  let hidden = false;
  const page = setup("src/pages/home/home.vue", {
    uni: { showLoading() {}, hideLoading: () => { hidden = true; }, showToast() {} },
    uploadImageWithCompress: async () => ({ fileID: "cloud://new" }),
    deleteFromCloud: async () => assert.fail("不应删除旧图"),
    api: { calculator: { update: async () => { throw new Error("保存失败"); } } },
  }, "calculators, setCardBackground");
  page.calculators.value = [{ id: 1, background: "cloud://old" }];
  await page.setCardBackground("temp.jpg", "1");
  assert.equal(hidden, true);
  assert.equal(page.calculators.value[0].background, "cloud://old");
});

for (const count of [8, 20]) {
  for (const report of [false, true]) {
    test(`${count} 人的${report ? "统计" : "账单"}海报完整绘制`, async () => {
      const rows = [];
      const context = new Proxy({}, { get: (_, name) => {
        if (name === "createLinearGradient") return () => ({ addColorStop() {} });
        if (name === "fillText") return (text, x, y) => rows.push({ text, y });
        return () => {};
      } });
      const shares = Array.from({ length: count }, (_, i) => ({ name: `member${i}`, amount: 1 }));
      const data = { month: 11, totalAmount: count, shares, splitStatistics: shares,
        fees: Array.from({ length: 12 }, (_, i) => ({ key: `f${i}`, name: `费用${i}`, amount: 1 })),
        costDistribution: [{ name: "水费", value: 1 }, { name: "电费", value: 1 }, { name: "燃气费", value: 1 }],
        comparison: { yearOverYear: 1 }, metrics: { totalExpense: count, avgMonthly: count,
          maxMonth: { month: "11月", amount: count }, minMonth: { month: "11月", amount: count } } };
      const page = setup(report ? "src/subpackages/statistics/components/ShareReportPoster.vue" :
        "src/components/SharePoster/SharePoster.vue", {
        defineProps: () => ({ data }), uni: { createCanvasContext: () => context },
      }, "show, canvasHeight");
      await page.show();
      const people = rows.filter((row) => row.text.includes("member"));
      assert.equal(people.length, count);
      if (!report) assert.equal(rows.filter((row) => row.text.includes("费用") && row.text !== "费用分摊结果" && !row.text.includes("总费用")).length, 12);
      assert.ok(people.every((row) => row.y < page.canvasHeight.value - 50));
      assert.ok(rows.every((row) => row.y < page.canvasHeight.value));
    });
  }
}
