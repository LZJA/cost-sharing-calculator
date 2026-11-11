/**
 * 与后端 FormulaEngine 同一套规则，只用于配规则时的试算和计算页的即时展示。
 * 真正落库的金额由后端重算，不采用这里的结果。
 *
 * 步骤：
 * - share：把一项费用按份额分给若干人。source 为 water/electric/gas/total，
 *   或 remainder:xxx（上一步没分完的部分）。partial 为 true 时份额是占费用的绝对比例，
 *   合计可以小于 1，剩下的留给后面的步骤。
 * - transfer：把某人当前金额的 ratio 按份额转给其他人。
 * 份额 basis 为 days/totalDays 时，实际份额还要乘以居住天数占比。
 */

const money = (value) => Math.round((Number(value) || 0) * 100) / 100;

const feeOf = (source, input, remainders) => {
  if (typeof source === "string" && source.startsWith("remainder:")) {
    const origin = source.slice("remainder:".length);
    if (!(origin in remainders)) {
      throw new Error(`没有找到上一步剩下的「${origin}」`);
    }
    return remainders[origin];
  }
  const fees = {
    water: input.waterBill,
    electric: input.electricBill,
    gas: input.gasBill,
    total: input.waterBill + input.electricBill + input.gasBill,
  };
  if (!(source in fees)) {
    throw new Error(`不认识的费用来源: ${source}`);
  }
  return fees[source];
};

const weightOf = (share, input) => {
  if (share.basis === "days/totalDays") {
    const totalDays = Number(input.totalDays) || 0;
    const ownerDays = Number(input.ownerDays) || 0;
    if (totalDays <= 0) throw new Error("使用居住天数分摊时，总天数必须大于 0");
    if (ownerDays < 0 || ownerDays > totalDays) {
      throw new Error("居住天数必须在 0 到总天数之间");
    }
    return (ownerDays / totalDays) * (Number(share.weight) || 1);
  }
  if (share.weight === undefined || share.weight === null || share.weight === "") {
    throw new Error("份额不能为空");
  }
  return Number(share.weight);
};

const applyShare = (step, input, amounts, remainders) => {
  const fee = feeOf(step.source, input, remainders);
  const shares = step.shares || [];
  if (shares.length === 0) throw new Error("分摊步骤没有分配对象");

  const weights = shares.map((share) => weightOf(share, input));
  const weightSum = weights.reduce((sum, weight) => sum + weight, 0);
  if (weightSum === 0) throw new Error("份额合计为 0");
  if (step.partial && weightSum > 1 + 1e-9) {
    throw new Error("只取一部分时，份额合计不能超过 1");
  }

  const denominator = step.partial ? 1 : weightSum;
  let distributed = 0;
  shares.forEach((share, index) => {
    const part = money((fee * weights[index]) / denominator);
    distributed = money(distributed + part);
    amounts[share.member] = money((amounts[share.member] || 0) + part);
  });
  if (step.partial) {
    remainders[step.source] = money(fee - distributed);
  }
};

const applyTransfer = (step, amounts) => {
  if (!(step.from in amounts)) throw new Error(`转出人不存在: ${step.from}`);
  const receivers = step.to || [];
  if (receivers.length === 0) throw new Error("转移步骤没有接收人");

  const moved = money(amounts[step.from] * (Number(step.ratio) || 0));
  amounts[step.from] = money(amounts[step.from] - moved);

  const weightSum = receivers.reduce(
    (sum, receiver) => sum + (Number(receiver.weight) || 1),
    0
  );
  if (weightSum === 0) throw new Error("接收份额合计为 0");

  receivers.forEach((receiver) => {
    if (!(receiver.member in amounts)) {
      throw new Error(`接收人不存在: ${receiver.member}`);
    }
    const part = money((moved * (Number(receiver.weight) || 1)) / weightSum);
    amounts[receiver.member] = money(amounts[receiver.member] + part);
  });
};

/**
 * @returns {Array<{key,name,emoji,amount}>}
 */
export function calculateShares(config, ruleName, input) {
  const members = config?.members || [];
  if (members.length === 0) throw new Error("还没有添加人员");

  const rules = config?.rules || [];
  const rule = ruleName
    ? rules.find((item) => item.name === ruleName)
    : rules.length === 1
      ? rules[0]
      : null;
  if (!rule) throw new Error(ruleName ? `找不到规则: ${ruleName}` : "请选择分账规则");

  const normalized = {
    waterBill: Number(input.waterBill) || 0,
    electricBill: Number(input.electricBill) || 0,
    gasBill: Number(input.gasBill) || 0,
    totalDays: input.totalDays,
    ownerDays: input.ownerDays,
  };

  const amounts = {};
  members.forEach((member) => {
    amounts[member.key] = 0;
  });
  const remainders = {};

  (rule.steps || []).forEach((step) => {
    if (step.type === "share") applyShare(step, normalized, amounts, remainders);
    else if (step.type === "transfer") applyTransfer(step, amounts);
    else throw new Error(`不认识的规则步骤: ${step.type}`);
  });

  return members.map((member) => ({
    key: member.key,
    name: member.name,
    emoji: member.emoji || "👤",
    amount: amounts[member.key] || 0,
  }));
}

let keySeed = 0;

/** 生成稳定的人员标识，改名字不会变。 */
export function createMemberKey() {
  keySeed += 1;
  return `m${Date.now().toString(36)}${keySeed}`;
}

export function emptyCalculatorConfig() {
  const first = createMemberKey();
  const second = createMemberKey();
  return {
    useStayDays: false,
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

export const FEE_SOURCES = [
  { value: "total", label: "总费用（水+电+燃气）" },
  { value: "water", label: "水费" },
  { value: "electric", label: "电费" },
  { value: "gas", label: "燃气费" },
  { value: "remainder:total", label: "上一步剩下的总费用" },
  { value: "remainder:water", label: "上一步剩下的水费" },
  { value: "remainder:electric", label: "上一步剩下的电费" },
  { value: "remainder:gas", label: "上一步剩下的燃气费" },
];
