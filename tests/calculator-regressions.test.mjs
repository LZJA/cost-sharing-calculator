import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { ref, computed, watch, nextTick } from "vue";
import dayjs from "dayjs";

const configHelpers = await import(`data:text/javascript;base64,${Buffer.from(
  readFileSync(new URL("../src/utils/formula.js", import.meta.url), "utf8")
).toString("base64")}`);

// 在无小程序运行时的环境执行真实页面脚本，替换网络和平台边界。
function setup(file, bindings, exposed) {
  const source = readFileSync(new URL(`../${file}`, import.meta.url), "utf8")
    .match(/<script setup>([\s\S]*?)<\/script>/)[1]
    .replace(/^import\s[\s\S]*?from\s+["'][^"']+["'];?\s*/gm, "");
  const dependencies = {
    ref, computed, watch, nextTick, dayjs, ...configHelpers,
    onMounted() {}, onShow() {}, onLoad() {}, getCurrentInstance: () => null,
    defineExpose() {}, defineEmits: () => () => {},
    console: { error() {} }, ...bindings,
  };
  return new Function(...Object.keys(dependencies), `${source}\nreturn {${exposed}};`)(...Object.values(dependencies));
}

function calculator(api) {
  const page = setup("src/pages/calculator/index.vue", { api },
    "calculator, form, calculateBill, shares, showResult, posterData, errorMessage, calculating");
  page.calculator.value = { id: 1, config: { fees: [{ key: "rent", name: "房租", emoji: "🏠" }], members: [], rules: [{ name: "默认分账" }] } };
  page.form.value.fees.rent = "2.01";
  return page;
}

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
  const source = readFileSync(new URL("../src/utils/statisticsHelper.js", import.meta.url), "utf8")
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

test("展示和海报使用保存接口金额，修改输入后隐藏旧结果", async () => {
  const saved = { month: 11, fees: [{ key: "rent", name: "房租", emoji: "🏠", amount: 2.01 }], totalAmount: 2.01,
    shares: [{ key: "a", amount: 1.01 }, { key: "b", amount: 1 }] };
  const page = calculator({ bill: { save: async (payload) => {
    assert.deepEqual(payload.fees, { rent: 2.01 });
    assert.equal("waterBill" in payload, false);
    return saved;
  } } });
  await page.calculateBill();
  assert.deepEqual(page.shares.value, saved.shares);
  assert.equal(page.posterData.value.totalAmount, "2.01");
  assert.deepEqual(page.posterData.value.shares.map((share) => share.amount), ["1.01", "1.00"]);
  assert.equal(page.showResult.value, true);
  page.form.value.fees.rent = "9";
  assert.equal(page.showResult.value, false);
  assert.equal(page.posterData.value.fees[0].amount, 2.01);
});

test("计算中重复点击只保存一次，异步返回不覆盖已修改的输入", async () => {
  let complete;
  let calls = 0;
  const page = calculator({ bill: { save: () => {
    calls++;
    return new Promise((resolve) => { complete = resolve; });
  } } });
  const pending = page.calculateBill();
  await page.calculateBill();
  assert.equal(calls, 1);
  page.form.value.fees.rent = "3";
  complete({ totalAmount: 2.01, shares: [] });
  await pending;
  assert.equal(page.showResult.value, false);
  assert.equal(page.calculating.value, false);
});

test("保存失败不展示成功结果", async () => {
  const page = calculator({ bill: { save: async () => { throw new Error("网络失败"); } } });
  await page.calculateBill();
  assert.equal(page.showResult.value, false);
  assert.equal(page.errorMessage.value, "网络失败");
  assert.equal(page.calculating.value, false);
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
  assert.equal(payload.days, 0);
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
