package com.daaao.bid.property;

/**
 * @author hao
 */
public interface BidProperty {
    String BID_REDIS_ENABLE = "spring.bid.redis.enable";
    String BID_UID_ENABLE = "spring.bid.uid.enable";
    String BID_APPNAME = "spring.bid.appName";
    String BID_REDIS_HOST = "spring.bid.redis.host";
    String BID_REDIS_PORT = "spring.bid.redis.port";
    String BID_REDIS_PASSWORD = "spring.bid.redis.password";
    String BID_REDIS_CLUSTER_NODES = "spring.bid.redis.cluster.nodes";
    String BID_UID_CACHED_SCHEDULE_INTERVAL = "spring.bid.uid.cached.scheduleInterval";
    String BID_UID_CACHED_PADDING_FACTOR = "spring.bid.uid.cached.paddingFactor";
    String BID_UID_CACHED_BOOST_POWER = "spring.bid.uid.cached.boostPower";

    String SPRING_APPLICATION_NAME = "spring.application.name";
    String SPRING_REDIS_HOST = "spring.redis.host";
    String SPRING_REDIS_PORT = "spring.redis.port";
    String SPRING_REDIS_PASSWORD = "spring.redis.password";
}
