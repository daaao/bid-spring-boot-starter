package com.daaao.bid;

import com.daaao.bid.policy.redis.RedisIdTemplate;
import com.daaao.bid.policy.fallback.DefaultIdFallback;
import com.daaao.bid.policy.redis.BIdRedisProperties;
import com.daaao.bid.policy.redis.RedisIdWorker;
import com.daaao.bid.policy.uid.UidProperties;
import com.daaao.bid.policy.uid.impl.CachedUidGenerator;
import com.daaao.bid.policy.uid.impl.DefaultUidGenerator;
import com.daaao.bid.policy.uid.worker.DisposableWorkerIdAssigner;
import com.daaao.bid.policy.uid.worker.WorkerIdAssigner;
import com.daaao.bid.property.BidProperty;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.util.ObjectUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hao
 */
@Configuration
@MapperScan({ "com.daaao.bid.policy.uid.worker.dao" })
@EnableConfigurationProperties({UidProperties.class})
public class BidAutoConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(BidAutoConfiguration.class);

    @Autowired
    private Environment environment;

    @Autowired
    private UidProperties uidProperties;

    public BidAutoConfiguration() {
        LOGGER.info("BidAutoConfiguration initializing...");
    }

    @Bean
    @ConditionalOnMissingBean
    public BIdRedisProperties bIdRedisProperties() {
        return new BIdRedisProperties();
    }

    @Bean(name = "bidGenericObjectPoolConfig")
    @ConditionalOnProperty(name = BidProperty.BID_REDIS_ENABLE, havingValue = "true")
    @ConditionalOnMissingBean(name = "bidGenericObjectPoolConfig")
    public GenericObjectPoolConfig bidGenericObjectPoolConfig(BIdRedisProperties bIdRedisProperties) {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(bIdRedisProperties.getMaxActive());
        config.setMaxIdle(bIdRedisProperties.getMaxIdle());
        config.setMinIdle(bIdRedisProperties.getMinIdle());
        config.setMaxWaitMillis(bIdRedisProperties.getMaxWait());
        config.setTestOnBorrow(bIdRedisProperties.getTestOnBorrow());
        config.setTestWhileIdle(bIdRedisProperties.getTestWhileIdle());
        return config;
    }

    @Bean(name = "bidLettuceConnectionFactory")
    @ConditionalOnMissingBean(name = "bidLettuceConnectionFactory")
    @ConditionalOnBean(name = "bidGenericObjectPoolConfig")
    public LettuceConnectionFactory bidLettuceConnectionFactory(@Qualifier("bidGenericObjectPoolConfig")GenericObjectPoolConfig bidGenericObjectPoolConfig, BIdRedisProperties bIdRedisProperties) {
        LettuceClientConfiguration clientConfiguration = LettucePoolingClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(bIdRedisProperties.getCommandTimeout()))
                .shutdownTimeout(Duration.ofMillis(bIdRedisProperties.getShutdownTimeout()))
                .poolConfig(bidGenericObjectPoolConfig).build();
        String host = environment.getProperty(BidProperty.BID_REDIS_HOST, environment.getProperty(BidProperty.SPRING_REDIS_HOST,""));
        int port = environment.getProperty(BidProperty.BID_REDIS_PORT, Integer.class, environment.getProperty(BidProperty.SPRING_REDIS_PORT, Integer.class,6379));
        String password = environment.getProperty(BidProperty.BID_REDIS_PASSWORD, environment.getProperty(BidProperty.SPRING_REDIS_PASSWORD,""));
        String clusterNodes = environment.getProperty(BidProperty.BID_REDIS_CLUSTER_NODES);

        if (ObjectUtils.isEmpty(clusterNodes)) {
            //单机配置
            RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
            redisStandaloneConfiguration.setHostName(host);
            if (!ObjectUtils.isEmpty(password)) {
                redisStandaloneConfiguration.setPassword(RedisPassword.of(password));
            }
            redisStandaloneConfiguration.setPort(port);
            redisStandaloneConfiguration.setDatabase(bIdRedisProperties.getDatabase());
            return new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfiguration);
        } else {
            //集群配置
            Map<String, Object> source = new HashMap<>(8);
            source.put("spring.redis.cluster.nodes", environment.getProperty("spring.bid.redis.cluster.nodes"));
//            source.put("spring.redis.cluster.timeout", environment.getProperty("spring.bid.redis.cluster.timeout"));
            source.put("spring.redis.cluster.max-redirects", environment.getProperty("spring.bid.redis.cluster.max-redirects"));
            RedisClusterConfiguration redisClusterConfiguration = new RedisClusterConfiguration(new MapPropertySource("RedisClusterConfiguration", source));
            if (!ObjectUtils.isEmpty(password)) {
                redisClusterConfiguration.setPassword(RedisPassword.of(password));
            }

            return new LettuceConnectionFactory(redisClusterConfiguration, clientConfiguration);
        }
    }

    @Bean("bidStringRedisTemplate")
    @ConditionalOnMissingBean(name = "bidStringRedisTemplate")
    @ConditionalOnBean(name = "bidLettuceConnectionFactory")
    public StringRedisTemplate bidStringRedisTemplate(@Qualifier("bidLettuceConnectionFactory") RedisConnectionFactory bidLettuceConnectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(bidLettuceConnectionFactory);
        return template;
    }

    @Bean
    @ConditionalOnMissingBean
    public DefaultRedisScript<Long> bidDefaultRedisScript() {
        DefaultRedisScript<Long> defaultRedisScript = new DefaultRedisScript<>();
        defaultRedisScript.setResultType(Long.class);
        defaultRedisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("script/nextuuid.lua")));
        return defaultRedisScript;
    }

    @Bean
    @ConditionalOnMissingBean
    public DefaultIdFallback defaultIdFallback() {
        return new DefaultIdFallback();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(name = "bidStringRedisTemplate")
    public RedisIdWorker redisIdWorker() {
        return new RedisIdWorker();
    }

    @Bean
    @ConditionalOnBean(RedisIdWorker.class)
    RedisIdTemplate redisIdTemplate() {
        return new RedisIdTemplate();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = BidProperty.BID_UID_ENABLE, havingValue = "true")
    WorkerIdAssigner workerIdAssigner() {
        return new DisposableWorkerIdAssigner();
    }

    @Bean
    @ConditionalOnMissingBean
//    @Lazy
    @ConditionalOnBean(WorkerIdAssigner.class)
    DefaultUidGenerator defaultUidGenerator() {
        return new DefaultUidGenerator(uidProperties);
    }

    @Bean
    @ConditionalOnMissingBean
//    @Lazy
    @ConditionalOnBean({WorkerIdAssigner.class})
    CachedUidGenerator cachedUidGenerator() {
        return new CachedUidGenerator(uidProperties);
    }

}
