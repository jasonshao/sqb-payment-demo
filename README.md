# 收钱吧支付接口对接 Demo

基于 Spring Boot 的收钱吧（Shouqianba）B2C 支付 API 集成项目，实现了完整的支付业务功能。

## 技术栈

- Java 17+
- Spring Boot 3.2.5
- Spring RestClient（HTTP 通信）
- Spring Data JPA + H2（Leaf-segment ID 生成）
- Jakarta Bean Validation（声明式参数校验）
- JUnit 5 + Mockito

## 项目结构

```
src/main/java/com/example/sqbpayment/
├── SqbPaymentApplication.java              # 启动类（@EnableScheduling, @EnableAsync）
├── config/
│   ├── SqbConfig.java                      # 配置类（vendor/terminal 凭证，volatile 字段）
│   └── AsyncConfig.java                    # 异步轮询线程池（core=8, max=32）
├── controller/
│   ├── GlobalExceptionHandler.java         # 全局异常处理（@RestControllerAdvice）
│   ├── SqbTerminalController.java          # 终端激活 & 签到
│   ├── SqbPayController.java              # B2C 支付
│   ├── SqbQueryController.java            # 订单查询
│   ├── SqbRefundController.java           # 退款
│   └── SqbNotifyController.java           # 异步回调通知（幂等，有界 LRU 缓存）
├── model/
│   ├── ApiResult.java                      # 统一 API 响应包装（Java Record）
│   ├── SqbResponse.java                    # 三层响应解析
│   ├── enums/
│   │   └── OrderStatus.java               # 订单状态枚举（含终态判断）
│   ├── request/
│   │   ├── PayCommand.java                # 支付命令（Record + Bean Validation）
│   │   ├── RefundCommand.java             # 退款命令（Record + 跨字段校验）
│   │   ├── PayRequest.java                # 支付 API 请求 DTO
│   │   ├── RefundRequest.java             # 退款 API 请求 DTO
│   │   ├── QueryRequest.java              # 查询 API 请求 DTO
│   │   ├── ActivateRequest.java           # 激活 API 请求 DTO
│   │   └── CheckinRequest.java            # 签到 API 请求 DTO
│   └── response/
│       ├── OrderResult.java               # 订单结果（Record，支付/查询/退款通用）
│       └── TerminalResult.java            # 终端结果（Record，激活/签到通用）
├── service/
│   ├── SqbApiTemplate.java               # API 调用模板（封装序列化→HTTP→解析）
│   ├── SqbPayService.java                # 支付服务（含自动轮询）
│   ├── SqbQueryService.java              # 查询服务（含轮询策略）
│   ├── SqbRefundService.java             # 退款服务（含异步轮询）
│   └── SqbTerminalService.java           # 终端激活 & 签到服务
├── leaf/
│   ├── LeafSegmentService.java            # Leaf-segment 号段服务（双 Buffer）
│   ├── LeafAllocEntity.java               # 号段分配实体
│   ├── LeafAllocRepository.java           # JPA Repository
│   ├── Segment.java                       # ID 号段容器
│   └── SegmentBuffer.java                 # 双缓冲持有者
└── util/
    ├── SqbSignUtil.java                   # MD5 签名工具
    ├── SqbHttpClient.java                 # HTTP 客户端（Spring RestClient）
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
| POST | `/api/pay` | B2C 收款 | `PayCommand` | `OrderResult` |
| POST | `/api/query` | 订单查询 | `{"sn":"..."}` 或 `{"clientSn":"..."}` | `OrderResult` |
| POST | `/api/refund` | 退款 | `RefundCommand` | `OrderResult` |
| POST | `/api/notify` | 异步回调 | 收钱吧推送 JSON | 纯文本 `success` |

### 请求参数

**PayCommand（支付）**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `dynamicId` | String | 是 | 用户付款码内容 |
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

## 核心架构设计

### SqbApiTemplate — 统一 API 调用模板

封装「序列化 → 签名 → HTTP → 解析」的重复逻辑，所有服务通过此模板与收钱吧 API 通信：

```java
// 终端级签名（支付、查询、退款、签到）
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

