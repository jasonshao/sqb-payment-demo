# 收钱吧支付接口对接 Demo

基于 Spring Boot 的收钱吧（Shouqianba）B2C 支付 API 集成项目，实现了完整的支付业务功能。

## 技术栈

- Java 17+
- Spring Boot 3.2.5
- OkHttp 4.12.0
- Jackson
- JUnit 5 + Mockito

## 项目结构

```
src/main/java/com/example/sqbpayment/
├── SqbPaymentApplication.java          # 启动类（@EnableScheduling）
├── config/
│   └── SqbConfig.java                  # 配置类（vendor/terminal 凭证）
├── controller/
│   ├── SqbTerminalController.java      # 终端激活 & 签到
│   ├── SqbPayController.java           # B2C 支付
│   ├── SqbQueryController.java         # 订单查询
│   ├── SqbRefundController.java        # 退款
│   └── SqbNotifyController.java        # 异步回调通知
├── model/
│   ├── SqbResponse.java                # 三层响应解析
│   ├── enums/
│   │   └── OrderStatus.java            # 订单状态枚举
│   └── request/                        # 请求模型
│       ├── ActivateRequest.java
│       ├── CheckinRequest.java
│       ├── PayRequest.java
│       ├── QueryRequest.java
│       └── RefundRequest.java
├── service/
│   ├── SqbTerminalService.java         # 终端激活 & 签到服务
│   ├── SqbPayService.java              # 支付服务（含自动轮询）
│   ├── SqbQueryService.java            # 查询服务（含轮询策略）
│   └── SqbRefundService.java           # 退款服务（含异步轮询）
└── util/
    ├── SqbSignUtil.java                # MD5 签名工具
    ├── SqbHttpClient.java              # HTTP 客户端（OkHttp）
    └── ClientSnGenerator.java          # 全局唯一流水号生成器
```

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/terminal/activate` | 终端激活（激活码仅能使用一次） |
| POST | `/api/terminal/checkin` | 终端签到（每日首笔交易前建议执行） |
| POST | `/api/pay` | B2C 收款（商家扫用户付款码） |
| POST | `/api/query` | 订单查询（支持 sn / client_sn） |
| POST | `/api/refund` | 退款（支持全额/部分退款） |
| POST | `/api/notify` | 异步回调通知接收端 |

## 核心设计

### 四级层级关系

```
服务商 (Vendor) → 商户 (Merchant) → 门店 (Store) → 终端 (Terminal)
```

### 两级签名机制

- **Vendor 级签名**：仅用于终端激活 (`vendor_sn` + `vendor_key`)
- **Terminal 级签名**：用于所有其他接口 (`terminal_sn` + `terminal_key`)
- **签名算法**：`Authorization: {sn} {MD5(request_body + key)}`

### 三层响应解析

1. **通讯层** — `result_code`：HTTP 通讯是否成功
2. **业务层** — `biz_response.result_code`：业务处理结果
3. **订单状态** — `biz_response.data.order_status`：交易最终状态

### 轮询策略

- 前 60 秒：每 3 秒查询一次
- 60 秒后：每 10 秒查询一次
- 最大超时：120 秒

### 订单终态

| 终态 | 说明 |
|------|------|
| PAID | 支付成功 |
| PAY_CANCELED | 支付撤销 |
| REFUNDED | 已全额退款 |
| PARTIAL_REFUNDED | 已部分退款 |
| CANCELED | 已取消 |

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

## 单元测试

项目包含 90 个单元测试，覆盖全部业务功能：

| 分类 | 测试类 | 测试数 |
|------|--------|--------|
| 工具类 | SqbSignUtilTest, ClientSnGeneratorTest | 14 |
| 模型层 | OrderStatusTest, SqbResponseTest | 21 |
| 服务层 | SqbTerminalServiceTest, SqbPayServiceTest, SqbQueryServiceTest, SqbRefundServiceTest | 47 |
| 控制器 | SqbNotifyControllerTest | 8 |

详细测试报告见 [TEST-REPORT.md](TEST-REPORT.md)。

## 重要提示

- `client_sn` 必须全局唯一，支付失败后不可复用
- 终端签到后 `terminal_key` 会更新，必须立即持久化
- 异步回调不能替代主动轮询查询
- 回调需要返回纯文本 `success`，否则会按 1s、5s、30s、600s 间隔重试
