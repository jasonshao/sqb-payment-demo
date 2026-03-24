# 收钱吧支付接口对接 Demo

基于 Spring Boot 的收钱吧（Shouqianba）支付 API 集成项目，实现了完整的支付业务功能，包括 B2C 付款码支付、C2B 预创建（客扫商户码）、退款、撤单等。

## 技术栈

- Java 17+
- Spring Boot 3.2.5
- Spring RestClient（HTTP 通信）
- Spring Data JPA + H2（Leaf-segment ID 生成 + 终端凭证持久化）
- Jakarta Bean Validation（声明式参数校验）
- JUnit 5 + Mockito

## 项目结构

```
src/main/java/com/example/sqbpayment/
├── SqbPaymentApplication.java              # 启动类（@EnableScheduling, @EnableAsync）
├── config/
│   ├── SqbConfig.java                      # 配置类（vendor/terminal 凭证，volatile 字段）
│   └── AsyncConfig.java                    # 异步轮询线程池（core=8, max=32, CallerRunsPolicy）
├── controller/
│   ├── GlobalExceptionHandler.java         # 全局异常处理（@RestControllerAdvice）
│   ├── SqbTerminalController.java          # 终端激活 & 签到
│   ├── SqbPayController.java              # B2C 付款码支付
│   ├── SqbPrecreateController.java        # C2B 预创建（客扫商户码）
│   ├── SqbQueryController.java            # 订单查询
│   ├── SqbRefundController.java           # 退款
│   ├── SqbCancelController.java           # 撤单
│   └── SqbNotifyController.java           # 异步回调通知（RSA 验签，幂等 TTL 缓存）
├── credential/
│   ├── TerminalCredentialEntity.java       # 终端凭证 JPA 实体
│   └── TerminalCredentialRepository.java   # 终端凭证 Repository
├── model/
│   ├── ApiResult.java                      # 统一 API 响应包装（Java Record）
│   ├── SqbResponse.java                    # 三层响应解析（含 qrCode 支持）
│   ├── enums/
│   │   └── OrderStatus.java               # 订单状态枚举（含终态判断）
│   ├── request/
│   │   ├── PayCommand.java                # 支付命令（Record + Bean Validation）
│   │   ├── PrecreateCommand.java          # 预创建命令（Record + Bean Validation）
│   │   ├── RefundCommand.java             # 退款命令（Record + 跨字段校验）
│   │   ├── CancelCommand.java            # 撤单命令（Record + 跨字段校验）
│   │   ├── PayRequest.java                # 支付 API 请求 DTO
│   │   ├── PrecreateRequest.java          # 预创建 API 请求 DTO
│   │   ├── RefundRequest.java             # 退款 API 请求 DTO
│   │   ├── CancelRequest.java            # 撤单 API 请求 DTO
│   │   ├── QueryRequest.java              # 查询 API 请求 DTO
│   │   ├── ActivateRequest.java           # 激活 API 请求 DTO
│   │   └── CheckinRequest.java            # 签到 API 请求 DTO
│   └── response/
│       ├── OrderResult.java               # 订单结果（Record，含 qrCode 字段）
│       └── TerminalResult.java            # 终端结果（Record，激活/签到通用）
├── scheduler/
│   └── CheckinScheduler.java             # 定时签到调度器（每日 00:05）
├── service/
│   ├── SqbApiTemplate.java               # API 调用模板（封装序列化→HTTP→解析）
│   ├── SqbPayService.java                # B2C 支付服务（含自动轮询）
│   ├── SqbPrecreateService.java          # C2B 预创建服务（含自动轮询）
│   ├── SqbQueryService.java              # 查询服务（含轮询策略）
│   ├── SqbRefundService.java             # 退款服务（含异步轮询）
│   ├── SqbCancelService.java             # 撤单服务（含 CANCEL_ERROR 查询确认）
│   └── SqbTerminalService.java           # 终端激活 & 签到（凭证持久化 + 密钥回滚）
├── leaf/
│   ├── LeafSegmentService.java            # Leaf-segment 号段服务（双 Buffer）
│   ├── LeafAllocEntity.java               # 号段分配实体
│   ├── LeafAllocRepository.java           # JPA Repository
│   ├── Segment.java                       # ID 号段容器
│   └── SegmentBuffer.java                 # 双缓冲持有者
└── util/
    ├── SqbSignUtil.java                   # MD5 签名工具（常量时间比较）
    ├── SqbRsaUtil.java                    # RSA SHA256WithRSA 验签工具（含异常日志）
    ├── SqbHttpClient.java                 # HTTP 客户端（敏感数据 DEBUG 级日志）
    └── ClientSnGenerator.java             # 全局唯一流水号生成器（基于 Leaf-segment）
```

