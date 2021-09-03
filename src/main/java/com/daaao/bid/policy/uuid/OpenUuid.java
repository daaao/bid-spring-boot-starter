package com.daaao.bid.policy.uuid;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author hao
 */
public class OpenUuid {

    /**
     * 生成4位随机数+UUID hashCode的序列号
     * @return uuid
     */
    public static Long uuid() {
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        int uuid = Math.abs(UUID.randomUUID().toString().hashCode());
        return Long.valueOf(random + String.format("%010d", uuid));
    }
}
