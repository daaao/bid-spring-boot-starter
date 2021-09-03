package com.daaao.bid.policy.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author hao
 */
@Configuration("bIdRedisProperties")
public class BIdRedisProperties {
    private static final Logger LOGGER = LoggerFactory.getLogger(BIdRedisProperties.class);

    public BIdRedisProperties() {
        LOGGER.info("BIdRedisProperties initializing...");
    }

    @Value("${spring.bid.redis.database:0}")
    private Integer database;
    @Value("${spring.bid.redis.testOnBorrow:false}")
    private boolean testOnBorrow;
    @Value("${spring.bid.redis.testWhileIdle:false}")
    private boolean testWhileIdle;
    @Value("${spring.bid.redis.timeout:2000}")
    private Long timeout;

    @Value("${spring.bid.redis.lettuce.commandTimeout:2}")
    private Long commandTimeout;
    @Value("${spring.bid.redis.lettuce.shutdownTimeout:100}")
    private Long shutdownTimeout;
    @Value("${spring.bid.redis.lettuce.pool.max-active:32}")
    private Integer maxActive;
    @Value("${spring.bid.redis.lettuce.pool.max-idle:16}")
    private Integer maxIdle;
    @Value("${spring.bid.redis.lettuce.pool.max-wait:1000}")
    private Long maxWait;
    @Value("${spring.bid.redis.lettuce.pool.min-idle:1}")
    private Integer minIdle;

    public Integer getMaxActive() {
        return maxActive;
    }

    public void setMaxActive(Integer maxActive) {
        this.maxActive = maxActive;
    }

    public Integer getMaxIdle() {
        return maxIdle;
    }

    public void setMaxIdle(Integer maxIdle) {
        this.maxIdle = maxIdle;
    }

    public Long getMaxWait() {
        return maxWait;
    }

    public void setMaxWait(Long maxWait) {
        this.maxWait = maxWait;
    }

    public Integer getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(Integer minIdle) {
        this.minIdle = minIdle;
    }

    public Integer getDatabase() {
        return database;
    }

    public void setDatabase(Integer database) {
        this.database = database;
    }

    public boolean getTestOnBorrow() {
        return testOnBorrow;
    }

    public void setTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
    }

    public boolean getTestWhileIdle() {
        return testWhileIdle;
    }

    public void setTestWhileIdle(boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public Long getCommandTimeout() {
        return commandTimeout;
    }

    public void setCommandTimeout(Long commandTimeout) {
        this.commandTimeout = commandTimeout;
    }

    public Long getShutdownTimeout() {
        return shutdownTimeout;
    }

    public void setShutdownTimeout(Long shutdownTimeout) {
        this.shutdownTimeout = shutdownTimeout;
    }

}