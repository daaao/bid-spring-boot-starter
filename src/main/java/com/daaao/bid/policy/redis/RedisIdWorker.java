package com.daaao.bid.policy.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Arrays;

/**
 * @author hao
 */
public class RedisIdWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisIdWorker.class);

    @Autowired
    @Qualifier("bidStringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    @Autowired
    @Qualifier("bidDefaultRedisScript")
    private DefaultRedisScript<Long> defaultRedisScript;

    public Long incr(String key, Integer increment, Long initial, long ttl) {
        increment = (increment == null || increment <= 0) ? 1 : increment;
        Long result = (Long) redisTemplate.execute(defaultRedisScript, Arrays.asList(key), String.valueOf(increment), String.valueOf(initial), String.valueOf(ttl));
        return result;
    }

    public Long incr(String key, Long initial) {
        return incr(key, -1, initial, -1);
    }

    public Long incr(String key, Integer increment) {
        return incr(key, increment, -1L, -1);
    }

    public Long incr(String key) {
        return incr(key, -1);
    }

}
