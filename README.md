# 收钱吧支付接口对接 Demo

基于 Spring Boot 的收钱吧（Shouqianba）支付 API 集成项目，采用 **Stripe 风格 SDK 分层架构**，实现了完整的支付业务功能，包括 B2C 付款码支付、C2B 预创建（客扫商户码）、退款、撤单等。

## 技术栈

- Java 17+
- Spring Boot 3.2.5
- Spring Security（HTTP Basic 认证，无状态会话）
- Spring RestClient（HTTP 通信，指数退避重试）
- Spring Data JPA + H2（订单聚合、凭证持久化、幂等去重）
- Jakarta Bean Validation（声明式参数校验）
- JUnit 5 + Mockito

## SDK 分层架构

项目参考 Stripe SDK 设计，将支付集成拆分为四层：

```
┌─────────────────────────────────────────────────────────────┐
│  Web Adapter (Controller)     ← 薄适配层，无业务逻辑        │
├─────────────────────────────────────────────────────────────┤
│  Domain Operations            ← 业务操作接口 + 默认实现      │
│  SqbClient 门面              ← 统一入口                     │
├─────────────────────────────────────────────────────────────┤
│  ResponseGetter               ← 签名、序列化、调 transport   │
├─────────────────────────────────────────────────────────────┤
│  Transport                    ← HTTP 通信（RestClient）      │
└─────────────────────────────────────────────────────────────┘
```

### SqbClient — 统一入口门面

```java
@Autowired
private SqbClient sqbClient;

// 支付
sqbClient.payments().pay(command, options);
sqbClient.payments().precreate(command, options);

// 退款 / 撤单 / 查询
sqbClient.refunds().refund(command, options);
sqbClient.cancels().cancel(command, options);
sqbClient.queries().queryByClientSn(clientSn, options);

// 终端管理
sqbClient.terminals().activate(code, deviceId, name, options);
sqbClient.terminals().checkin(options);

// 回调验签
SqbWebhookEvent event = sqbClient.webhooks().verifyAndParse(payload, authorization, webhookOptions);
```

## 项目结构

