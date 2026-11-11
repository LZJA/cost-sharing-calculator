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
  page.calculator.value = { id: 1, config: { members: [], rules: [{ name: "默认分账" }] } };
  page.form.value.waterBill = "2.01";
  return page;
}

test("展示和海报使用保存接口金额，修改输入后隐藏旧结果", async () => {
  const saved = { month: 11, waterBill: 2.01, totalAmount: 2.01,
    shares: [{ key: "a", amount: 1.01 }, { key: "b", amount: 1 }] };
  const page = calculator({ bill: { save: async () => saved } });
  await page.calculateBill();
  assert.deepEqual(page.shares.value, saved.shares);
  assert.equal(page.posterData.value.totalAmount, "2.01");
  assert.deepEqual(page.posterData.value.shares.map((share) => share.amount), ["1.01", "1.00"]);
  assert.equal(page.showResult.value, true);
  page.form.value.waterBill = "9";
  assert.equal(page.showResult.value, false);
  assert.equal(page.posterData.value.waterBill, 2.01);
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
  page.form.value.waterBill = "3";
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

test("试算调用后端且不保存账单，按天分摊时不能关闭天数输入", async () => {
  let payload;
  const toasts = [];
  const page = setup("src/pages/calculator/edit.vue", {
    api: { bill: { preview: async (request) => { payload = request; return [{ key: "a", amount: 50 }]; } } },
    uni: { showToast: (toast) => toasts.push(toast) },
  }, "form, preview, runPreview, previewResult, onStayDaysChange");
  page.form.value.config.useStayDays = true;
  page.form.value.config.rules[0].steps[0].shares[0].basis = "days/totalDays/memberCount";
  page.preview.value.waterBill = 300;
  await page.runPreview();
  assert.equal(payload.waterBill, 300);
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
  page.preview.value.waterBill = 123;
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
      assert.ok(people.every((row) => row.y < page.canvasHeight.value - 50));
      assert.ok(rows.every((row) => row.y < page.canvasHeight.value));
    });
  }
}
