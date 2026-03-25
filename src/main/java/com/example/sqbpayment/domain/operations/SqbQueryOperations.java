package com.example.sqbpayment.domain.operations;

import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.sdk.SqbRequestOptions;

/**
 * 查询操作接口
 */
public interface SqbQueryOperations {
    SqbResponse queryByClientSn(String clientSn, SqbRequestOptions options);
    SqbResponse queryBySn(String sn, SqbRequestOptions options);
}