```
src/main/java/com/example/sqbpayment/
├── SqbPaymentApplication.java              # 启动类（@EnableScheduling, @EnableAsync）
│
├── sdk/                                     # ★ SDK 核心层
│   ├── SqbClient.java                      # 统一入口门面接口
│   ├── DefaultSqbClient.java               # 门面默认实现（委托各 Operations）
│   ├── SqbRequestOptions.java              # 每请求级配置（Builder 模式）
│   ├── exception/                           # 异常体系（全部 unchecked）
│   │   ├── SqbException.java               # 基础异常（含 requestId, httpStatus, retryable）
│   │   ├── SqbApiException.java            # API 业务错误
│   │   ├── SqbApiConnectionException.java  # 网络连接失败（retryable）
│   │   ├── SqbApiTimeoutException.java     # 请求超时（retryable）
│   │   ├── SqbAuthenticationException.java # 认证失败
│   │   ├── SqbSignatureVerificationException.java # 签名验证失败
│   │   ├── SqbRateLimitException.java      # 限流
│   │   └── SqbDeserializationException.java # 反序列化失败
│   ├── transport/                           # 传输层
│   │   ├── SqbTransport.java               # 传输接口
│   │   ├── SqbRawRequest.java              # 原始 HTTP 请求
│   │   ├── SqbRawResponse.java             # 原始 HTTP 响应
│   │   └── RestClientSqbTransport.java     # RestClient 实现（指数退避 + 抖动重试）
│   ├── responsegetter/                      # 响应获取层
│   │   ├── SqbResponseGetter.java          # 响应获取器接口
│   │   ├── SqbApiRequest.java              # API 请求封装（含 SqbRequestOptions）
│   │   └── LiveSqbResponseGetter.java      # 实现：签名→序列化→transport→解析
│   ├── signing/                             # 签名工具
│   │   ├── SqbSignUtil.java                # MD5 签名
│   │   └── SqbRsaUtil.java                 # RSA SHA256WithRSA 验签
│   └── webhook/                             # 回调通知
│       ├── SqbWebhookOperations.java       # 验签接口
│       ├── DefaultSqbWebhookOperations.java # 验签实现
│       ├── SqbWebhookEvent.java            # 回调事件（Record）
│       └── SqbWebhookOptions.java          # 回调配置（公钥、时间容差）
│
├── domain/                                  # ★ 领域层
│   ├── operations/                          # 业务操作接口 + 实现
│   │   ├── SqbPaymentOperations.java       # 支付操作接口（pay, precreate）
│   │   ├── SqbRefundOperations.java        # 退款操作接口
│   │   ├── SqbCancelOperations.java        # 撤单操作接口
│   │   ├── SqbQueryOperations.java         # 查询操作接口
│   │   ├── SqbTerminalOperations.java      # 终端操作接口（activate, checkin）
│   │   ├── DefaultPaymentOperations.java   # 支付实现
│   │   ├── DefaultRefundOperations.java    # 退款实现
│   │   ├── DefaultCancelOperations.java    # 撤单实现
│   │   ├── DefaultQueryOperations.java     # 查询实现
│   │   └── DefaultTerminalOperations.java  # 终端实现
│   ├── credential/                          # 凭证管理
│   │   ├── SqbCredentialProvider.java      # 凭证提供者接口
│   │   ├── DatabaseCredentialProvider.java # DB + 内存缓存 write-through 实现
│   │   ├── VendorCredential.java           # 服务商凭证（Record）
│   │   └── TerminalCredential.java         # 终端凭证（Record）
│   ├── order/                               # 订单聚合
│   │   ├── PaymentOrder.java               # 订单 JPA 实体（@Version 乐观锁）
│   │   ├── PaymentOrderRepository.java     # 订单 Repository
│   │   ├── OrderStateService.java          # 状态机服务（终态守卫）
│   │   ├── IdempotencyRecord.java          # 幂等记录 JPA 实体
│   │   └── IdempotencyRepository.java      # 幂等 Repository
│   └── polling/
│       └── PollingService.java             # 通用轮询服务（两阶段策略）
│
├── infrastructure/                          # ★ 基础设施层
│   ├── config/
│   │   ├── SecurityConfig.java             # Spring Security 配置（无状态 + HTTP Basic）
│   │   ├── SqbSdkConfig.java              # SDK Bean 装配（Transport, ResponseGetter）
│   │   └── SqbPollingProperties.java       # 轮询参数配置（@ConfigurationProperties）
│   └── observability/
│       ├── RequestIdFilter.java            # MDC requestId 过滤器（X-Request-Id 传播）
│       └── LogSanitizer.java              # 日志脱敏（terminal_key, dynamic_id 等）
│
├── config/
│   ├── SqbConfig.java                      # 业务配置（vendor/terminal 凭证）
│   └── AsyncConfig.java                    # 异步线程池（core=8, max=32, AbortPolicy）
├── controller/                              # Web 适配层
│   ├── GlobalExceptionHandler.java         # 全局异常处理（含 SqbException 体系）
│   ├── SqbPayController.java              # B2C 付款码支付
│   ├── SqbPrecreateController.java        # C2B 预创建（客扫商户码）
│   ├── SqbQueryController.java            # 订单查询
│   ├── SqbRefundController.java           # 退款
│   ├── SqbCancelController.java           # 撤单
│   ├── SqbTerminalController.java          # 终端激活 & 签到
│   └── SqbNotifyController.java           # 异步回调通知（DB 幂等去重）
├── credential/
│   ├── TerminalCredentialEntity.java       # 终端凭证 JPA 实体
│   └── TerminalCredentialRepository.java   # 终端凭证 Repository
├── model/
│   ├── ApiResult.java                      # 统一 API 响应包装（Java Record）
│   ├── SqbResponse.java                    # 三层响应解析（含 qrCode 支持）
│   ├── enums/
│   │   └── OrderStatus.java               # 订单状态枚举（含终态判断）
│   ├── request/                            # 命令 + API 请求 DTO
│   └── response/                           # 响应 DTO
├── service/                                 # 旧版服务层（向后兼容，逐步迁移）
│   ├── SqbApiTemplate.java               # API 调用模板（委托 ResponseGetter）
│   ├── SqbPayService.java                # B2C 支付服务（含自动轮询）
│   ├── SqbPrecreateService.java          # C2B 预创建服务
│   ├── SqbQueryService.java              # 查询服务（含轮询策略）
│   ├── SqbRefundService.java             # 退款服务
│   ├── SqbCancelService.java             # 撤单服务
│   └── SqbTerminalService.java           # 终端激活 & 签到
├── scheduler/
│   └── CheckinScheduler.java             # 定时签到调度器（每日 00:05）
├── leaf/                                   # Leaf-segment 分布式 ID 生成
│   ├── LeafSegmentService.java            # 号段服务（双 Buffer + 异步预加载）
│   └── ...
└── util/
    └── ClientSnGenerator.java             # 全局唯一流水号生成器
```

## API 接口

