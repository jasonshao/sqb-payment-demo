-- Leaf-segment 号段分配表
CREATE TABLE IF NOT EXISTS leaf_alloc (
    biz_tag     VARCHAR(128) NOT NULL PRIMARY KEY,
    max_id      BIGINT       NOT NULL DEFAULT 0,
    step        INT          NOT NULL DEFAULT 2000,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 初始化业务号段
MERGE INTO leaf_alloc (biz_tag, max_id, step) KEY (biz_tag) VALUES ('PAY', 0, 2000);
MERGE INTO leaf_alloc (biz_tag, max_id, step) KEY (biz_tag) VALUES ('REFUND', 0, 2000);

-- 终端凭证持久化表
CREATE TABLE IF NOT EXISTS terminal_credential (
    device_id    VARCHAR(128) NOT NULL PRIMARY KEY,
    terminal_sn  VARCHAR(128),
    terminal_key VARCHAR(128),
    update_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
