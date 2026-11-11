/** 人员标识和规则编辑器的配置选项。金额统一由后端 FormulaEngine 计算。 */

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
