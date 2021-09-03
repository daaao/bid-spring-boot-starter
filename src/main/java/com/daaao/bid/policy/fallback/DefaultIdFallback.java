package com.daaao.bid.policy.fallback;

import com.daaao.bid.policy.uuid.OpenUuid;

/**
 * 默认的ID服务降级策略-采用随机数+UUID
 *
 * @author hao
 */
public class DefaultIdFallback implements IdFallback {

    @Override
    public String nextId() {
        return String.valueOf(OpenUuid.uuid());
    }
}
