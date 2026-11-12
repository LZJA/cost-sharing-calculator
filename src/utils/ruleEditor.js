/** 仅用于规则编辑、说明和示例。正式金额仍由后端试算和保存接口计算。 */
export const DAY_BASIS = "days/totalDays";
export const DAY_PER_MEMBER = "days/totalDays/memberCount";

export function allocationRows(step) {
  return (step.type === "transfer" ? step.to : step.shares) || [];
}

export function allocationMode(step) {
  const rows = allocationRows(step);
  const samePositiveWeight = rows.length && Number(rows[0].weight ?? 1) > 0 && rows.every((row) => !row.basis && Number(row.weight ?? 1) === Number(rows[0].weight ?? 1));
  const partial = step.type === "share" && step.partial;
  if (step.allocationMode === "equal" && !partial && samePositiveWeight) return "equal";
  if (step.allocationMode === "ratio" && !partial && !rows.some((row) => row.basis)) return "ratio";
  if (step.allocationMode === "percent" && partial && !rows.some((row) => row.basis)) return "percent";
  if (rows.some((row) => row.basis)) return partial && rows.every((row) => [DAY_BASIS, DAY_PER_MEMBER].includes(row.basis)) ? "days" : "custom";
  if (partial) return "percent";
  return samePositiveWeight ? "equal" : "ratio";
}

export function percentText(value) {
  return value == null ? "" : String(Number((Number(value) * 100).toFixed(6)));
}

export function parsePercentage(text) {
  const raw = String(text).trim();
  if (!/^(?:\d+(?:\.\d*)?|\.\d+)$/.test(raw)) return null;
  const [whole, fraction = ""] = raw.split(".");
  const padded = whole.padStart(3, "0");
  const digits = padded + fraction;
  const point = padded.length - 2;
  return Number(`${digits.slice(0, point)}.${digits.slice(point)}`);
}

export function setAllocationMode(step, mode, members) {
  const current = allocationRows(step);
  const rows = current.length ? current : members.map((member) => ({ member: member.key, weight: 1 }));
  step.allocationMode = mode;
  if (step.type === "transfer") {
    step.to = rows.map((row) => ({ member: row.member, weight: mode === "equal" ? 1 : row.weight ?? 1 }));
    return;
  }
  step.partial = mode === "percent" || mode === "days";
  if (mode === "days") {
    const first = rows[0];
    step.shares = [{ member: first?.member || members[0]?.key || "", weight: 1, basis: DAY_PER_MEMBER }];
  } else if (mode === "percent") {
    // 用整数百分位分配 100%，避免三等分时出现 99.99% 或 100.01%。
    const weights = rows.map((row) => Math.max(0, Number(row.weight) || 0));
    const sum = weights.reduce((total, weight) => total + weight, 0);
    const units = weights.map((weight) => Math.floor((sum ? weight / sum : 1 / rows.length) * 10000));
    let left = 10000 - units.reduce((total, value) => total + value, 0);
    const order = rows.map((_, i) => i).sort((a, b) => {
      const fraction = (i) => (sum ? weights[i] / sum : 1 / rows.length) * 10000 - units[i];
      return fraction(b) - fraction(a);
    });
    for (let i = 0; i < left; i++) units[order[i % order.length]]++;
    step.shares = rows.map((row, i) => ({ member: row.member, weight: units[i] / 10000, basis: "" }));
  } else {
    step.shares = rows.map((row) => ({ member: row.member, weight: mode === "equal" ? 1 : row.weight ?? 1, basis: "" }));
  }
}

export function sampleFraction(step, row, memberCount) {
  const effective = (item) => {
    let weight = Math.max(0, Number(item.weight) || 0);
    if (item.basis === DAY_BASIS) weight *= 0.5;
    if (item.basis === DAY_PER_MEMBER) weight *= 0.5 / Math.max(1, memberCount);
    return weight;
  };
  if (step.type !== "transfer" && step.partial) return effective(row);
  const sum = allocationRows(step).reduce((total, item) => total + effective(item), 0);
  return sum ? effective(row) / sum : 0;
}

export function selectedFeeSources(step) {
  return Array.isArray(step.sources) ? step.sources : step.source ? [step.source] : [];
}

export function sourceGroupKey(sources) {
  if (!sources.length) return null;
  if (sources.length === 1) return sources[0].replace(/^remainder:/, "");
  return `group:${[...sources].sort().map((source) => `${source.length}:${source}`).join("")}`;
}

export function setFeeSources(step, sources) {
  step.sources = [...new Set(sources)];
  // 单选保留旧字段；多选时置空，避免旧服务端忽略 sources 后误算为某一项费用。
  step.source = step.sources.length === 1 ? step.sources[0] : null;
}

export function expandedFeeSources(step, fees) {
  return selectedFeeSources(step).flatMap((source) => source === "total" ? fees.map((fee) => fee.key) : [source]);
}

export function stepSourceOptions(rule, index, fees) {
  const claimed = new Set();
  const remaining = new Map();
  const definitions = new Map(fees.map((fee) => [fee.key, fee]));
  rule.steps.slice(0, index).forEach((step, i) => {
    if (step.type !== "share") return;
    const sources = selectedFeeSources(step);
    const keys = sources.flatMap((source) => source.startsWith("remainder:")
      ? remaining.get(source.slice("remainder:".length))?.feeKeys || []
      : source === "total" ? fees.map((fee) => fee.key) : [source]);
    const allZero = sources.length > 0 && sources.every((source) => source.startsWith("remainder:")
      && remaining.get(source.slice("remainder:".length))?.optional);
    sources.forEach((source) => {
      if (source.startsWith("remainder:")) remaining.delete(source.slice("remainder:".length));
      else (source === "total" ? fees.map((fee) => fee.key) : [source]).forEach((key) => claimed.add(key));
    });
    const origin = sourceGroupKey(sources);
    if (step.partial && origin) {
      const feeKeys = [...new Set(keys)];
      const names = feeKeys.map((key) => definitions.get(key)?.name || "已删除费用").join(" + ");
      const fixedFull = allocationRows(step).every((row) => !row.basis) && Math.abs(allocationRows(step).reduce((sum, row) => sum + Number(row.weight || 0), 0) - 1) < 1e-9;
      remaining.set(origin, { value: `remainder:${origin}`, label: `第 ${i + 1} 步剩余费用（${names}）`, feeKeys, optional: allZero || fixedFull });
    }
  });
  return [
    ...fees.filter((fee) => !claimed.has(fee.key)).map((fee) => ({ value: fee.key, label: fee.name || "未命名费用", emoji: fee.emoji, optional: false })),
    ...remaining.values(),
  ];
}

export function nextFeeSource(rule, fees) {
  return stepSourceOptions(rule, rule.steps.length, fees).find((source) => !source.optional)?.value || null;
}