## API 接口

所有接口统一返回 `ApiResult<T>` 响应体：

```json
{
  "success": true,
  "message": null,
  "data": { ... }
}
```

### 接口列表

| 方法 | 路径 | 说明 | 请求体 | 响应 data |
|------|------|------|--------|-----------|
| POST | `/api/terminal/activate` | 终端激活 | Query: `code`, `deviceId`, `name` | `TerminalResult` |
| POST | `/api/terminal/checkin` | 终端签到 | 无 | `TerminalResult` |
| POST | `/api/pay` | B2C 付款码收款 | `PayCommand` | `OrderResult` |
| POST | `/api/precreate` | C2B 预创建（客扫码） | `PrecreateCommand` | `OrderResult`（含 `qrCode`） |
| POST | `/api/query` | 订单查询 | `{"sn":"..."}` 或 `{"clientSn":"..."}` | `OrderResult` |
| POST | `/api/refund` | 退款 | `RefundCommand` | `OrderResult` |
| POST | `/api/cancel` | 撤单 | `CancelCommand` | `OrderResult` |
| POST | `/api/notify` | 异步回调 | 收钱吧推送 JSON | 纯文本 `success`（验签失败返回 403） |

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

### SqbApiTemplate — 统一 API 调用模板

封装「序列化 → 签名 → HTTP → 解析」的重复逻辑，所有服务通过此模板与收钱吧 API 通信：

```java
// 终端级签名（支付、查询、退款、签到、预创建、撤单）
apiTemplate.call("/upay/v2/pay", payRequest);

// 服务商级签名（仅激活）
apiTemplate.callAsVendor("/terminal/activate", activateRequest);
```

### GlobalExceptionHandler — 集中式异常处理

通过 `@RestControllerAdvice` 统一捕获异常，Controller 无需 try-catch：

| 异常类型 | HTTP 状态码 | 说明 |
|----------|-------------|------|
| `MethodArgumentNotValidException` | 400 | Bean Validation 校验失败 |
| `IllegalArgumentException` | 400 | 业务参数校验失败 |
| `IOException` | 502 | 通信异常 |
| `InterruptedException` | 500 | 请求被中断 |
| `Exception`（兜底） | 500 | 未预期的服务异常 |

### 终端凭证持久化

终端凭证（`terminal_sn`、`terminal_key`）持久化到 H2 数据库，应用重启后自动恢复：

- **激活成功**时写入数据库
- **签到成功**时更新数据库中的 `terminal_key`
- **启动时** `@PostConstruct` 自动从数据库加载凭证到内存
- **签到失败**时回滚 `terminal_key` 为旧值，防止凭证丢失

```sql
CREATE TABLE terminal_credential (
    device_id    VARCHAR(128) PRIMARY KEY,
    terminal_sn  VARCHAR(128) NOT NULL,
    terminal_key VARCHAR(128) NOT NULL,
    update_time  TIMESTAMP
);
```

### RSA 回调验签

异步回调通知使用 **RSA SHA256WithRSA** 签名验证（替代 MD5），验签失败返回 **HTTP 403**：