所有 `/api/**` 接口（除 `/api/notify` 外）需要 HTTP Basic 认证。

```json
{
  "success": true,
  "message": null,
  "data": { ... }
}
```

### 接口列表

| 方法 | 路径 | 说明 | 认证 | 请求体 | 响应 data |
|------|------|------|------|--------|-----------|
| POST | `/api/terminal/activate` | 终端激活 | Basic | Query: `code`, `deviceId`, `name` | `TerminalResult` |
| POST | `/api/terminal/checkin` | 终端签到 | Basic | 无 | `TerminalResult` |
| POST | `/api/pay` | B2C 付款码收款 | Basic | `PayCommand` | `OrderResult` |
| POST | `/api/precreate` | C2B 预创建（客扫码） | Basic | `PrecreateCommand` | `OrderResult`（含 `qrCode`） |
| POST | `/api/query` | 订单查询 | Basic | `{"sn":"..."}` 或 `{"clientSn":"..."}` | `OrderResult` |
| POST | `/api/refund` | 退款 | Basic | `RefundCommand` | `OrderResult` |
| POST | `/api/cancel` | 撤单 | Basic | `CancelCommand` | `OrderResult` |
| POST | `/api/notify` | 异步回调 | 无（RSA 验签） | 收钱吧推送 JSON | 纯文本 `success` |

### 请求参数

**PayCommand（B2C 付款码支付）**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `dynamicId` | String | 是 | 用户付款码内容 |
| `totalAmount` | long | 是 | 金额（单位：分） |
| `subject` | String | 是 | 交易简介 |
| `operator` | String | 是 | 操作员 |
| `notifyUrl` | String | 否 | 异步回调地址 |

**PrecreateCommand（C2B 预创建）**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `payway` | String | 是 | 支付方式：`3`-微信，`4`-支付宝 |
| `totalAmount` | long | 是 | 金额（单位：分） |
| `subject` | String | 是 | 交易简介 |
| `operator` | String | 是 | 操作员 |
| `notifyUrl` | String | 否 | 异步回调地址 |

**RefundCommand（退款）**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `sn` | String | 二选一 | 收钱吧订单号 |
| `clientSn` | String | 二选一 | 商户订单号 |
| `refundAmount` | long | 是 | 退款金额（单位：分） |
| `operator` | String | 是 | 操作员 |
| `refundReason` | String | 否 | 退款原因 |

**CancelCommand（撤单）**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `sn` | String | 二选一 | 收钱吧订单号 |
| `clientSn` | String | 二选一 | 商户订单号 |

## 核心架构设计

### 异常体系

所有异常继承 `SqbException`（unchecked），消除 `throws IOException`：

| 异常类型 | 说明 | 可重试 |
|----------|------|--------|
| `SqbApiException` | API 业务错误 | 否 |
| `SqbApiConnectionException` | 网络连接失败 | 是 |
| `SqbApiTimeoutException` | 请求超时 | 是 |
| `SqbAuthenticationException` | 认证失败 | 否 |
| `SqbSignatureVerificationException` | 签名验证失败 | 否 |
| `SqbRateLimitException` | 限流 | 是 |
| `SqbDeserializationException` | 反序列化失败 | 否 |

### Transport 层 — 指数退避重试

`RestClientSqbTransport` 基于 Spring RestClient，对网络错误和超时自动重试：

- 最大重试 3 次
- 退避间隔：`baseDelay * 2^attempt + random jitter`
- 仅对 `retryable` 异常重试（连接失败、超时）

### 凭证管理 — CredentialProvider 抽象

`SqbCredentialProvider` 接口替代直接读写 `SqbConfig` volatile 字段：

```java
public interface SqbCredentialProvider {
    VendorCredential getVendorCredential();
    TerminalCredential getTerminalCredential(String deviceId);
    void updateTerminalCredential(String deviceId, TerminalCredential credential);
}
```

`DatabaseCredentialProvider` 实现 **ConcurrentHashMap 内存缓存 + DB write-through**，激活/签到后自动持久化。

### 订单聚合 — 乐观锁状态机

`PaymentOrder` JPA 实体使用 `@Version` 乐观锁防止并发覆盖：

```java
@Entity
public class PaymentOrder {
    @Version private int version;
    @Enumerated(EnumType.STRING) private OrderStatus currentStatus;
    // ...
}
```

`OrderStateService` 提供终态守卫 — 已到达终态的订单不可再变更。

### 回调幂等 — DB 去重

`IdempotencyRecord` 替代内存 `ConcurrentHashMap`，重启后幂等记录不丢失：

