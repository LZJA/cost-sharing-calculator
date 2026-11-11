/**
 * 应用配置
 *
 * 换室友只改 PEOPLE 里的 name（和想换的 emoji）。
 * 不要改 key：key 对应计算字段和数据库列，改了历史账单会对不上。
 * 人数和分账比例不在这里，改名字不会改变算法。
 */
export const PEOPLE = {
  // 李子账单：房主按居住天数分摊，另外两人平分剩余费用
  lizi: {
    title: "李子的分账计算器",
    shortTitle: "李子账单",
    emoji: "🌰",
    members: {
      owner: { name: "房主", emoji: "👤" },
      sister: { name: "谢林珠", emoji: "🌸" },
      datou: { name: "张锦豪", emoji: "🌻" },
    },
  },
  // 鸽子账单：四人固定比例，普通分账 / 特殊分账规则不变
  gezi: {
    title: "鸽子的分账计算器",
    shortTitle: "鸽子账单",
    emoji: "🕊️",
    members: {
      lizi: { name: "李子", emoji: "🍐" },
      gezi: { name: "鸽子", emoji: "🕊️" },
      chunfeng: { name: "春风", emoji: "🌸" },
      chengzi: { name: "橙子", emoji: "🍊" },
    },
  },
};

const withEmoji = (person) => `${person.emoji} ${person.name}`;

export const CONFIG = {
  // 月份中文映射
  MONTH_CN_MAP: ['', '一', '二', '三', '四', '五', '六', '七', '八', '九', '十', '十一', '十二'],

  // 验证规则
  VALIDATION: {
    MIN_DAYS: 1,
    MAX_DAYS: 31
  },

  // 计算规则
  CALCULATION: {
    OWNER_RATIO_DIVISOR: 3  // 房主承担比例除数
  },

  PEOPLE,
  // 「emoji 名字」，结果页、统计、海报直接用
  LABELS: {
    lizi: {
      owner: withEmoji(PEOPLE.lizi.members.owner),
      sister: withEmoji(PEOPLE.lizi.members.sister),
      datou: withEmoji(PEOPLE.lizi.members.datou),
    },
    gezi: {
      lizi: withEmoji(PEOPLE.gezi.members.lizi),
      gezi: withEmoji(PEOPLE.gezi.members.gezi),
      chunfeng: withEmoji(PEOPLE.gezi.members.chunfeng),
      chengzi: withEmoji(PEOPLE.gezi.members.chengzi),
    },
  },
}

