-- 创建数据库
CREATE DATABASE IF NOT EXISTS inventory_demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE inventory_demo;

-- 商品表
CREATE TABLE IF NOT EXISTS goods (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
    name VARCHAR(100) NOT NULL COMMENT '商品名称',
    price DECIMAL(10,2) NOT NULL COMMENT '商品价格'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 库存表
CREATE TABLE IF NOT EXISTS inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '库存ID',
    goods_id BIGINT NOT NULL COMMENT '商品ID',
    quantity INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    UNIQUE KEY uk_goods_id (goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';

-- 库存流水表
CREATE TABLE IF NOT EXISTS stock_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '流水ID',
    goods_id BIGINT NOT NULL COMMENT '商品ID',
    before_quantity INT NOT NULL COMMENT '扣减前库存',
    after_quantity INT NOT NULL COMMENT '扣减后库存',
    change_num INT NOT NULL COMMENT '变化数量（负数表示扣减）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_goods_id (goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水表';

-- ============================================
-- 可选：初始化测试数据（按需执行，也可手动插入）
-- ============================================
-- INSERT INTO goods (name, price) VALUES
-- ('机械键盘', 399.00),
-- ('无线鼠标', 129.00),
-- ('显示器', 1299.00);
--
-- INSERT INTO inventory (goods_id, quantity, version) VALUES
-- (1, 10, 0),
-- (2, 5, 0),
-- (3, 3, 0);