```sql
CREATE TABLE idempotency_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(256) NOT NULL UNIQUE,
    event_type VARCHAR(64),
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 安全

- **Spring Security** — 无状态会话，CSRF 禁用，`/api/notify` 匿名访问（自带 RSA 验签），其余 `/api/**` 需 HTTP Basic
- **RSA 回调验签** — `SqbWebhookOperations.verifyAndParse()` 验签 + 解析一体化
- **日志脱敏** — `LogSanitizer` 对 `terminal_key`、`dynamic_id`、`Authorization` 字段自动掩码

### 可观测性

- **RequestIdFilter** — 生成/传播 `X-Request-Id`，写入 MDC，所有日志自动携带 `requestId`
- **日志格式** — `%d [%thread] [%X{requestId}] %-5level %logger - %msg`

### 两级签名机制

- **Vendor 级签名**：仅用于终端激活 (`vendor_sn` + `vendor_key`)
- **Terminal 级签名**：用于所有其他接口 (`terminal_sn` + `terminal_key`)
- **签名算法**：`Authorization: {sn} {MD5(request_body + key)}`

### 三层响应解析

```
HTTP 响应
  └─ 通讯层: result_code (200=成功)
       └─ 业务层: biz_response.result_code (PAY_SUCCESS / PAY_FAIL / ...)
            └─ 数据层: biz_response.data (order_status, sn, amount, ...)
```

### 异步轮询策略

`PollingService` 提供可配置的两阶段轮询，参数通过 `sqb.polling.*` 外部化：

| 阶段 | 时间范围 | 查询间隔 |
|------|----------|----------|
| 快速阶段 | 0 ~ 超时/2 | 每 3 秒（默认） |
| 慢速阶段 | 超时/2 ~ 超时 | 每 10 秒（默认） |
| 超时 | > 120 秒（默认） | 返回最后查询结果 |

轮询在独立线程池 `pollExecutor`（core=8, max=32, queue=128, AbortPolicy）中执行。

### 订单终态

| 终态 | 说明 |
|------|------|
| `PAID` | 支付成功 |
| `PAY_CANCELED` | 支付撤销 |
| `REFUNDED` | 已全额退款 |
| `PARTIAL_REFUNDED` | 已部分退款 |
| `CANCELED` | 已取消 |

### Leaf-segment 分布式 ID 生成

采用美团 Leaf 号段模式生成全局唯一流水号，双缓冲 + 异步预加载：

- **双缓冲**：当前号段使用率达 90% 时异步预加载下一号段
- **零等待**：号段切换无需等待 DB 查询
- **格式**：支付 `yyyyMMdd + 12位序号`（20位），退款 `REF + yyyyMMdd + 12位序号`（23位）

## 配置

编辑 `application.yml` 或通过环境变量配置：

```yaml
spring:
  security:
    user:
      name: ${SQB_API_USER:admin}
      password: ${SQB_API_PASSWORD:changeme}

sqb:
  api-base: https://vsi-api.shouqianba.com
  vendor-sn: ${SQB_VENDOR_SN:}
  vendor-key: ${SQB_VENDOR_KEY:}
  app-id: ${SQB_APP_ID:}
  terminal-sn: ${SQB_TERMINAL_SN:}
  terminal-key: ${SQB_TERMINAL_KEY:}
  device-id: ${SQB_DEVICE_ID:}
  notify-public-key: ${SQB_NOTIFY_PUBLIC_KEY:}
  checkin-cron: "0 5 0 * * ?"

  # 轮询参数（可选）
  polling:
    enabled: true
    timeout-ms: 120000
    fast-interval-ms: 3000
    slow-interval-ms: 10000
```

> **注意**：收钱吧没有沙箱环境，所有交易均为真实交易。金额单位为分（1 元 = 100 分）。

## 快速开始

```bash
# 设置环境变量
export SQB_VENDOR_SN=your_vendor_sn
export SQB_VENDOR_KEY=your_vendor_key
export SQB_APP_ID=your_app_id
export SQB_DEVICE_ID=your_device_id
export SQB_NOTIFY_PUBLIC_KEY=your_rsa_public_key_base64
export SQB_API_USER=admin
export SQB_API_PASSWORD=your_password

# 编译运行
mvn spring-boot:run

# 运行单元测试
mvn test
```

### 使用流程

1. **激活终端** — 使用激活码调用 `/api/terminal/activate`，获取 `terminal_sn` 和 `terminal_key`（自动持久化到数据库）
2. **终端签到** — 每日自动执行（00:05），也可手动调用 `/api/terminal/checkin`，更新 `terminal_key`
3. **发起支付** — B2C：扫描用户付款码，调用 `/api/pay`；C2B：调用 `/api/precreate` 获取二维码让用户扫码
4. **查询订单** — 调用 `/api/query` 查询交易状态
5. **退款** — 调用 `/api/refund` 发起退款
6. **撤单** — 调用 `/api/cancel` 撤销未完成的交易

### 请求示例

```bash
# B2C 付款码支付（需 HTTP Basic 认证）
curl -u admin:changeme -X POST http://localhost:8080/api/pay \
  -H 'Content-Type: application/json' \
  -d '{"dynamicId":"285620138893218234","totalAmount":1,"subject":"测试商品","operator":"cashier01"}'

# C2B 预创建（获取二维码）
curl -u admin:changeme -X POST http://localhost:8080/api/precreate \
  -H 'Content-Type: application/json' \
  -d '{"payway":"4","totalAmount":1,"subject":"测试商品","operator":"cashier01"}'

# 查询订单
curl -u admin:changeme -X POST http://localhost:8080/api/query \
  -H 'Content-Type: application/json' \
  -d '{"clientSn":"20260321000000000001"}'

# 发起退款
curl -u admin:changeme -X POST http://localhost:8080/api/refund \
  -H 'Content-Type: application/json' \
  -d '{"sn":"789284025","refundAmount":1,"operator":"cashier01","refundReason":"测试退款"}'

# 撤单
curl -u admin:changeme -X POST http://localhost:8080/api/cancel \
  -H 'Content-Type: application/json' \
  -d '{"sn":"789284025"}'
```

## 单元测试

项目包含 140 个单元测试，覆盖全部业务功能：

| 分类 | 测试类 | 测试数 |
|------|--------|--------|
| 工具类 | `SqbSignUtilTest`, `SqbRsaUtilTest`, `ClientSnGeneratorTest` | 21 |
| 模型层 | `OrderStatusTest`, `SqbResponseTest` | 21 |
| 服务层 | `SqbTerminalServiceTest`, `SqbPayServiceTest`, `SqbQueryServiceTest`, `SqbRefundServiceTest`, `SqbPrecreateServiceTest`, `SqbCancelServiceTest` | 70 |
| 控制器 | `SqbNotifyControllerTest`, `SqbPrecreateControllerTest`, `SqbCancelControllerTest`, `GlobalExceptionHandlerTest` | 17 |
| 调度器 | `CheckinSchedulerTest` | 3 |
| 配置 | `AsyncConfigTest` | 3 |
| ID 生成 | `LeafSegmentServiceTest` | 5 |

## 数据库表

```sql
-- 终端凭证
CREATE TABLE terminal_credential (
    device_id    VARCHAR(128) PRIMARY KEY,
    terminal_sn  VARCHAR(128),
    terminal_key VARCHAR(128),
    update_time  TIMESTAMP
);

-- 订单聚合（@Version 乐观锁）
CREATE TABLE payment_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_order_no VARCHAR(64) NOT NULL,
    channel_order_no VARCHAR(64),
    current_status VARCHAR(30) NOT NULL,
    amount BIGINT,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 回调幂等去重
CREATE TABLE idempotency_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(256) NOT NULL UNIQUE,
    event_type VARCHAR(64),
    processed_at TIMESTAMP
);

-- Leaf-segment 号段分配
CREATE TABLE leaf_alloc (
    biz_tag  VARCHAR(128) PRIMARY KEY,
    max_id   BIGINT NOT NULL DEFAULT 0,
    step     INT NOT NULL DEFAULT 2000
);
```

## 重要提示

- `client_sn` 必须全局唯一，支付失败后不可复用
- 终端凭证通过 `DatabaseCredentialProvider` 自动持久化，应用重启后自动恢复
- 终端签到后 `terminal_key` 会更新，通过 `CredentialProvider` 原子写入缓存和 DB
- 异步回调使用 RSA SHA256WithRSA 验签（`SqbWebhookOperations`），验签失败返回 HTTP 403
- 异步回调不能替代主动轮询查询
- 回调需要返回纯文本 `success`，否则会按 1s、5s、30s、600s 间隔重试
- 金额统一使用 `long` 类型（单位：分），仅在构建 API 请求时转为 String
- 所有 API 异常为 unchecked（`SqbException` 体系），无需 `throws` 声明
- 撤单的 `CANCEL_ERROR` 状态需通过查询确认最终结果
- 回调幂等已从内存 LRU 缓存迁移到 DB 持久化，重启后不丢失
