-- ============================================
-- 成本分摊计算器数据库初始化脚本
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS cost_sharing_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE cost_sharing_db;

-- ============================================
-- 卡片配置表
-- ============================================
CREATE TABLE IF NOT EXISTS cards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    type VARCHAR(10) NOT NULL UNIQUE COMMENT '卡片类型：lizi 或 gezi',
    name VARCHAR(100) NOT NULL COMMENT '卡片名称',
    description VARCHAR(200) COMMENT '描述语',
    avatar VARCHAR(10) COMMENT '头像（emoji）',
    background VARCHAR(500) COMMENT '背景图片路径',
    enable_background BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用背景图',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='卡片配置表';

-- ============================================
-- 李子账单表
-- ============================================
CREATE TABLE IF NOT EXISTS lizi_bills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    month INT NOT NULL COMMENT '月份 (1-12)',
    year INT NOT NULL COMMENT '年份',
    total_days INT NOT NULL COMMENT '月份总天数',
    water_bill DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '水费',
    electric_bill DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '电费',
    gas_bill DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '燃气费',
    owner_days INT NOT NULL DEFAULT 0 COMMENT '房主居住天数',
    total_amount DECIMAL(10, 2) NOT NULL COMMENT '总费用',
    owner_amount DECIMAL(10, 2) NOT NULL COMMENT '房主应承担费用',
    remaining_amount DECIMAL(10, 2) NOT NULL COMMENT '剩余费用',
    sister_amount DECIMAL(10, 2) NOT NULL COMMENT '谢林珠应承担费用',
    datou_amount DECIMAL(10, 2) NOT NULL COMMENT '张锦豪应承担费用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_month_year (month, year),
    INDEX idx_year (year),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='李子账单表';

-- ============================================
-- 鸽子账单表
-- ============================================
CREATE TABLE IF NOT EXISTS gezi_bills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    water_bill DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '水费',
    electric_bill DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '电费',
    gas_bill DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '燃气费',
    split_rule VARCHAR(20) NOT NULL COMMENT '分账规则：普通分账 或 特殊分账',
    total_amount DECIMAL(10, 2) NOT NULL COMMENT '总费用',
    lizi_amount DECIMAL(10, 2) COMMENT '李子应承担费用',
    gezi_amount DECIMAL(10, 2) COMMENT '鸽子应承担费用',
    chunfeng_amount DECIMAL(10, 2) COMMENT '春风应承担费用',
    chengzi_amount DECIMAL(10, 2) COMMENT '橙子应承担费用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_split_rule (split_rule),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鸽子账单表';

-- ============================================
-- 初始化卡片数据
-- ============================================
INSERT INTO cards (type, name, description, avatar, background, enable_background) VALUES
('lizi', '李子的分账计算器', '分账让生活更简单', '🍐', '', true),
('gezi', '鸽子的分账计算器', '记录每一份美好小账单', '🕊️', '', true)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    avatar = VALUES(avatar);

-- ============================================
-- 查询语句示例
-- ============================================

-- 查询所有卡片
-- SELECT * FROM cards;

-- 查询指定类型的卡片
-- SELECT * FROM cards WHERE type = 'lizi';

-- 查询李子的最近账单
-- SELECT * FROM lizi_bills ORDER BY year DESC, month DESC LIMIT 10;

-- 查询李子指定月份的账单
-- SELECT * FROM lizi_bills WHERE year = 2024 AND month = 1;

-- 查询鸽子的最近账单
-- SELECT * FROM gezi_bills ORDER BY created_at DESC LIMIT 10;

-- 统计李子某年的总费用
-- SELECT
--     year,
--     SUM(total_amount) as total_yearly_amount,
--     COUNT(*) as bill_count
-- FROM lizi_bills
-- WHERE year = 2024
-- GROUP BY year;