- 从 `Authorization` 请求头提取签名值
- 使用收钱吧提供的 RSA 公钥验证请求体签名
- 验签失败立即返回 403，不处理业务逻辑

### 定时签到

通过 `@Scheduled` 实现每日自动签到，更新 `terminal_key`：

- 默认 cron：`0 5 0 * * ?`（每天 00:05）
- 可通过 `sqb.checkin-cron` 配置自定义 cron 表达式
- 签到失败记录错误日志，不影响应用运行

### 四级层级关系

```
服务商 (Vendor) → 商户 (Merchant) → 门店 (Store) → 终端 (Terminal)
```

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

当支付/退款/预创建/撤单状态未确定时，自动启动异步轮询（不阻塞 Tomcat 线程）：

| 阶段 | 时间范围 | 查询间隔 |
|------|----------|----------|
| 快速阶段 | 0 ~ 60 秒 | 每 3 秒 |
| 慢速阶段 | 60 ~ 120 秒 | 每 10 秒 |
| 超时 | > 120 秒 | 返回最后查询结果 |

轮询在独立线程池 `pollExecutor`（core=8, max=32, queue=128, `CallerRunsPolicy`）中执行。

### 订单终态

| 终态 | 说明 |
|------|------|
| `PAID` | 支付成功 |
| `PAY_CANCELED` | 支付撤销 |
| `REFUNDED` | 已全额退款 |
| `PARTIAL_REFUNDED` | 已部分退款 |
| `CANCELED` | 已取消 |

### 撤单处理

撤单接口的 `CANCEL_ERROR` 状态表示结果不确定，系统会自动启动查询确认最终状态：

```
CANCEL_SUCCESS → 直接返回（终态）
CANCEL_FAIL    → 直接返回（失败）
CANCEL_ERROR   → 自动轮询查询确认最终状态
```

### Leaf-segment 分布式 ID 生成

采用美团 Leaf 号段模式生成全局唯一流水号，双缓冲 + 异步预加载：

```
┌─────────────────────────────────────┐
│ SegmentBuffer (每个 bizTag 一个)     │
│  segments[0]: 当前号段 [min, max)    │
│  segments[1]: 预加载号段             │
│  currentIndex: 指向当前号段          │
└─────────────────────────────────────┘
```

- **双缓冲**：当前号段使用率达 90% 时异步预加载下一号段
- **零等待**：号段切换无需等待 DB 查询
- **格式**：支付 `yyyyMMdd + 12位序号`（20位），退款 `REF + yyyyMMdd + 12位序号`（23位）

### 回调通知幂等处理

`SqbNotifyController` 使用 `ConcurrentHashMap` + TTL（24 小时）防止重复处理：

- 以订单 `sn` 为去重 key（`putIfAbsent` 原子操作）
- 条目超过 24 小时自动过期清理，缓存上限 10,000 条
- 收钱吧回调重试间隔：1s → 5s → 30s → 600s

### 安全加固

项目实施了多项安全最佳实践：

| 措施 | 说明 |
|------|------|
| **常量时间签名比较** | `SqbSignUtil.verifySign()` 使用 `MessageDigest.isEqual()` 防止时序攻击 |
| **敏感数据日志保护** | `SqbHttpClient` 请求/响应体仅在 DEBUG 级别输出，INFO 仅记录 URL 和 result_code |
| **RSA 验签异常日志** | `SqbRsaUtil` 验签失败时记录 WARN 日志，便于问题排查 |
| **线程池拒绝策略** | `pollExecutor` 使用 `CallerRunsPolicy`，队列满时由调用线程执行，避免任务丢失 |
| **事务一致性** | `LeafSegmentService.loadSegmentFromDb()` 使用 `@Transactional` 确保 UPDATE + SELECT 原子性 |
| **启动配置校验** | `SqbConfig` 实现 `InitializingBean`，启动时校验必要配置并输出 WARN |
| **兜底异常处理** | `GlobalExceptionHandler` 包含 `Exception.class` 兜底处理器，返回统一 `ApiResult` 格式 |
| **回调幂等 TTL** | 回调去重缓存使用 24 小时 TTL 过期机制，替代简单 LRU 淘汰 |