当支付/退款状态未确定时，自动启动异步轮询（不阻塞 Tomcat 线程）：

| 阶段 | 时间范围 | 查询间隔 |
|------|----------|----------|
| 快速阶段 | 0 ~ 60 秒 | 每 3 秒 |
| 慢速阶段 | 60 ~ 120 秒 | 每 10 秒 |
| 超时 | > 120 秒 | 返回最后查询结果 |

轮询在独立线程池 `pollExecutor`（core=8, max=32, queue=128）中执行。

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

数据库表：

```sql
CREATE TABLE leaf_alloc (
    biz_tag     VARCHAR(128) PRIMARY KEY,  -- 业务标识 (PAY / REFUND)
    max_id      BIGINT DEFAULT 0,          -- 当前最大 ID
    step        INT DEFAULT 2000,          -- 每次分配步长
    update_time TIMESTAMP
);
```

### 回调通知幂等处理

`SqbNotifyController` 使用有界 LRU 缓存（最大 10,000 条）防止重复处理：

- 以订单 `sn` 为去重 key（`putIfAbsent` 原子操作）
- 超过上限自动淘汰最久未访问的记录，防止内存泄漏
- 收钱吧回调重试间隔：1s → 5s → 30s → 600s

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
```

> **注意**：收钱吧没有沙箱环境，所有交易均为真实交易。金额单位为分（1 元 = 100 分）。

## 快速开始

```bash
# 设置环境变量
export SQB_VENDOR_SN=your_vendor_sn
export SQB_VENDOR_KEY=your_vendor_key
export SQB_APP_ID=your_app_id
export SQB_DEVICE_ID=your_device_id

# 编译运行
mvn spring-boot:run

# 运行单元测试
mvn test
```

### 使用流程

1. **激活终端** — 使用激活码调用 `/api/terminal/activate`，获取 `terminal_sn` 和 `terminal_key`
2. **终端签到** — 每日首笔交易前调用 `/api/terminal/checkin`，更新 `terminal_key`
3. **发起支付** — 扫描用户付款码，调用 `/api/pay`
4. **查询订单** — 调用 `/api/query` 查询交易状态
5. **退款** — 调用 `/api/refund` 发起退款

### 请求示例

```bash
# 发起支付
curl -X POST http://localhost:8080/api/pay \
  -H 'Content-Type: application/json' \
  -d '{"dynamicId":"285620138893218234","totalAmount":1,"subject":"测试商品","operator":"cashier01"}'

# 查询订单
curl -X POST http://localhost:8080/api/query \
  -H 'Content-Type: application/json' \
  -d '{"clientSn":"20260321000000000001"}'

# 发起退款
curl -X POST http://localhost:8080/api/refund \
  -H 'Content-Type: application/json' \
  -d '{"sn":"789284025","refundAmount":1,"operator":"cashier01","refundReason":"测试退款"}'
```

## 单元测试

项目包含 97 个单元测试，覆盖全部业务功能：

| 分类 | 测试类 | 测试数 |
|------|--------|--------|
| 工具类 | `SqbSignUtilTest`, `ClientSnGeneratorTest` | 16 |
| 模型层 | `OrderStatusTest`, `SqbResponseTest` | 21 |
| 服务层 | `SqbTerminalServiceTest`, `SqbPayServiceTest`, `SqbQueryServiceTest`, `SqbRefundServiceTest` | 46 |
| 控制器 | `SqbNotifyControllerTest` | 8 |
| ID 生成 | `LeafSegmentServiceTest` | 6 |

测试采用 Mockito `ArgumentCaptor` 进行类型安全的请求参数断言，替代脆弱的 JSON 字符串匹配。

## 重要提示

- `client_sn` 必须全局唯一，支付失败后不可复用
- 终端签到后 `terminal_key` 会更新，必须立即持久化
- 异步回调不能替代主动轮询查询
- 回调需要返回纯文本 `success`，否则会按 1s、5s、30s、600s 间隔重试
- 金额统一使用 `long` 类型（单位：分），仅在构建 API 请求时转为 String
- `terminalSn` 和 `terminalKey` 使用 `volatile` 修饰，支持运行时动态更新
