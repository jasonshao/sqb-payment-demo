# 收钱吧支付SDK - 单元测试报告

**生成时间**: 2026-03-19 01:36:23 UTC
**项目**: sqb-payment-demo
**Spring Boot**: 3.2.5
**Java**: 17+

## 测试概览

| 指标 | 数值 |
|------|------|
| 测试总数 | 90 |
| 通过 | 90 |
| 失败 | 0 |
| 错误 | 0 |
| 跳过 | 0 |
| 通过率 | 100.0% |
| 总耗时 | 12.598s |

## 测试套件详情

### SqbNotifyControllerTest

- **包名**: `com.example.sqbpayment.controller`
- **测试数**: 8 | **耗时**: 1.662s | **状态**: PASS

| # | 测试方法 | 状态 | 耗时 |
|---|---------|------|------|
| 1 | `testNotifyWithMissingAuthorization` | PASS | 0.071s |
| 2 | `testNotifyWithFallbackToStatusField` | PASS | 0.007s |
| 3 | `testNotifyWithInvalidSignature` | PASS | 0.005s |
| 4 | `testNotifyPartialRefunded` | PASS | 0.005s |
| 5 | `testNotifyPayCanceled` | PASS | 0.005s |
| 6 | `testNotifyWithRefundedStatus` | PASS | 0.005s |
| 7 | `testNotifyWithValidSignature` | PASS | 0.005s |
| 8 | `testNotifyWithMalformedAuthorization` | PASS | 0.004s |

### SqbResponseTest

- **包名**: `com.example.sqbpayment.model`
- **测试数**: 14 | **耗时**: 0.239s | **状态**: PASS

| # | 测试方法 | 状态 | 耗时 |
|---|---------|------|------|
| 1 | `testCheckinResponse` | PASS | 0.040s |
| 2 | `testCommunicationFailure` | PASS | 0.002s |
| 3 | `testToString` | PASS | 0.016s |
| 4 | `testPartialRefundResponse` | PASS | 0.001s |
| 5 | `testMissingFields` | PASS | 0.001s |
| 6 | `testBizResultCode` | PASS | 0.001s |
| 7 | `testActivateResponse` | PASS | 0.000s |
| 8 | `testGetRawResponse` | PASS | 0.001s |
| 9 | `testRefundResponse` | PASS | 0.001s |
| 10 | `testPaySuccessResponse` | PASS | 0.001s |
| 11 | `testOrderStatusFallbackToStatus` | PASS | 0.001s |
| 12 | `testBizError` | PASS | 0.001s |
| 13 | `testCommunicationSuccess` | PASS | 0.001s |
| 14 | `testMissingBizResponse` | PASS | 0.001s |

### OrderStatusTest

- **包名**: `com.example.sqbpayment.model.enums`
- **测试数**: 7 | **耗时**: 0.009s | **状态**: PASS

| # | 测试方法 | 状态 | 耗时 |
|---|---------|------|------|
| 1 | `testIsFinalWithNonFinalString` | PASS | 0.001s |
| 2 | `testAllEnumValuesHaveDescription` | PASS | 0.000s |
| 3 | `testDescriptions` | PASS | 0.000s |
| 4 | `testIsFinalWithUnknownStatus` | PASS | 0.000s |
| 5 | `testIsFinalWithString` | PASS | 0.000s |
| 6 | `testFinalStates` | PASS | 0.000s |
| 7 | `testNonFinalStates` | PASS | 0.000s |

### SqbPayServiceTest

- **包名**: `com.example.sqbpayment.service`
- **测试数**: 12 | **耗时**: 0.068s | **状态**: PASS

| # | 测试方法 | 状态 | 耗时 |
|---|---------|------|------|
| 1 | `testPayInProgressTriggersPolling` | PASS | 0.007s |
| 2 | `testPayRequestContainsSubject` | PASS | 0.005s |
| 3 | `testPaySuccessButNonFinalStatusTriggersPolling` | PASS | 0.005s |
| 4 | `testPayWithNotifyUrl` | PASS | 0.004s |
| 5 | `testPayNetworkException` | PASS | 0.005s |
| 6 | `testPayFailImmediate` | PASS | 0.004s |
| 7 | `testPayFailErrorTriggersPolling` | PASS | 0.004s |
| 8 | `testPayRequestContainsAmount` | PASS | 0.004s |
| 9 | `testPaySuccessImmediate` | PASS | 0.003s |
| 10 | `testPayUsesTerminalLevelSigning` | PASS | 0.004s |
| 11 | `testPayCommunicationFailure` | PASS | 0.004s |
| 12 | `testPayRequestContainsDynamicId` | PASS | 0.004s |

### SqbQueryServiceTest

- **包名**: `com.example.sqbpayment.service`
- **测试数**: 12 | **耗时**: 9.075s | **状态**: PASS

| # | 测试方法 | 状态 | 耗时 |
|---|---------|------|------|
| 1 | `testPollReturnOnRefunded` | PASS | 0.007s |
| 2 | `testQueryRequestContainsTerminalSn` | PASS | 0.006s |
| 3 | `testPollContinuesOnNonFinalStatus` | PASS | 3.006s |
| 4 | `testPollReturnsImmediatelyOnFinalStatus` | PASS | 0.004s |
| 5 | `testQueryBySn` | PASS | 0.004s |
| 6 | `testPollContinuesOnPayError` | PASS | 3.006s |
| 7 | `testPollInterrupted` | PASS | 0.006s |
| 8 | `testPollReturnOnPayCanceled` | PASS | 0.004s |
| 9 | `testQueryByClientSn` | PASS | 0.004s |
| 10 | `testQueryNetworkException` | PASS | 0.004s |
| 11 | `testPollContinuesOnCommunicationFailure` | PASS | 3.005s |
| 12 | `testQueryUsesTerminalLevelSigning` | PASS | 0.005s |

