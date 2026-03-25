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

-- Payment order aggregate table
CREATE TABLE IF NOT EXISTS payment_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_order_no VARCHAR(64) NOT NULL,
    channel_order_no VARCHAR(64),
    refund_request_no VARCHAR(64),
    device_id VARCHAR(128),
    order_type VARCHAR(20) NOT NULL,
    current_status VARCHAR(30) NOT NULL,
    amount BIGINT,
    last_channel_code VARCHAR(64),
    last_channel_message VARCHAR(512),
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_po_merchant_no ON payment_order(merchant_order_no);
CREATE INDEX IF NOT EXISTS idx_po_channel_no ON payment_order(channel_order_no);

-- Webhook idempotency table
CREATE TABLE IF NOT EXISTS idempotency_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(256) NOT NULL UNIQUE,
    event_type VARCHAR(64),
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
