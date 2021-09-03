package com.daaao.bid.policy.redis;

import com.daaao.bid.policy.fallback.DefaultIdFallback;
import com.daaao.bid.policy.fallback.IdFallback;
import com.daaao.bid.property.BidProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author hao
 */
public class RedisIdTemplate implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisTemplate.class);

    @Autowired
    private Environment environment;

    @Autowired
    private RedisIdWorker redisIdWorker;

    @Autowired
    private DefaultIdFallback defaultIdFallback;

    private IdPatternConfiguration idPatternConfiguration;

    private String appName = "BID_DEFAULT_APP";

    public RedisIdTemplate() {
        this.idPatternConfiguration = IdPatternConfiguration.builder().build();
    }

    public RedisIdTemplate(IdPatternConfiguration idPatternConfiguration) {
        this.idPatternConfiguration = idPatternConfiguration;
    }

    /**
     * 按规则生成ID
     *
     * @param key               redis key名称，同时作为ID前缀（若配置了需要前缀）
     * @param dateTimeFormatter 日期格式，为空则不拼接日期，默认null
     * @param digits            redis id位数，id不足位数则在前面补0，默认0
     * @param increment         redis id增长步长，默认1
     * @param initial           redis id初始化值，默认从0开始
     * @param needPrefix         是否需要前缀，默认false不拼接
     * @param isResetDaily      是否需要每天重置序列，默认false不重置，为true的话序列会每天重置为initial的参数值，一般搭配dateTimeFormatter参数一起使用
     * @param idFallback        redis不可用时，服务降级的ID生成策略
     * @return ID
     */
    public String next(String key, DateTimeFormatter dateTimeFormatter, int digits, Integer increment, Long initial, boolean needPrefix, boolean isResetDaily, IdFallback idFallback) {
        Assert.hasLength(key, "key must not be null or empty!");
        StringBuffer seq = new StringBuffer();
        if (needPrefix && !StringUtils.isEmpty(key)) {
            seq.append(key);
        }
        try {
            //如果配置置了每天重置序列，但未配置日期格式化的格式，就会导致生成序列会跟前一天的重复，所以当配置了按天重置序列时，日期格式化默认按天
            if(isResetDaily && dateTimeFormatter==null){
                dateTimeFormatter = SimpleDateFormatter.FORMATTER_DAY;
                LOGGER.warn("当前参数配置了每天重置序列(isResetDaily=true)，但未配置日期格式化的格式(dateTimeFormatter)，这将导致生成序列会跟前一天的重复，系统将为您配置默认的dateTimeFormatter参数以避免ID重复，建议您配置isResetDaily=true的同时配置dateTimeFormatter参数");
            }

            String dateStr = (dateTimeFormatter != null) ? dateTimeFormatter.format(LocalDateTime.now()) : "";
            //如果需要每天重置序列，则key的过期时间设置为48小时，也按照当天日期拼接生成新的redis key；否则永不过期
            long ttl = isResetDaily ? 172800 : -1;
            Long id = redisIdWorker.incr(getRedisKeyName(key, isResetDaily), increment, initial, ttl);
            String formatRedisId = digits > 0 ? String.format("%0" + digits + "d", id) : String.valueOf(id);
            return seq.append(dateStr).append(formatRedisId).toString();
        } catch (Exception ex) {
            //为了容错和服务降级, 当Redis不能提供服务或其他异常错误导致生成失败时
            LOGGER.error("通过Redis生成唯一ID失败，采用服务降级策略生成", ex);
        }
        //没有配置ID服务降级策略，则使用默认策略
        idFallback = (idFallback == null) ? defaultIdFallback : idFallback;
        return seq.append(idFallback.nextId()).toString();
    }

    public String next(String key, DateTimeFormatter dateTimeFormatter, int digits, Long initial, boolean needPrefix, boolean isResetDaily) {
        return next(key, dateTimeFormatter, digits, idPatternConfiguration.getIncrement(), initial, needPrefix, isResetDaily, idPatternConfiguration.getIdFallback());
    }

    public String next(String key, DateTimeFormatter dateTimeFormatter, int digits, Long initial, boolean needPrefix) {
        return next(key, dateTimeFormatter, digits, initial, needPrefix, idPatternConfiguration.isResetDaily);
    }

    public String next(String key, DateTimeFormatter dateTimeFormatter, int digits, Long initial) {
        return next(key, dateTimeFormatter, digits, initial, idPatternConfiguration.isNeedPrefix());
    }

    public String next(String key, DateTimeFormatter dateTimeFormatter, int digits) {
        return next(key, dateTimeFormatter, digits, idPatternConfiguration.getInitial());
    }

    public String next(String key, int digits) {
        return next(key, idPatternConfiguration.getDateTimeFormatter(), digits);
    }

    public String next(String key, Long initial) {
        return next(key, idPatternConfiguration.getDateTimeFormatter(), idPatternConfiguration.getDigits(), initial);
    }

    public String next(String key) {
        return next(key, idPatternConfiguration.getDigits());
    }

    public String next(Long initial) {
        return next(idPatternConfiguration.getKey(), initial);
    }

    public String next() {
        return next(idPatternConfiguration.getKey());
    }

    public String getRedisKeyName(String key, boolean isResetDaily) {
        StringBuffer redisKey = new StringBuffer().append(appName.toUpperCase());
        if (!StringUtils.isEmpty(key)) {
            redisKey.append("_").append(key.toUpperCase());
        }
        redisKey.append("_UID");

        if (isResetDaily) {
            redisKey.append("_").append(SimpleDateFormatter.FORMATTER_DAY.format(LocalDateTime.now()));
        }
        return redisKey.toString();
    }

    public String getAppName(){
        return environment!=null ? (environment.getProperty(BidProperty.BID_APPNAME, environment.getProperty(BidProperty.SPRING_APPLICATION_NAME))) : this.appName;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.appName=getAppName();
    }
}