### SqbRefundServiceTest

- **包名**: `com.example.sqbpayment.service`
- **测试数**: 12 | **耗时**: 1.318s | **状态**: PASS

| # | 测试方法 | 状态 | 耗时 |
|---|---------|------|------|
| 1 | `testRefundRequestContainsRefundAmount` | PASS | 1.201s |
| 2 | `testRefundFullSuccess` | PASS | 0.010s |
| 3 | `testRefundCommunicationFailure` | PASS | 0.007s |
| 4 | `testRefundRequestContainsRefundReason` | PASS | 0.009s |
| 5 | `testRefundFailErrorTriggersPolling` | PASS | 0.011s |
| 6 | `testRefundUsesTerminalLevelSigning` | PASS | 0.007s |
| 7 | `testRefundInProgressTriggersPollingByClientSn` | PASS | 0.010s |
| 8 | `testRefundNetworkException` | PASS | 0.009s |
| 9 | `testRefundPartialSuccess` | PASS | 0.005s |
| 10 | `testRefundRequestContainsRefundRequestNo` | PASS | 0.005s |
| 11 | `testRefundInProgressTriggersPollingBySn` | PASS | 0.007s |
| 12 | `testRefundFail` | PASS | 0.005s |

### SqbTerminalServiceTest

- **包名**: `com.example.sqbpayment.service`
- **测试数**: 11 | **耗时**: 0.098s | **状态**: PASS

| # | 测试方法 | 状态 | 耗时 |
|---|---------|------|------|
| 1 | `testCheckinUsesTerminalLevelSigning` | PASS | 0.011s |
| 2 | `testCheckinRequestContainsDeviceId` | PASS | 0.004s |
| 3 | `testActivateNetworkException` | PASS | 0.005s |
| 4 | `testActivateSuccess` | PASS | 0.020s |
| 5 | `testCheckinFailureKeyNotUpdated` | PASS | 0.008s |
| 6 | `testActivateRequestContainsAppId` | PASS | 0.003s |
| 7 | `testActivateCommunicationError` | PASS | 0.004s |
| 8 | `testCheckinSuccess` | PASS | 0.009s |
| 9 | `testActivateFailure` | PASS | 0.003s |
| 10 | `testCheckinRequestContainsTerminalSn` | PASS | 0.003s |
| 11 | `testActivateUsesVendorLevelSigning` | PASS | 0.005s |

### ClientSnGeneratorTest

- **包名**: `com.example.sqbpayment.util`
- **测试数**: 3 | **耗时**: 0.046s | **状态**: PASS

| # | 测试方法 | 状态 | 耗时 |
|---|---------|------|------|
| 1 | `testGenerateUniqueness` | PASS | 0.041s |
| 2 | `testGenerateRefundNo` | PASS | 0.001s |
| 3 | `testGenerateFormat` | PASS | 0.000s |

### SqbSignUtilTest

- **包名**: `com.example.sqbpayment.util`
- **测试数**: 11 | **耗时**: 0.083s | **状态**: PASS

| # | 测试方法 | 状态 | 耗时 |
|---|---------|------|------|
| 1 | `testVerifySignWithWrongKey` | PASS | 0.025s |
| 2 | `testVerifySign` | PASS | 0.008s |
| 3 | `testSignDifferentBodyProducesDifferentSign` | PASS | 0.004s |
| 4 | `testSign` | PASS | 0.003s |
| 5 | `testVerifySignWithTamperedBody` | PASS | 0.003s |
| 6 | `testBuildAuthorizationFormat` | PASS | 0.015s |
| 7 | `testSignConsistency` | PASS | 0.005s |
| 8 | `testSignDifferentKeyProducesDifferentSign` | PASS | 0.001s |
| 9 | `testSignWithChineseCharacters` | PASS | 0.005s |
| 10 | `testBuildAuthorization` | PASS | 0.001s |
| 11 | `testSignWithEmptyBody` | PASS | 0.002s |

## 测试分类统计

| 分类 | 测试类 | 测试数 | 说明 |
|------|--------|--------|------|
| 工具类 | 2 | 14 | MD5签名、客户端流水号生成 |
| 模型层 | 2 | 21 | 订单状态枚举、三层响应解析 |
| 服务层 | 4 | 47 | 终端激活/签到、支付、查询、退款 |
| 控制器 | 1 | 8 | 回调通知(@WebMvcTest+MockMvc) |

## 业务覆盖矩阵

| 业务功能 | 测试覆盖 | 关键场景 |
|----------|----------|----------|
| 终端激活 (activate) | SqbTerminalServiceTest | vendor级签名、terminal_sn/key持久化 |
| 终端签到 (checkin) | SqbTerminalServiceTest | terminal级签名、key轮换 |
| B2C支付 (pay) | SqbPayServiceTest | 即时成功/失败、轮询触发、通讯失败 |
| 订单查询 (query) | SqbQueryServiceTest | sn/client_sn查询、轮询策略(3s/10s)、120s超时 |
| 退款 (refund) | SqbRefundServiceTest | 全额/部分退款、异步轮询、退款失败 |
| 回调通知 (notify) | SqbNotifyControllerTest | 签名验证、多种订单状态、异常报文处理 |
| MD5签名 | SqbSignUtilTest | 签名生成、Authorization格式、中文字符、篡改检测 |
| 三层响应解析 | SqbResponseTest | 通讯层/业务层/订单状态三层解析 |
| 订单状态机 | OrderStatusTest | 终态/非终态判断、字符串查找 |
| 流水号生成 | ClientSnGeneratorTest | 唯一性(1000次)、格式校验、退款流水号 |

---
*报告由 Maven Surefire 测试结果自动生成*