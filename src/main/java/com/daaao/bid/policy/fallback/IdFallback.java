package com.daaao.bid.policy.fallback;

/**
 * ID生成服务降级策略
 *
 * @author hao
 */
public interface IdFallback {
    String nextId();
}
