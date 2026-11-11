/** 人员标识和规则编辑器的配置选项。金额统一由后端 FormulaEngine 计算。 */

let keySeed = 0;

/** 生成稳定的人员标识，改名字不会变。 */
export function createMemberKey() {
  keySeed += 1;
  return `m${Date.now().toString(36)}${keySeed}`;
}

export function createFeeKey() {
  keySeed += 1;
  return `f${Date.now().toString(36)}${keySeed}`;
}

export function getFeeDefinitions(config) {
  return config?.fees ?? [
    { key: "water", name: "水费", emoji: "💧" },
    { key: "electric", name: "电费", emoji: "⚡" },
    { key: "gas", name: "燃气费", emoji: "🔥" },
  ];
}

export function getBillFees(bill) {
  if (Array.isArray(bill?.fees)) return bill.fees;
  const fields = { water: "waterBill", electric: "electricBill", gas: "gasBill" };
  return getFeeDefinitions(null).map((fee) => ({ ...fee, amount: Number(bill?.[fields[fee.key]]) || 0 }));
}

export function getFeeAmounts(config, values) {
  return Object.fromEntries(getFeeDefinitions(config).map((fee) => [fee.key, Number(values?.[fee.key]) || 0]));
}

export function getFeeSources(config) {
  const fees = getFeeDefinitions(config);
  return [
    { value: "total", label: "总费用" },
    ...fees.map((fee) => ({ value: fee.key, label: fee.name || "未命名费用" })),
    { value: "remainder:total", label: "上一步剩下的总费用" },
    ...fees.map((fee) => ({ value: `remainder:${fee.key}`, label: `上一步剩下的${fee.name || "费用"}` })),
  ];
}

export function getDayLabels(config) {
  return {
    total: config?.dayLabels?.total || "周期总天数",
    counted: config?.dayLabels?.counted || "参与天数",
  };
}

export function emptyCalculatorConfig() {
  const first = createMemberKey();
  const second = createMemberKey();
  return {
    fees: [{ key: createFeeKey(), name: "费用", emoji: "💰" }],
    useStayDays: false,
    dayLabels: getDayLabels(null),
    members: [
      { key: first, name: "", emoji: "👤" },
      { key: second, name: "", emoji: "👤" },
    ],
    rules: [
      {
        name: "默认分账",
        steps: [
          {
            type: "share",
            source: "total",
            partial: false,
            shares: [
              { member: first, weight: 1 },
              { member: second, weight: 1 },
            ],
          },
        ],
      },
    ],
  };
}