## 配置

编辑 `application.yml` 或通过环境变量配置：

```yaml
sqb:
  api-base: https://vsi-api.shouqianba.com
  vendor-sn: ${SQB_VENDOR_SN}
  vendor-key: ${SQB_VENDOR_KEY}
  app-id: ${SQB_APP_ID}
  terminal-sn: ${SQB_TERMINAL_SN:}
  terminal-key: ${SQB_TERMINAL_KEY:}
  device-id: ${SQB_DEVICE_ID}

  # 回调通知 RSA 公钥（Base64 编码）
  notify-public-key: ${SQB_NOTIFY_PUBLIC_KEY:}

  # 签到定时 cron 表达式（默认每天 00:05）
  checkin-cron: "0 5 0 * * ?"
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
# B2C 付款码支付
curl -X POST http://localhost:8080/api/pay \
  -H 'Content-Type: application/json' \
  -d '{"dynamicId":"285620138893218234","totalAmount":1,"subject":"测试商品","operator":"cashier01"}'

# C2B 预创建（获取二维码）
curl -X POST http://localhost:8080/api/precreate \
  -H 'Content-Type: application/json' \
  -d '{"payway":"4","totalAmount":1,"subject":"测试商品","operator":"cashier01"}'

# 查询订单
curl -X POST http://localhost:8080/api/query \
  -H 'Content-Type: application/json' \
  -d '{"clientSn":"20260321000000000001"}'

# 发起退款
curl -X POST http://localhost:8080/api/refund \
  -H 'Content-Type: application/json' \
  -d '{"sn":"789284025","refundAmount":1,"operator":"cashier01","refundReason":"测试退款"}'

# 撤单
curl -X POST http://localhost:8080/api/cancel \
  -H 'Content-Type: application/json' \
  -d '{"sn":"789284025"}'
```

## 单元测试

项目包含 138 个单元测试，覆盖全部业务功能：

| 分类 | 测试类 | 测试数 |
|------|--------|--------|
| 工具类 | `SqbSignUtilTest`, `SqbRsaUtilTest`, `ClientSnGeneratorTest` | 22 |
| 模型层 | `OrderStatusTest`, `SqbResponseTest` | 21 |
| 服务层 | `SqbTerminalServiceTest`, `SqbPayServiceTest`, `SqbQueryServiceTest`, `SqbRefundServiceTest`, `SqbPrecreateServiceTest`, `SqbCancelServiceTest` | 70 |
| 控制器 | `SqbNotifyControllerTest`, `SqbPrecreateControllerTest`, `SqbCancelControllerTest`, `GlobalExceptionHandlerTest` | 17 |
| 配置 | `AsyncConfigTest` | 1 |
| 调度器 | `CheckinSchedulerTest` | 3 |
| ID 生成 | `LeafSegmentServiceTest` | 5 |

测试采用 Mockito `ArgumentCaptor` 进行类型安全的请求参数断言，替代脆弱的 JSON 字符串匹配。

## 重要提示

- `client_sn` 必须全局唯一，支付失败后不可复用
- 终端凭证自动持久化到 H2 数据库，应用重启后自动恢复
- 终端签到后 `terminal_key` 会更新，签到失败时自动回滚旧密钥
- 异步回调使用 RSA SHA256WithRSA 验签，验签失败返回 HTTP 403
- 异步回调不能替代主动轮询查询
- 回调需要返回纯文本 `success`，否则会按 1s、5s、30s、600s 间隔重试
- 金额统一使用 `long` 类型（单位：分），仅在构建 API 请求时转为 String
- `terminalSn` 和 `terminalKey` 使用 `volatile` 修饰，支持运行时动态更新
- 撤单的 `CANCEL_ERROR` 状态需通过查询确认最终结果
