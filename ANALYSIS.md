# 收钱吧支付对接最佳实践分析报告

> 本文档将 `sqb-payment-demo` 的实现与 `sqb-payment-skills` 定义的最佳实践进行系统对比，识别差距并给出改进建议。

---

## 目录

1. [总体评估](#1-总体评估)
2. [签名与授权](#2-签名与授权)
3. [三层响应解析](#3-三层响应解析)
4. [终端管理（激活 + 签到）](#4-终端管理激活--签到)
5. [B2C 付款码支付](#5-b2c-付款码支付)
6. [轮询策略](#6-轮询策略)
7. [退款处理](#7-退款处理)
8. [异步回调通知](#8-异步回调通知)
9. [金额与序列号处理](#9-金额与序列号处理)
10. [安全与运维实践](#10-安全与运维实践)
11. [缺失功能](#11-缺失功能)
12. [改进建议优先级](#12-改进建议优先级)

---

## 1. 总体评估

| 维度 | 评价 | 说明 |
|------|------|------|
| 架构设计 | ★★★★★ | SqbApiTemplate 模板模式、分层清晰、关注点分离良好 |
| 签名机制 | ★★★★★ | MD5 签名实现正确，vendor/terminal 两级分离 |
| 响应解析 | ★★★★★ | 三层响应模型完整，final/non-final 状态判定正确 |
| 轮询策略 | ★★★★☆ | 两阶段频率策略正确，缺少回调和元数据 |
| 终端管理 | ★★★☆☆ | 基本功能完整，缺少密钥灾备和持久化 |
| 回调通知 | ★★☆☆☆ | 验签算法与 skills 规范不一致（MD5 vs RSA） |
| 功能覆盖 | ★★★☆☆ | 缺少 C2B 预下单和撤单两个核心业务 |
| 测试覆盖 | ★★★★★ | 90 个单元测试，100% 通过率 |

**总分：4.0 / 5.0** — 架构和核心流程实现优秀，但在生产安全和功能完整性方面存在差距。

---

## 2. 签名与授权

### Skills 规范

- 签名算法：`MD5(request_body + key)`
- Authorization 格式：`{sn} {signature}`（中间恰好一个空格）
- Vendor 级别签名用于终端激活，Terminal 级别签名用于其他操作
- 请求体必须是 UTF-8 JSON，字段顺序一致

### Demo 实现

**文件**: `SqbSignUtil.java`

```java
public static String sign(String requestBody, String key) {
    String raw = requestBody + key;
    MessageDigest md = MessageDigest.getInstance("MD5");
    byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
    return bytesToHex(digest);
}

public static String buildAuthorization(String sn, String requestBody, String key) {
    return sn + " " + sign(requestBody, key);
}
```

**文件**: `SqbApiTemplate.java`

```java
// Terminal 级别
public SqbResponse call(String path, Object request) {
    return doCall(path, request, config.getTerminalSn(), config.getTerminalKey());
}

// Vendor 级别
public SqbResponse callAsVendor(String path, Object request) {
    return doCall(path, request, config.getVendorSn(), config.getVendorKey());
}
```

### 评估：✅ 完全符合

- MD5 签名算法实现正确
- UTF-8 编码明确指定
- Authorization 格式正确（`sn + " " + sign`）
- Vendor/Terminal 两级签名通过 `SqbApiTemplate` 清晰分离
- 签名使用序列化后的 JSON 原始字符串，确保一致性

---

## 3. 三层响应解析

### Skills 规范

三层判定模型：
1. **通信层** (`result_code`): 200 表示通信成功
2. **业务层** (`biz_response.result_code`): 操作结果码
3. **订单状态层** (`order_status`): 最终交易状态

最终状态：`PAID`, `PAY_CANCELED`, `REFUNDED`, `PARTIAL_REFUNDED`, `CANCELED`

非最终状态（需轮询）：`CREATED`, `PAY_ERROR`, `REFUND_ERROR`, `CANCEL_ERROR`, `PAY_IN_PROGRESS`, `REFUND_IN_PROGRESS`

### Demo 实现

**文件**: `SqbResponse.java`

```java
public boolean isCommunicationSuccess() {     // 第1层：通信层
    return "200".equals(getResultCode());
}
public String getBizResultCode() {              // 第2层：业务层
    return rawResponse.path("biz_response").path("result_code").asText("");
}
public String getOrderStatus() {                // 第3层：订单状态
    return getData().path("order_status").asText(getData().path("status").asText(""));
}
```

**文件**: `OrderStatus.java`

```java
private static final Set<OrderStatus> FINAL_STATES = Set.of(
    PAID, PAY_CANCELED, REFUNDED, PARTIAL_REFUNDED, CANCELED
);
```

### 评估：✅ 完全符合

- 三层结构完整实现
- 最终状态集合与 skills 规范一致
- 非最终状态（含 `PAY_IN_PROGRESS`、`CANCEL_ERROR` 等）均触发轮询
- `OrderStatus` 枚举设计清晰，`isFinal()` 静态方法便于使用

---

## 4. 终端管理（激活 + 签到）

### Skills 规范

**激活要求：**
- 激活码一次性使用
- `device_id` 每个终端唯一
- 激活成功后必须**持久化** `terminal_sn` 和 `terminal_key`
- 密钥丢失只能重新激活

**签到要求：**
- 签到成功后 `terminal_key` 自动轮换
- **必须实现**：签到前密钥备份、超时时用旧密钥重试
- 签名错误时触发重新激活
- 分布式环境需要分布式锁协调
- 建议每天首笔交易前执行一次

### Demo 实现

**文件**: `SqbTerminalService.java`

```java
// 激活：仅更新内存
config.setTerminalSn(terminalSn);
config.setTerminalKey(terminalKey);  // ⚠️ 仅 volatile 字段，未持久化

// 签到：synchronized 但无灾备
public synchronized SqbResponse checkin() {
    // ...
    config.setTerminalKey(newTerminalKey);  // ⚠️ 无旧密钥备份
}
```

**文件**: `SqbConfig.java`

```java
private volatile String terminalSn;    // 仅内存
private volatile String terminalKey;   // 仅内存，重启丢失
```

### 评估：⚠️ 存在显著差距

| 检查项 | Skills 要求 | Demo 状态 |
|--------|-----------|-----------|
| 凭证持久化 | 数据库或加密配置 | ❌ 仅 volatile 内存字段 |
| 签到前密钥备份 | 必须 | ❌ 未实现 |
| 超时用旧密钥重试 | 必须 | ❌ 未实现 |
| 签名错误触发重新激活 | 推荐 | ❌ 未实现 |
| 分布式锁 | 多节点必须 | ❌ 未实现（仅 `synchronized`） |
| 定时签到 | 每天首笔前 | ❌ 未调度（虽有 `@EnableScheduling`） |

### 风险评估

**高风险**：应用重启后 `terminal_key` 丢失，无法恢复，只能重新激活终端。签到过程中网络超时可能导致本地密钥与服务端不同步，后续所有 API 调用都会签名失败。

---

## 5. B2C 付款码支付

### Skills 规范

- 端点：`POST /upay/v2/pay`
- `client_sn` 全局唯一，支付失败后**不能复用**
- `dynamic_id`（付款码）每笔交易过期
- 必须轮询：3 秒间隔 60 秒，然后 10 秒间隔，最大超时 120 秒
- 支付结果只依赖 `order_status`，不能仅看 `biz_response.result_code`

### Demo 实现

**文件**: `SqbPayService.java`

```java
public CompletableFuture<SqbResponse> pay(PayCommand command) {
    String clientSn = clientSnGenerator.generate();  // ✅ Leaf 号段保证唯一

    // 三层判定
    if (!sqbResponse.isCommunicationSuccess()) return ...;        // 通信层
    if ("PAY_FAIL".equals(bizResultCode)) return ...;            // 业务层
    if ("PAY_SUCCESS".equals(bizResultCode) && isFinal) return ...; // 状态层

    return queryService.pollByClientSn(clientSn);  // ✅ 非最终→轮询
}
```

### 评估：✅ 基本符合，有一处小差距

| 检查项 | Skills 要求 | Demo 状态 |
|--------|-----------|-----------|
| client_sn 全局唯一 | 必须 | ✅ Leaf-segment 号段模式 |
| 三层响应判定 | 必须 | ✅ 通信→业务→状态逐层判定 |
| 非最终状态轮询 | 必须 | ✅ 异步轮询 |
| client_sn 失败后防复用 | 推荐 | ⚠️ 无显式防护（Leaf 自增天然不复用，但无断路器） |

---

## 6. 轮询策略

### Skills 规范

**B2C 支付轮询：**
- 快速阶段：每 3 秒查询，持续 60 秒
- 慢速阶段：每 10 秒查询，持续到 120 秒
- 超时返回最后结果

**C2B 预下单轮询（Demo 未实现）：**
- 快速阶段：每 2 秒查询，持续 30 秒
- 慢速阶段：每 5 秒查询，持续到 240 秒

**附加要求：**
- 可选 `PollCallback` 进度回调
- 返回 `PollingResult` 含状态、耗时、轮询次数

### Demo 实现

**文件**: `SqbQueryService.java`

```java
private static final int FAST_INTERVAL_MS = 3_000;     // ✅
private static final int SLOW_INTERVAL_MS = 10_000;    // ✅
private static final long FAST_PHASE_MS = 60_000L;     // ✅
private static final int POLL_TIMEOUT_SECONDS = 120;   // ✅
```

**文件**: `AsyncConfig.java`

```java
executor.setCorePoolSize(8);
executor.setMaxPoolSize(32);
executor.setQueueCapacity(128);
```

### 评估：✅ 核心参数正确，缺少增强功能

| 检查项 | Skills 要求 | Demo 状态 |
|--------|-----------|-----------|
| 两阶段频率 | 3s/10s | ✅ 完全一致 |
| 超时 120 秒 | 必须 | ✅ |
| 独立线程池 | 推荐 | ✅ 8-32 核心，128 队列 |
| 轮询进度回调 | 可选 | ❌ 无 PollCallback |
| 轮询结果元数据 | 可选 | ❌ 无 PollingResult（直接返回 SqbResponse） |
| C2B 轮询参数 | 2s/5s/240s | ❌ 不适用（C2B 未实现） |

---

## 7. 退款处理

### Skills 规范

- 端点：`POST /upay/v2/refund`
- 异步操作，必须轮询确认最终结果
- `refund_request_no` 每次请求唯一
- `refund_amount` 单位为分
- 累计退款不超过原订单金额
- 支持全额退款和部分退款

### Demo 实现

**文件**: `SqbRefundService.java`

```java
String refundRequestNo = clientSnGenerator.generateRefundNo();  // ✅ Leaf 号段唯一
request.setRefundAmount(String.valueOf(command.refundAmount())); // ✅ 分为单位

// 三层判定后启动轮询
if ("REFUND_SUCCESS".equals(bizResultCode) && isFinal) return ...;
if ("REFUND_FAIL".equals(bizResultCode)) return ...;
return queryService.pollBySn(command.sn());  // ✅ 非最终→轮询
```

### 评估：✅ 基本符合

| 检查项 | Skills 要求 | Demo 状态 |
|--------|-----------|-----------|
| refund_request_no 唯一 | 必须 | ✅ Leaf 号段生成 |
| 退款金额分为单位 | 必须 | ✅ |
| 异步轮询确认 | 必须 | ✅ |
| 部分退款支持 | 推荐 | ✅ 通过 RefundCommand 支持 |
| 累计退款校验 | 推荐 | ⚠️ 依赖服务端校验，本地无前置检查 |

---

## 8. 异步回调通知

### Skills 规范

- **必须使用 RSA SHA256WithRSA 验签**
- 验签失败返回 HTTP 403
- 不处理任何业务逻辑
- 使用 `sn` 作为幂等键
- 回调不可替代主动轮询
- 重试间隔：1s → 5s → 30s → 600s

### Demo 实现

**文件**: `SqbNotifyController.java`

```java
// ⚠️ 使用 MD5 验签，不是 RSA
if (!SqbSignUtil.verifySign(requestBody, config.getTerminalKey(), receivedSign)) {
    log.warn("回调通知签名验证失败: sn={}", receivedSn);
    return "fail";  // ⚠️ 返回字符串 "fail"，不是 HTTP 403
}

// ✅ 幂等处理：有界 LRU 缓存
private final Map<String, Long> processedOrders = Collections.synchronizedMap(
    new LinkedHashMap<>(256, 0.75f, true) { ... });
```

### 评估：❌ 存在重大差距

| 检查项 | Skills 要求 | Demo 状态 |
|--------|-----------|-----------|
| 验签算法 | RSA SHA256WithRSA | ❌ MD5（算法不匹配） |
| 验签失败响应 | HTTP 403 | ❌ 返回字符串 "fail" |
| 幂等处理 | 使用 sn 去重 | ✅ 有界 LRU 缓存 |
| 缓存大小限制 | 防止内存泄漏 | ✅ 10,000 上限 |
| 最终状态处理 | 更新本地订单状态 | ⚠️ 仅 TODO 注释 |

### 风险评估

**高风险**：回调验签算法不匹配意味着在收钱吧使用 RSA 签名的情况下，所有回调通知的签名验证将永远失败（或被伪造请求通过 MD5 验证），导致要么拒绝所有合法通知，要么无法防御伪造请求。

---

## 9. 金额与序列号处理

### Skills 规范

- 所有金额以分为单位（不是元）
- 必须使用整数运算，避免浮点精度问题
- `client_sn` 全局唯一

### Demo 实现

**文件**: `PayCommand.java`（参数为 `int totalAmount`）
**文件**: `ClientSnGenerator.java`（Leaf-segment 双缓冲号段）

```java
// 格式：yyyyMMdd + 12位号段ID = 20位
return date + String.format("%012d", id);

// 退款：REF + yyyyMMdd + 12位号段ID = 23位
return "REF" + date + String.format("%012d", id);
```

### 评估：✅ 完全符合

- 金额使用 `int` 类型，天然避免浮点问题
- `client_sn` 使用 Leaf-segment 号段模式，双缓冲异步预加载，零等待切换
- 号段步长 2000，足够应对并发场景
- ID 格式包含日期前缀，兼顾可读性和唯一性

---

## 10. 安全与运维实践

### Skills 规范

**关键安全措施：**
1. Terminal Key 必须安全存储（数据库或加密配置）
2. Checkin 自动轮换密钥，必须有灾备方案
3. 回调必须 RSA 验签
4. 无沙箱环境 — 所有交易涉及真实资金
5. 测试后必须退款清理

### Demo 实现

| 安全措施 | Demo 状态 | 具体情况 |
|---------|-----------|---------|
| 密钥安全存储 | ❌ | volatile 内存字段，重启丢失 |
| 密钥轮换灾备 | ❌ | 无备份/回滚机制 |
| RSA 回调验签 | ❌ | 使用 MD5 |
| 无沙箱警告 | ⚠️ | README 提到配置需要环境变量，但无显式"真实资金"警告 |
| 环境变量保护 | ✅ | 敏感配置通过 `${SQB_*}` 环境变量注入 |
| HTTP 超时设置 | ✅ | connect=10s, read=60s |
| 日志记录 | ✅ | 请求/响应完整记录 |
| 异常处理 | ✅ | GlobalExceptionHandler 统一处理 |

### 运维实践

| 运维措施 | Skills 要求 | Demo 状态 |
|---------|-----------|-----------|
| 定时签到 | 每天首笔交易前 | ❌ 无定时任务（虽已启用 `@EnableScheduling`） |
| 分布式锁 | 多节点签到协调 | ❌ 仅 Java `synchronized` |
| 健康检查 | 签到失败告警 | ❌ 无监控/告警 |
| 日志脱敏 | 密钥不可明文日志 | ⚠️ 请求体明文记录，可能含敏感信息 |

---

## 11. 缺失功能

### 11.1 C2B 预下单（`/upay/v2/precreate`）

Skills 定义了完整的 C2B 流程：
- 商户生成 QR 码，客户扫码支付
- 必须指定 `payway` 参数（1=支付宝, 3=微信）
- 独立轮询策略：2s/5s 间隔，240s 超时
- 适用于无人零售、桌码支付等场景

**Demo 状态**：未实现。仅支持 B2C（商户扫码）模式。

### 11.2 撤单（`/upay/v2/cancel`）

Skills 定义了当天撤单功能：
- 仅限当天交易（00:00 后）
- 仅限全额撤单
- 不能撤销部分退款的订单
- `CANCEL_ERROR` 必须查询确认（不能假定失败）

**Demo 状态**：未实现。`OrderStatus` 枚举中已定义 `CANCELED` 和 `CANCEL_ERROR` 状态，但无对应 Service 和 Controller。

### 11.3 RSA 验签工具

Skills 明确要求回调通知使用 RSA SHA256WithRSA 验签，需要：
- 收钱吧公钥配置
- `Signature.getInstance("SHA256WithRSA")` 验证
- 验签失败返回 HTTP 403（非字符串 "fail"）

**Demo 状态**：使用 MD5 验签（`SqbSignUtil.verifySign`），与规范不一致。

---

## 12. 改进建议优先级

### P0 — 必须修复（生产阻断）

| # | 改进项 | 原因 | 涉及文件 |
|---|--------|------|---------|
| 1 | 实现 terminal_key 持久化存储 | 重启丢失密钥，无法恢复 | `SqbConfig`, `SqbTerminalService` |
| 2 | 回调通知改用 RSA SHA256WithRSA 验签 | 当前验签算法与收钱吧不匹配 | `SqbNotifyController` |
| 3 | 验签失败返回 HTTP 403 | 安全规范要求 | `SqbNotifyController` |

### P1 — 强烈建议（生产必需）

| # | 改进项 | 原因 | 涉及文件 |
|---|--------|------|---------|
| 4 | 签到前密钥备份 + 超时旧密钥重试 | 防止密钥不同步 | `SqbTerminalService` |
| 5 | 添加定时签到调度 | 每天首笔交易前执行 | 新建 `CheckinScheduler` |
| 6 | 实现 C2B 预下单功能 | 核心支付场景缺失 | 新建 Service/Controller |
| 7 | 实现撤单功能 | 核心支付场景缺失 | 新建 Service/Controller |

### P2 — 推荐改进（增强健壮性）

| # | 改进项 | 原因 | 涉及文件 |
|---|--------|------|---------|
| 8 | 分布式锁替代 synchronized | 多节点部署需求 | `SqbTerminalService` |
| 9 | 轮询进度回调 + 结果元数据 | 提升可观测性 | `SqbQueryService` |
| 10 | 回调通知本地订单状态更新 | 当前仅 TODO | `SqbNotifyController` |
| 11 | 日志脱敏（屏蔽密钥和敏感字段） | 安全最佳实践 | `SqbHttpClient` |
| 12 | 添加"无沙箱环境"显式警告 | 防止测试误操作 | `README.md`, 配置文件 |

---

## 附录：文件对照表

| Skills 规范模块 | Demo 对应文件 | 匹配度 |
|----------------|--------------|--------|
| sqb-activate | `SqbTerminalService.activate()` | ★★★★☆ |
| sqb-checkin | `SqbTerminalService.checkin()` | ★★★☆☆ |
| sqb-pay | `SqbPayService.pay()` | ★★★★★ |
| sqb-precreate | *未实现* | — |
| sqb-query | `SqbQueryService` | ★★★★★ |
| sqb-refund | `SqbRefundService.refund()` | ★★★★☆ |
| sqb-cancel | *未实现* | — |
| sqb-notify | `SqbNotifyController` | ★★☆☆☆ |
| shared-reference/SqbSignUtil | `SqbSignUtil` | ★★★★★ |
| shared-reference/SqbStatusUtil | `SqbResponse` + `OrderStatus` | ★★★★★ |
| shared-reference/SqbPollingUtil | `SqbQueryService.doPoll()` | ★★★★☆ |

---

*分析基于 sqb-payment-demo (commit 7d4e252) 与 sqb-payment-skills (2026-03-22 版本) 对比完成。*
