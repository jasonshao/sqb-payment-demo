package com.example.sqbpayment.sdk.responsegetter;

import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.sdk.exception.SqbException;

/**
 * 响应获取器接口
 *
 * 职责：合并配置、签名、序列化、调 transport、解析响应、映射异常
 */
public interface SqbResponseGetter {

    SqbResponse request(SqbApiRequest apiRequest) throws SqbException;
}
